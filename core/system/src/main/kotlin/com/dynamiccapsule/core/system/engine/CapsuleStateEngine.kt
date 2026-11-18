package com.dynamiccapsule.core.system.engine

import com.dynamiccapsule.core.database.CapsuleDatabaseRepository
import com.dynamiccapsule.core.datastore.CapsulePreferencesRepository
import com.dynamiccapsule.core.model.ActionDrawerTab
import com.dynamiccapsule.core.model.ActiveDrawerContent
import com.dynamiccapsule.core.model.ActiveDynamicCard
import com.dynamiccapsule.core.model.AirPodsStatusPayload
import com.dynamiccapsule.core.model.CapsuleDisplayMode
import com.dynamiccapsule.core.model.CapsuleNotificationAction
import com.dynamiccapsule.core.model.CapsuleNotificationPayload
import com.dynamiccapsule.core.model.CapsuleOverlayUiState
import com.dynamiccapsule.core.model.DynamicCardType
import com.dynamiccapsule.core.model.MiniCapsuleEventPayload
import com.dynamiccapsule.core.model.MiniCapsuleEventType
import com.dynamiccapsule.core.model.RingerStateMode
import com.dynamiccapsule.core.model.ScreenOrientationMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Moteur réactif central (Singleton) arbitrant les Cartes Dynamiques, les événements Mini-Capsule,
 * les Notifications d'applications, le Tiroir d'actions et l'Éditeur visuel en direct.
 */
