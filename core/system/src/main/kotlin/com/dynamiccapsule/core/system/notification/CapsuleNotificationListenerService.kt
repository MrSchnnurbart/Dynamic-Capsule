package com.dynamiccapsule.core.system.notification

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.dynamiccapsule.core.model.ActiveDynamicCard
import com.dynamiccapsule.core.model.CapsuleNotificationAction
import com.dynamiccapsule.core.model.CapsuleNotificationPayload
import com.dynamiccapsule.core.model.DynamicCardType
import com.dynamiccapsule.core.model.MediaCommandType
import com.dynamiccapsule.core.system.CapsuleServiceLocator

/**
 * Service d'écoute des notifications et des sessions média actives (MediaSessionManager) :
 * - Extrait les métadonnées musicales et permet le contrôle de lecture (Play/Pause/Next/Prev).
 * - Détecte les barres de progression (Maps, Play Store, téléchargements, Telegram).
 * - Détecte les minuteurs/chronomètres de l'horloge système.
 * - Détecte les appels téléphoniques et VoIP en cours.
 * - Affiche les notifications des applications sélectionnées directement dans la capsule.
 */
class CapsuleNotificationListenerService : NotificationListenerService() {

    private var mediaSessionManager: MediaSessionManager? = null
    private var activeControllers: List<MediaController> = emptyList()
    private val pendingActionsCache = mutableMapOf<String, Array<Notification.Action>>()