class CapsuleStateEngine(
    private val preferencesRepository: CapsulePreferencesRepository,
    private val databaseRepository: CapsuleDatabaseRepository,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {
    private val rawActiveCards = MutableStateFlow<Map<DynamicCardType, ActiveDynamicCard>>(emptyMap())
    private val selectedCardIndexFlow = MutableStateFlow(0)
    private val isExpandedCardOpenFlow = MutableStateFlow(false)
    private val currentMiniEventFlow = MutableStateFlow<MiniCapsuleEventPayload?>(null)
    private val currentNotificationFlow = MutableStateFlow<CapsuleNotificationPayload?>(null)
    private val activeDrawerFlow = MutableStateFlow<ActiveDrawerContent?>(null)
    private val isLiveEditorOpenFlow = MutableStateFlow(false)

    private val currentDisplayResolution = MutableStateFlow( DisplayConfigState() )
    private val isNightModeFlow = MutableStateFlow(true)
    private val isDeviceLockedFlow = MutableStateFlow(false)

    private var miniEventDismissJob: Job? = null
    private var notificationDismissJob: Job? = null
    private var volumeCardAutoHideJob: Job? = null

    private val _uiState = MutableStateFlow(CapsuleOverlayUiState())
    val uiState: StateFlow<CapsuleOverlayUiState> = _uiState.asStateFlow()

    init {
        scope.launch {
            combine(
                preferencesRepository.isMasterEnabled,
                preferencesRepository.dynamicCardPriorities,
                preferencesRepository.appearanceConfig,
                preferencesRepository.animationConfig,
                preferencesRepository.notificationVisualConfig
            ) { master, priorities, appearance, animation, notifVisual ->
                 PrefsSnapshot(master, priorities, appearance, animation, notifVisual)
            }.combine(
                combine(
                    preferencesRepository.gestureConfig,
                    preferencesRepository.generalSettings,
                    databaseRepository.screenProfiles,
                    currentDisplayResolution
                ) { gestures, general, profiles, display ->
                    val key = "${display.widthPx}x${display.heightPx}_${display.orientation.name}"
                    val profile = profiles[key] ?: databaseRepository.getOrCreateProfileForDisplay(
                        display.widthPx,
                        display.heightPx,
                        display.orientation,
                        display.cutoutTopPx
                    )
                    ConfigSnapshot(gestures, general, profile)
                }
            ) { prefs, config ->
                prefs to config
            }.combine(
                combine(
                    rawActiveCards,
                    selectedCardIndexFlow,
                    isExpandedCardOpenFlow,
                    currentMiniEventFlow,
                    currentNotificationFlow
                ) { cardsMap, selectedIdx, isExpanded, miniEvent, notif ->
                    RuntimeActivitySnapshot(cardsMap, selectedIdx, isExpanded, miniEvent, notif)
                }
            ) { (prefs, config), runtime ->
                Triple(prefs, config, runtime)
            }.combine(
                combine(
                    activeDrawerFlow,
                    isLiveEditorOpenFlow,
                    isNightModeFlow,
                    isDeviceLockedFlow
                ) { drawer, liveEditor, nightMode, locked ->
                    OverlayEnvironmentSnapshot(drawer, liveEditor, nightMode, locked)
                }
            ) { (prefs, config, runtime), env ->
                buildOverlayUiState(prefs, config, runtime, env)
            }.collectLatest { newState ->
                _uiState.value = newState
            }
        }
    }

    private fun buildOverlayUiState(
        prefs: PrefsSnapshot,
        config: ConfigSnapshot,
        runtime: RuntimeActivitySnapshot,
        env: OverlayEnvironmentSnapshot
    ): CapsuleOverlayUiState {
        // Filtrer et trier les cartes dynamiques actives selon l'ordre drag & drop de l'utilisateur
        val sortedActiveCards = if (env.isDeviceLocked && !config.general.showCardsOnLockScreen) {
            emptyList()
        } else {
            prefs.priorities
                .filter { it.enabled }
                .sortedBy { it.priorityOrder }
                .mapNotNull { item -> runtime.cardsMap[item.type] }
        }

        val safeIndex = if (sortedActiveCards.isEmpty()) {
            0
        } else {
            runtime.selectedIdx.coerceIn(0, sortedActiveCards.size - 1)
        }

        val displayMode = when {
            !prefs.masterEnabled -> CapsuleDisplayMode.HIDDEN
            env.isLiveEditor -> CapsuleDisplayMode.LIVE_PROFILE_EDITOR
            env.drawer != null -> CapsuleDisplayMode.ACTION_DRAWER
            runtime.miniEvent != null -> CapsuleDisplayMode.MINI_EVENT_POPUP
            runtime.notif != null && runtime.isExpanded -> CapsuleDisplayMode.EXPANDED_CARD
            runtime.notif != null -> CapsuleDisplayMode.NOTIFICATION_POPUP
            runtime.isExpanded && sortedActiveCards.isNotEmpty() -> CapsuleDisplayMode.EXPANDED_CARD
            sortedActiveCards.size >= 2 -> CapsuleDisplayMode.SPLIT_COMPACT_CARD
            sortedActiveCards.size == 1 -> CapsuleDisplayMode.COMPACT_CARD
            prefs.appearance.showIdleCutoutRing -> CapsuleDisplayMode.IDLE_CUTOUT
            else -> CapsuleDisplayMode.HIDDEN
        }

        return CapsuleOverlayUiState(
            isMasterEnabled = prefs.masterEnabled,
            displayMode = displayMode,
            activeDynamicCards = sortedActiveCards,
            selectedCardIndex = safeIndex,
            currentMiniEvent = runtime.miniEvent,
            currentNotification = runtime.notif,
            activeDrawerContent = env.drawer,
            isLiveProfileEditorOpen = env.isLiveEditor,
            currentProfile = config.profile,
            appearance = prefs.appearance,
            animation = prefs.animation,
            notificationStyle = prefs.notifVisual,
            gestureConfig = config.gestures,
            generalSettings = config.general,
            isNightMode = env.isNightMode,
            isDeviceLocked = env.isDeviceLocked
        )
    }

    /**
     * Publie un événement temporaire "Mini Capsule" s'il est activé dans les préférences.
     */
    fun postMiniCapsuleEvent(payload: MiniCapsuleEventPayload, bypassFilter: Boolean = false) {
        if (!bypassFilter && !preferencesRepository.isMiniEventEnabled(payload.type)) {
            return
        }
        miniEventDismissJob?.cancel()
        currentMiniEventFlow.value = payload
        miniEventDismissJob = scope.launch {
            delay(payload.durationMs.coerceIn(1500L, 6000L))
            currentMiniEventFlow.value = null
        }
    }

    /**
     * Publie une notification d'application dans la capsule.
     */
    fun postAppNotification(payload: CapsuleNotificationPayload, bypassFilter: Boolean = false) {
        val visualConfig = preferencesRepository.notificationVisualConfig.value
        if (!bypassFilter) {
            if (!visualConfig.enabledGlobally) return
            if (!databaseRepository.isAppNotificationAllowed(payload.packageName)) return
        }
        notificationDismissJob?.cancel()
        currentNotificationFlow.value = payload
        notificationDismissJob = scope.launch {
            delay(visualConfig.displayDurationSeconds.coerceIn(2, 12) * 1000L)
            if (!isExpandedCardOpenFlow.value) {
                currentNotificationFlow.value = null
            }
        }
    }

    fun dismissCurrentPopupOrCollapse() {
        when {
            isLiveEditorOpenFlow.value -> isLiveEditorOpenFlow.value = false
            activeDrawerFlow.value != null -> activeDrawerFlow.value = null
            currentMiniEventFlow.value != null -> {
                miniEventDismissJob?.cancel()
                currentMiniEventFlow.value = null
            }
            currentNotificationFlow.value != null -> {
                notificationDismissJob?.cancel()
                currentNotificationFlow.value = null
                isExpandedCardOpenFlow.value = false
            }
            isExpandedCardOpenFlow.value -> isExpandedCardOpenFlow.value = false
        }
    }

    /**
     * Met à jour ou active une Carte Dynamique.
     */
    fun upsertDynamicCard(card: ActiveDynamicCard) {
        rawActiveCards.value = rawActiveCards.value.toMutableMap().apply {
            put(card.cardType, card)
        }
        if (card is ActiveDynamicCard.VolumeControlCard) {
            volumeCardAutoHideJob?.cancel()
            volumeCardAutoHideJob = scope.launch {
                delay(2600L)
                removeDynamicCard(DynamicCardType.VOLUME_CONTROL)
            }
        }
    }

    fun removeDynamicCard(type: DynamicCardType) {
        rawActiveCards.value = rawActiveCards.value.toMutableMap().apply {
            remove(type)
        }
        if (rawActiveCards.value.isEmpty()) {
            isExpandedCardOpenFlow.value = false
        }
    }

    fun clearAllDynamicCards() {
        rawActiveCards.value = emptyMap()
        isExpandedCardOpenFlow.value = false
    }

    fun toggleExpandedCard() {
        if (_uiState.value.activeDynamicCards.isNotEmpty() || _uiState.value.currentNotification != null) {
            isExpandedCardOpenFlow.value = !isExpandedCardOpenFlow.value
        }
    }

    fun setExpandedCard(expanded: Boolean) {
        isExpandedCardOpenFlow.value = expanded
    }

    fun cycleSelectedDynamicCard() {
        val count = _uiState.value.activeDynamicCards.size
        if (count > 1) {
            selectedCardIndexFlow.value = (selectedCardIndexFlow.value + 1) % count
        }
    }

    fun selectDynamicCardIndex(index: Int) {
        val count = _uiState.value.activeDynamicCards.size
        if (count > 0) {
            selectedCardIndexFlow.value = index.coerceIn(0, count - 1)
        }
    }

    fun openActionDrawer(content: ActiveDrawerContent) {
        activeDrawerFlow.value = content
    }

    fun closeActionDrawer() {
        activeDrawerFlow.value = null
    }

    fun setLiveProfileEditorOpen(open: Boolean) {
        isLiveEditorOpenFlow.value = open
    }

    fun onDisplayConfigurationChanged(
        widthPx: Int,
        heightPx: Int,
        orientation: ScreenOrientationMode,
        cutoutTopPx: Int = 18,
        isNightMode: Boolean = true
    ) {
        currentDisplayResolution.value = DisplayConfigState(
            widthPx = widthPx,
            heightPx = heightPx,
            orientation = orientation,
            cutoutTopPx = cutoutTopPx
        )
        isNightModeFlow.value = isNightMode
    }

    fun setDeviceLocked(locked: Boolean) {
        isDeviceLockedFlow.value = locked
    }

    // --- Méthodes du Centre de Simulation en Direct ---

    fun simulateMiniEvent(type: MiniCapsuleEventType) {
        val lang = preferencesRepository.generalSettings.value.languageCode
        val isEn = com.dynamiccapsule.core.model.isEnglishLang(lang)
        val samplePayload = when (type) {
            MiniCapsuleEventType.DEVICE_UNLOCKED -> MiniCapsuleEventPayload(
                id = UUID.randomUUID().toString(),
                type = type,
                title = if (isEn) "Unlocked" else "Déverrouillé",
                subtitle = if (isEn) "Welcome" else "Bienvenue",
                accentColorArgb = 0xFF30D158L
            )
            MiniCapsuleEventType.CHARGING_STARTED -> MiniCapsuleEventPayload(
                id = UUID.randomUUID().toString(),
                type = type,
                title = if (isEn) "Charging" else "En charge",
                subtitle = if (isEn) "78% • Fast charging" else "78 % • Charge rapide",
                numericValue = 78,
                accentColorArgb = 0xFF30D158L
            )
            MiniCapsuleEventType.CHARGING_STOPPED -> MiniCapsuleEventPayload(
                id = UUID.randomUUID().toString(),
                type = type,
                title = if (isEn) "Unplugged" else "Débranché",
                subtitle = if (isEn) "Battery at 92%" else "Batterie à 92 %",
                numericValue = 92,
                isActiveState = false,
                accentColorArgb = 0xFF90A4AEL
            )
            MiniCapsuleEventType.BATTERY_LOW_5_PERCENT -> MiniCapsuleEventPayload(
                id = UUID.randomUUID().toString(),
                type = type,
                title = if (isEn) "Low Battery" else "Batterie faible",
                subtitle = if (isEn) "5% remaining • Plug in" else "5 % restants • Branchez le chargeur",
                numericValue = 5,
                accentColorArgb = 0xFFFF453AL
            )
            MiniCapsuleEventType.BATTERY_SAVER_CHANGED -> MiniCapsuleEventPayload(
                id = UUID.randomUUID().toString(),
                type = type,
                title = if (isEn) "Battery Saver On" else "Économiseur activé",
                subtitle = if (isEn) "Power optimization" else "Optimisation énergétique",
                accentColorArgb = 0xFFFF9F0AL
            )
            MiniCapsuleEventType.WIFI_CONNECTED -> MiniCapsuleEventPayload(
                id = UUID.randomUUID().toString(),
                type = type,
                title = if (isEn) "Wi-Fi Connected" else "Wi-Fi Connecté",
                subtitle = "Fibre_5GHz • 866 Mbps",
                accentColorArgb = 0xFF0A84FFL
            )
            MiniCapsuleEventType.HOTSPOT_CHANGED -> MiniCapsuleEventPayload(
                id = UUID.randomUUID().toString(),
                type = type,
                title = if (isEn) "Hotspot Active" else "Point d'accès actif",
                subtitle = if (isEn) "1 device connected" else "Partage Wi-Fi • 1 appareil",
                accentColorArgb = 0xFF30D158L
            )
            MiniCapsuleEventType.VPN_CHANGED -> MiniCapsuleEventPayload(
                id = UUID.randomUUID().toString(),
                type = type,
                title = if (isEn) "VPN Protected" else "VPN Sécurisé",
                subtitle = "WireGuard",
                accentColorArgb = 0xFF5E5CE6L
            )
            MiniCapsuleEventType.AIRPLANE_MODE_CHANGED -> MiniCapsuleEventPayload(
                id = UUID.randomUUID().toString(),
                type = type,
                title = if (isEn) "Airplane Mode" else "Mode Avion",
                subtitle = if (isEn) "Radios disabled" else "Connexions radio désactivées",
                accentColorArgb = 0xFF64D2FFL
            )
            MiniCapsuleEventType.LOCATION_CHANGED -> MiniCapsuleEventPayload(
                id = UUID.randomUUID().toString(),
                type = type,
                title = if (isEn) "Location Active" else "Localisation active",
                subtitle = "GPS",
                accentColorArgb = 0xFF0A84FFL
            )
            MiniCapsuleEventType.BLUETOOTH_AUDIO_CHANGED -> MiniCapsuleEventPayload(
                id = UUID.randomUUID().toString(),
                type = type,
                title = "Sony WH-1000XM5",
                subtitle = if (isEn) "Connected • 90%" else "Connecté • 90 %",
                numericValue = 90,
                accentColorArgb = 0xFF0A84FFL
            )
            MiniCapsuleEventType.RINGER_MODE_CHANGED -> MiniCapsuleEventPayload(
                id = UUID.randomUUID().toString(),
                type = type,
                title = if (isEn) "Vibrate Mode" else "Mode Vibreur",
                subtitle = if (isEn) "Ringtone muted" else "Sonnerie coupée",
                ringerMode = RingerStateMode.VIBRATE,
                accentColorArgb = 0xFFFF9F0AL
            )
            MiniCapsuleEventType.DND_MODE_CHANGED -> MiniCapsuleEventPayload(
                id = UUID.randomUUID().toString(),
                type = type,
                title = if (isEn) "Do Not Disturb" else "Ne pas déranger",
                subtitle = if (isEn) "Notifications silenced" else "Notifications silencieuses",
                accentColorArgb = 0xFF5E5CE6L
            )
            MiniCapsuleEventType.ADAPTIVE_BRIGHTNESS_CHANGED -> MiniCapsuleEventPayload(
                id = UUID.randomUUID().toString(),
                type = type,
                title = if (isEn) "Auto Brightness" else "Luminosité Auto",
                subtitle = if (isEn) "Ambient sensor active" else "Adaptation au capteur ambiant",
                accentColorArgb = 0xFFFFD60AL
            )
            MiniCapsuleEventType.UI_NIGHT_MODE_CHANGED -> MiniCapsuleEventPayload(
                id = UUID.randomUUID().toString(),
                type = type,
                title = if (isEn) "Dark Theme" else "Thème Sombre",
                subtitle = if (isEn) "Night mode enabled" else "Interface nuit activée",
                accentColorArgb = 0xFF5E5CE6L
            )
            MiniCapsuleEventType.AUTO_ROTATE_CHANGED -> MiniCapsuleEventPayload(
                id = UUID.randomUUID().toString(),
                type = type,
                title = if (isEn) "Auto-Rotate" else "Rotation Auto",
                subtitle = if (isEn) "Unlocked" else "Orientation déverrouillée",
                accentColorArgb = 0xFF64D2FFL
            )
            MiniCapsuleEventType.USB_DEVICE_CHANGED -> MiniCapsuleEventPayload(
                id = UUID.randomUUID().toString(),
                type = type,
                title = if (isEn) "USB-C Device" else "Périphérique USB-C",
                subtitle = if (isEn) "SanDisk 128 GB mounted" else "Clé SanDisk 128 Go montée",
                accentColorArgb = 0xFF30D158L
            )
            MiniCapsuleEventType.AIRPODS_BATTERY_UPDATE -> MiniCapsuleEventPayload(
                id = UUID.randomUUID().toString(),
                type = type,
                title = "AirPods Pro",
                subtitle = if (isEn) "L: 88% • R: 92% • Case: 100%" else "G: 88% • D: 92% • Boîtier: 100%",
                accentColorArgb = 0xFF30D158L,
                airPodsStatus = AirPodsStatusPayload(
                    deviceName = "AirPods Pro",
                    leftPercent = 88,
                    rightPercent = 92,
                    casePercent = 100,
                    isChargingCase = true
                ),
                durationMs = 3800L
            )
        }
        postMiniCapsuleEvent(samplePayload, bypassFilter = true)
    }

    fun toggleSimulatedDynamicCard(type: DynamicCardType) {
        if (rawActiveCards.value.containsKey(type)) {
            removeDynamicCard(type)
            return
        }
        val lang = preferencesRepository.generalSettings.value.languageCode
        val isEn = com.dynamiccapsule.core.model.isEnglishLang(lang)
        val sampleCard: ActiveDynamicCard = when (type) {
            DynamicCardType.MEDIA_PLAYER -> ActiveDynamicCard.MediaPlayerCard(
                title = "Veridis Quo",
                artist = "Daft Punk • Discovery",
                album = "Discovery",
                isPlaying = true,
                positionMs = 114_000L,
                durationMs = 345_000L,
                dominantColorArgb = 0xFF7C4DFFL
            )
            DynamicCardType.FLASHLIGHT -> ActiveDynamicCard.FlashlightCard(
                isOn = true,
                strengthLevel = 4,
                maxStrengthLevel = 5
            )
            DynamicCardType.VOLUME_CONTROL -> ActiveDynamicCard.VolumeControlCard(
                currentVolume = 11,
                maxVolume = 15,
                outputDeviceName = "AirPods Pro"
            )
            DynamicCardType.LIVE_PROGRESS -> ActiveDynamicCard.LiveProgressCard(
                notificationKey = "sim_maps_nav",
                sourcePackage = "com.google.android.apps.maps",
                appLabel = "Google Maps",
                title = if (isEn) "Turn right in 250 m" else "Tourner à droite dans 250 m",
                statusText = if (isEn) "ETA 12:14 • 68% completed" else "Arrivée à 12:14 • 68 % du trajet",
                progress = 68,
                maxProgress = 100,
                accentColorArgb = 0xFF0A84FFL
            )
            DynamicCardType.TIMER_STOPWATCH -> ActiveDynamicCard.TimerStopwatchCard(
                label = if (isEn) "Green Tea Timer" else "Minuteur Thé",
                isCountDown = true,
                targetOrBaseEpochMs = System.currentTimeMillis() + 165_000L,
                remainingOrElapsedSeconds = 165L,
                totalDurationSeconds = 240L
            )
            DynamicCardType.ACTIVE_CALL -> ActiveDynamicCard.ActiveCallCard(
                callerName = "Camille Laurent",
                phoneNumberOrApp = if (isEn) "HD Audio Call" else "Appel Audio HD",
                elapsedSeconds = 84L
            )
            DynamicCardType.CHARGING_STATUS -> ActiveDynamicCard.ChargingStatusCard(
                batteryPercentage = 82,
                chargingTypeLabel = if (isEn) "45W Turbo Charge" else "Charge Turbo 45W",
                estimatedMinutesRemaining = 14
            )
        }
        upsertDynamicCard(sampleCard)
    }

    fun simulateIncomingAppNotification() {
        val lang = preferencesRepository.generalSettings.value.languageCode
        val isEn = com.dynamiccapsule.core.model.isEnglishLang(lang)
        postAppNotification(
            CapsuleNotificationPayload(
                key = "sim_whatsapp_${System.currentTimeMillis()}",
                packageName = "com.whatsapp",
                appName = "WhatsApp",
                title = "Thomas Mercier",
                message = if (isEn) {
                    "See you in front of the studio in 10 minutes?"
                } else {
                    "On se retrouve devant le studio dans 10 minutes ?"
                },
                appAccentColorArgb = 0xFF30D158L,
                actions = listOf(
                    CapsuleNotificationAction(if (isEn) "Mark as read" else "Marquer lu", 0),
                    CapsuleNotificationAction(if (isEn) "Reply" else "Répondre", 1)
                )
            ),
            bypassFilter = true
        )
    }

    private data class DisplayConfigState(
        val widthPx: Int = 1080,
        val heightPx: Int = 2400,
        val orientation: ScreenOrientationMode = ScreenOrientationMode.PORTRAIT,
        val cutoutTopPx: Int = 18
    )

    private data class PrefsSnapshot(
        val masterEnabled: Boolean,
        val priorities: List<com.dynamiccapsule.core.model.DynamicCardPriorityItem>,
        val appearance: com.dynamiccapsule.core.model.CapsuleAppearanceConfig,
        val animation: com.dynamiccapsule.core.model.CapsuleAnimationConfig,
        val notifVisual: com.dynamiccapsule.core.model.NotificationVisualConfig
    )

    private data class ConfigSnapshot(
        val gestures: com.dynamiccapsule.core.model.GestureConfiguration,
        val general: com.dynamiccapsule.core.model.AppGeneralSettings,
        val profile: com.dynamiccapsule.core.model.ScreenProfile
    )

    private data class RuntimeActivitySnapshot(
        val cardsMap: Map<DynamicCardType, ActiveDynamicCard>,
        val selectedIdx: Int,
        val isExpanded: Boolean,
        val miniEvent: MiniCapsuleEventPayload?,
        val notif: CapsuleNotificationPayload?
    )

    private data class OverlayEnvironmentSnapshot(
        val drawer: ActiveDrawerContent?,
        val isLiveEditor: Boolean,
        val isNightMode: Boolean,
        val isDeviceLocked: Boolean
    )
}