    private val activeSessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        bindMediaControllers(controllers ?: emptyList())
    }

    private val mediaCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            refreshMediaCardFromControllers()
        }

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            refreshMediaCardFromControllers()
        }
    }

    override fun onCreate() {
        super.onCreate()
        CapsuleServiceLocator.initialize(applicationContext)
        instance = this
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        setupMediaSessionsListener()
        scanExistingActiveNotifications()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        teardownMediaSessionsListener()
        if (instance === this) {
            instance = null
        }
    }

    override fun onDestroy() {
        teardownMediaSessionsListener()
        pendingActionsCache.clear()
        if (instance === this) {
            instance = null
        }
        super.onDestroy()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val notification = sbn ?: return
        if (notification.packageName == packageName) return
        processStatusBarNotification(notification, isNewPost = true)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        val notification = sbn ?: return
        pendingActionsCache.remove(notification.key)
        val engine = CapsuleServiceLocator.stateEngine
        val currentCards = engine.uiState.value.activeDynamicCards

        if (currentCards.any { it is ActiveDynamicCard.LiveProgressCard && it.notificationKey == notification.key }) {
            engine.removeDynamicCard(DynamicCardType.LIVE_PROGRESS)
        }
        if (currentCards.any { it is ActiveDynamicCard.ActiveCallCard && it.notificationKey == notification.key }) {
            engine.removeDynamicCard(DynamicCardType.ACTIVE_CALL)
        }
        if (isClockOrTimerPackage(notification.packageName, notification.notification)) {
            engine.removeDynamicCard(DynamicCardType.TIMER_STOPWATCH)
        }
    }

    private fun setupMediaSessionsListener() {
        runCatching {
            val manager = getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager ?: return
            mediaSessionManager = manager
            val componentName = ComponentName(this, CapsuleNotificationListenerService::class.java)
            val initial = manager.getActiveSessions(componentName)
            bindMediaControllers(initial)
            manager.addOnActiveSessionsChangedListener(activeSessionsListener, componentName)
        }
    }

    private fun teardownMediaSessionsListener() {
        runCatching {
            activeControllers.forEach { it.unregisterCallback(mediaCallback) }
            activeControllers = emptyList()
            mediaSessionManager?.removeOnActiveSessionsChangedListener(activeSessionsListener)
        }
    }

    private fun bindMediaControllers(controllers: List<MediaController>) {
        activeControllers.forEach { runCatching { it.unregisterCallback(mediaCallback) } }
        activeControllers = controllers
        controllers.forEach { runCatching { it.registerCallback(mediaCallback) } }
        refreshMediaCardFromControllers()
    }

    private fun refreshMediaCardFromControllers() {
        val engine = CapsuleServiceLocator.stateEngine
        val primary = activeControllers.firstOrNull { controller ->
            val state = controller.playbackState?.state
            state == PlaybackState.STATE_PLAYING ||
                state == PlaybackState.STATE_PAUSED ||
                state == PlaybackState.STATE_BUFFERING
        }

        if (primary == null) {
            engine.removeDynamicCard(DynamicCardType.MEDIA_PLAYER)
            return
        }

        val metadata = primary.metadata
        val playbackState = primary.playbackState
        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
            ?: "Lecture en cours"
        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: resolveAppLabel(primary.packageName)
        val album = metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM) ?: ""
        val duration = (metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 180_000L).coerceAtLeast(1L)
        val position = (playbackState?.position ?: 0L).coerceIn(0L, duration)
        val isPlaying = playbackState?.state == PlaybackState.STATE_PLAYING

        engine.upsertDynamicCard(
            ActiveDynamicCard.MediaPlayerCard(
                packageName = primary.packageName,
                appName = resolveAppLabel(primary.packageName),
                title = title,
                artist = artist,
                album = album,
                isPlaying = isPlaying,
                positionMs = position,
                durationMs = duration,
                dominantColorArgb = 0xFF7C4DFFL
            )
        )
    }

    private fun scanExistingActiveNotifications() {
        runCatching {
            activeNotifications?.forEach { sbn ->
                processStatusBarNotification(sbn, isNewPost = false)
            }
        }
    }

    private fun processStatusBarNotification(sbn: StatusBarNotification, isNewPost: Boolean) {
        val notif = sbn.notification ?: return
        val extras = notif.extras ?: return
        val pkg = sbn.packageName
        val engine = CapsuleServiceLocator.stateEngine

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim().orEmpty()
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()?.trim().orEmpty()
        val appLabel = resolveAppLabel(pkg)

        // 1. Détection des Appels (Téléphone & Messageries VoIP)
        if (notif.category == Notification.CATEGORY_CALL) {
            val isIncoming = (notif.flags and Notification.FLAG_ONGOING_EVENT) == 0
            engine.upsertDynamicCard(
                ActiveDynamicCard.ActiveCallCard(
                    callerName = title.ifBlank { appLabel },
                    phoneNumberOrApp = text.ifBlank { appLabel },
                    callStartEpochMs = notif.`when`.takeIf { it > 0L } ?: System.currentTimeMillis(),
                    isIncomingRinging = isIncoming,
                    notificationKey = sbn.key
                )
            )
            notif.actions?.let { pendingActionsCache[sbn.key] = it }
            return
        }

        // 2. Détection des Minuteurs & Chronomètres (Horloge système)
        if (isClockOrTimerPackage(pkg, notif)) {
            val isCountDown = extras.getBoolean(Notification.EXTRA_CHRONOMETER_COUNT_DOWN, true)
            val baseWhen = notif.`when`.takeIf { it > 0L } ?: (System.currentTimeMillis() + 60_000L)
            val diffSec = kotlin.math.abs(baseWhen - System.currentTimeMillis()) / 1000L
            engine.upsertDynamicCard(
                ActiveDynamicCard.TimerStopwatchCard(
                    label = title.ifBlank { "Horloge" },
                    isCountDown = isCountDown,
                    targetOrBaseEpochMs = baseWhen,
                    remainingOrElapsedSeconds = diffSec,
                    totalDurationSeconds = (diffSec + 60L).coerceAtLeast(60L),
                    sourcePackage = pkg
                )
            )
            notif.actions?.let { pendingActionsCache[sbn.key] = it }
            return
        }

        // 3. Détection des Mises à jour en direct & Progression (Maps, Play Store, téléchargements, Telegram)
        val progressMax = extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0)
        val progressCurrent = extras.getInt(Notification.EXTRA_PROGRESS, 0)
        val progressIndeterminate = extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE, false)

        if (progressMax > 0 || (progressIndeterminate && isProgressRelevantPackage(pkg))) {
            engine.upsertDynamicCard(
                ActiveDynamicCard.LiveProgressCard(
                    notificationKey = sbn.key,
                    sourcePackage = pkg,
                    appLabel = appLabel,
                    title = title.ifBlank { appLabel },
                    statusText = text.ifBlank { subText.ifBlank { "$progressCurrent / $progressMax" } },
                    progress = progressCurrent.coerceIn(0, progressMax.coerceAtLeast(100)),
                    maxProgress = progressMax.coerceAtLeast(100),
                    isIndeterminate = progressIndeterminate,
                    accentColorArgb = if (notif.color != 0) (notif.color.toLong() or 0xFF000000L) else 0xFF00B0FFL
                )
            )
            return
        }

        // 4. Notifications d'applications sélectionnées (ex: WhatsApp, Telegram, Messages)
        if (isNewPost && (title.isNotBlank() || text.isNotBlank())) {
            val actions = notif.actions?.mapIndexedNotNull { idx, act ->
                val actionTitle = act.title?.toString()?.trim()
                if (!actionTitle.isNullOrBlank()) {
                    CapsuleNotificationAction(label = actionTitle, actionIndex = idx)
                } else {
                    null
                }
            }.orEmpty()
            notif.actions?.let { pendingActionsCache[sbn.key] = it }

            val ruleColor = CapsuleServiceLocator.databaseRepository.allowedApps.value[pkg]?.customAccentColorArgb
            val notifColor = when {
                ruleColor != null -> ruleColor
                notif.color != 0 -> (notif.color.toLong() or 0xFF000000L)
                else -> 0xFF25D366L
            }

            engine.postAppNotification(
                CapsuleNotificationPayload(
                    key = sbn.key,
                    packageName = pkg,
                    appName = appLabel,
                    title = title.ifBlank { appLabel },
                    message = text.ifBlank { subText },
                    appAccentColorArgb = notifColor,
                    actions = actions.take(3)
                )
            )
        }
    }

    private fun isClockOrTimerPackage(pkg: String, notif: Notification): Boolean {
        val extras = notif.extras
        val usesChrono = extras?.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER, false) == true
        val isClockPkg = pkg.contains("deskclock", ignoreCase = true) ||
            pkg.contains("clock", ignoreCase = true)
        return (isClockPkg && (usesChrono || notif.category == Notification.CATEGORY_ALARM || notif.category == Notification.CATEGORY_STOPWATCH))
    }

    private fun isProgressRelevantPackage(pkg: String): Boolean {
        return pkg.contains("maps", ignoreCase = true) ||
            pkg.contains("vending", ignoreCase = true) ||
            pkg.contains("telegram", ignoreCase = true) ||
            pkg.contains("download", ignoreCase = true)
    }

    private fun resolveAppLabel(packageName: String): String = runCatching {
        val appInfo = packageManager.getApplicationInfo(packageName, 0)
        packageManager.getApplicationLabel(appInfo).toString()
    }.getOrDefault(packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() })

    fun dispatchMediaCommand(command: MediaCommandType) {
        val controller = activeControllers.firstOrNull()
        if (controller != null) {
            val controls = controller.transportControls
            when (command) {
                MediaCommandType.PLAY_PAUSE -> {
                    if (controller.playbackState?.state == PlaybackState.STATE_PLAYING) {
                        controls.pause()
                    } else {
                        controls.play()
                    }
                }
                MediaCommandType.NEXT_TRACK -> controls.skipToNext()
                MediaCommandType.PREVIOUS_TRACK -> controls.skipToPrevious()
                MediaCommandType.STOP -> controls.stop()
            }
        } else {
            // Si la carte média provient du Centre de Simulation, met à jour l'état simulé
            val engine = CapsuleServiceLocator.stateEngine
            val currentMedia = engine.uiState.value.activeDynamicCards
                .filterIsInstance<ActiveDynamicCard.MediaPlayerCard>()
                .firstOrNull() ?: return
            when (command) {
                MediaCommandType.PLAY_PAUSE ->
                    engine.upsertDynamicCard(currentMedia.copy(isPlaying = !currentMedia.isPlaying))
                MediaCommandType.NEXT_TRACK ->
                    engine.upsertDynamicCard(
                        currentMedia.copy(
                            title = "Digital Love",
                            artist = "Daft Punk • Discovery",
                            positionMs = 0L
                        )
                    )
                MediaCommandType.PREVIOUS_TRACK ->
                    engine.upsertDynamicCard(
                        currentMedia.copy(
                            title = "One More Time",
                            artist = "Daft Punk • Discovery",
                            positionMs = 0L
                        )
                    )
                MediaCommandType.STOP ->
                    engine.removeDynamicCard(DynamicCardType.MEDIA_PLAYER)
            }
        }
    }

    fun triggerNotificationAction(notificationKey: String, actionIndex: Int) {
        val actions = pendingActionsCache[notificationKey]
        val action = actions?.getOrNull(actionIndex)
        runCatching {
            action?.actionIntent?.send()
        }
        CapsuleServiceLocator.stateEngine.dismissCurrentPopupOrCollapse()
    }

    companion object {
        @Volatile
        var instance: CapsuleNotificationListenerService? = null
            private set
    }
}
