package com.dynamiccapsule.core.system.monitor

import android.app.KeyguardManager
import android.app.NotificationManager
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.database.ContentObserver
import android.hardware.usb.UsbManager
import android.location.LocationManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.dynamiccapsule.core.model.ActiveDynamicCard
import com.dynamiccapsule.core.model.AirPodsStatusPayload
import com.dynamiccapsule.core.model.DynamicCardType
import com.dynamiccapsule.core.model.MiniCapsuleEventPayload
import com.dynamiccapsule.core.model.MiniCapsuleEventType
import com.dynamiccapsule.core.model.RingerStateMode
import com.dynamiccapsule.core.system.engine.CapsuleStateEngine
import java.util.UUID

/**
 * Capteur système surveillant l'ensemble des 17 événements "Mini Capsule"
 * ainsi que les états de batterie/charge et de volume audio, avec cycle de vie strict
 * (enregistrement/désenregistrement symétrique sans fuite mémoire).
 */
class SystemEventMonitor(
    private val context: Context,
    private val stateEngine: CapsuleStateEngine
) {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())

    private val connectivityManager =
        appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    private val audioManager =
        appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val powerManager =
        appContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
    private val notificationManager =
        appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
    private val locationManager =
        appContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    private val keyguardManager =
        appContext.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager

    private var isStarted = false
    private var lastBatteryPercentage = -1
    private var wasCharging = false
    private var lowBattery5PercentAlerted = false
    private var lastNightModeState: Boolean? = null
    private var wasWifiConnected = false
    private var wasVpnActive = false

    private val systemBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            val action = intent?.action ?: return
            handleBroadcastIntent(action, intent)
        }
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            val hasWifi = networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
            val hasVpn = networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)

            if (hasWifi && !wasWifiConnected) {
                wasWifiConnected = true
                stateEngine.postMiniCapsuleEvent(
                    MiniCapsuleEventPayload(
                        id = UUID.randomUUID().toString(),
                        type = MiniCapsuleEventType.WIFI_CONNECTED,
                        title = "Wi-Fi Connecté",
                        subtitle = "Connexion réseau active",
                        accentColorArgb = 0xFF2979FFL
                    )
                )
            } else if (!hasWifi) {
                wasWifiConnected = false
            }

            if (hasVpn != wasVpnActive) {
                wasVpnActive = hasVpn
                stateEngine.postMiniCapsuleEvent(
                    MiniCapsuleEventPayload(
                        id = UUID.randomUUID().toString(),
                        type = MiniCapsuleEventType.VPN_CHANGED,
                        title = if (hasVpn) "VPN Activé" else "VPN Désactivé",
                        subtitle = if (hasVpn) "Trafic réseau chiffré" else "Tunnel VPN fermé",
                        isActiveState = hasVpn,
                        accentColorArgb = if (hasVpn) 0xFF7C4DFFL else 0xFF90A4AEL
                    )
                )
            }
        }

        override fun onLost(network: Network) {
            wasWifiConnected = false
        }
    }

    private val settingsObserver = object : ContentObserver(mainHandler) {
        override fun onChange(selfChange: Boolean, uri: android.net.Uri?) {
            when (uri) {
                Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS_MODE) -> {
                    val mode = runCatching {
                        Settings.System.getInt(
                            appContext.contentResolver,
                            Settings.System.SCREEN_BRIGHTNESS_MODE
                        )
                    }.getOrDefault(Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
                    val isAuto = mode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
                    stateEngine.postMiniCapsuleEvent(
                        MiniCapsuleEventPayload(
                            id = UUID.randomUUID().toString(),
                            type = MiniCapsuleEventType.ADAPTIVE_BRIGHTNESS_CHANGED,
                            title = if (isAuto) "Luminosité Auto" else "Luminosité Manuelle",
                            subtitle = if (isAuto) "Adaptation au capteur activée" else "Réglage fixe",
                            isActiveState = isAuto,
                            accentColorArgb = 0xFFFFD54FL
                        )
                    )
                }
                Settings.System.getUriFor(Settings.System.ACCELEROMETER_ROTATION) -> {
                    val autoRotate = runCatching {
                        Settings.System.getInt(
                            appContext.contentResolver,
                            Settings.System.ACCELEROMETER_ROTATION
                        ) == 1
                    }.getOrDefault(false)
                    stateEngine.postMiniCapsuleEvent(
                        MiniCapsuleEventPayload(
                            id = UUID.randomUUID().toString(),
                            type = MiniCapsuleEventType.AUTO_ROTATE_CHANGED,
                            title = if (autoRotate) "Rotation Auto" else "Portrait Verrouillé",
                            subtitle = if (autoRotate) "Orientation libre" else "Rotation désactivée",
                            isActiveState = autoRotate,
                            accentColorArgb = 0xFF26C6DAL
                        )
                    )
                }
            }
        }
    }

    fun start() {
        if (isStarted) return
        isStarted = true

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
            addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED)
            addAction(LocationManager.PROVIDERS_CHANGED_ACTION)
            addAction(ACTION_WIFI_AP_STATE_CHANGED)
            addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
            addAction(ACTION_VOLUME_CHANGED)
            addAction(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(ACTION_BLUETOOTH_BATTERY_CHANGED)
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            // Intégrations tierces AirPods (MaterialPods / OpenPods / Podroid / Custom)
            addAction(ACTION_MATERIALPODS_STATUS)
            addAction(ACTION_OPENPODS_STATUS)
            addAction(ACTION_PODROID_STATUS)
            addAction(ACTION_CAPSULE_AIRPODS_BROADCAST)
        }

        runCatching {
            ContextCompat.registerReceiver(
                appContext,
                systemBroadcastReceiver,
                filter,
                ContextCompat.RECEIVER_EXPORTED
            )
        }

        runCatching {
            val request = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .addTransportType(NetworkCapabilities.TRANSPORT_VPN)
                .build()
            connectivityManager?.registerNetworkCallback(request, networkCallback)
        }

        runCatching {
            appContext.contentResolver.registerContentObserver(
                Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS_MODE),
                false,
                settingsObserver
            )
            appContext.contentResolver.registerContentObserver(
                Settings.System.getUriFor(Settings.System.ACCELEROMETER_ROTATION),
                false,
                settingsObserver
            )
        }

        lastNightModeState = (appContext.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        stateEngine.setDeviceLocked(keyguardManager?.isKeyguardLocked == true)
    }

    fun stop() {
        if (!isStarted) return
        isStarted = false
        runCatching { appContext.unregisterReceiver(systemBroadcastReceiver) }
        runCatching { connectivityManager?.unregisterNetworkCallback(networkCallback) }
        runCatching { appContext.contentResolver.unregisterContentObserver(settingsObserver) }
    }

    fun onConfigurationChanged(newConfig: Configuration) {
        val isNight = (newConfig.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val previous = lastNightModeState
        lastNightModeState = isNight
        if (previous != null && previous != isNight) {
            stateEngine.postMiniCapsuleEvent(
                MiniCapsuleEventPayload(
                    id = UUID.randomUUID().toString(),
                    type = MiniCapsuleEventType.UI_NIGHT_MODE_CHANGED,
                    title = if (isNight) "Thème Sombre" else "Thème Clair",
                    subtitle = if (isNight) "Mode nuit activé" else "Mode jour activé",
                    isActiveState = isNight,
                    accentColorArgb = if (isNight) 0xFF9575CDL else 0xFFFFB300L
                )
            )
        }
    }

    private fun handleBroadcastIntent(action: String, intent: Intent) {
        when (action) {
            Intent.ACTION_USER_PRESENT -> {
                stateEngine.setDeviceLocked(false)
                stateEngine.postMiniCapsuleEvent(
                    MiniCapsuleEventPayload(
                        id = UUID.randomUUID().toString(),
                        type = MiniCapsuleEventType.DEVICE_UNLOCKED,
                        title = "Déverrouillé",
                        subtitle = "Appareil prêt",
                        accentColorArgb = 0xFF4CAF50L
                    )
                )
            }
            Intent.ACTION_SCREEN_OFF -> {
                stateEngine.setDeviceLocked(keyguardManager?.isKeyguardLocked == true)
            }
            Intent.ACTION_POWER_CONNECTED -> {
                val pct = lastBatteryPercentage.coerceIn(1, 100)
                wasCharging = true
                stateEngine.postMiniCapsuleEvent(
                    MiniCapsuleEventPayload(
                        id = UUID.randomUUID().toString(),
                        type = MiniCapsuleEventType.CHARGING_STARTED,
                        title = "En charge",
                        subtitle = "$pct % • Chargeur connecté",
                        numericValue = pct,
                        accentColorArgb = 0xFF00E676L
                    )
                )
            }
            Intent.ACTION_POWER_DISCONNECTED -> {
                val pct = lastBatteryPercentage.coerceIn(1, 100)
                wasCharging = false
                stateEngine.removeDynamicCard(DynamicCardType.CHARGING_STATUS)
                stateEngine.postMiniCapsuleEvent(
                    MiniCapsuleEventPayload(
                        id = UUID.randomUUID().toString(),
                        type = MiniCapsuleEventType.CHARGING_STOPPED,
                        title = "Fin de charge",
                        subtitle = "Batterie à $pct %",
                        numericValue = pct,
                        isActiveState = false,
                        accentColorArgb = 0xFF90A4AEL
                    )
                )
            }
            Intent.ACTION_BATTERY_CHANGED -> {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
                val pct = ((level * 100f) / scale).toInt().coerceIn(0, 100)
                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL
                val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
                val voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 4100)
                val temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 300)

                lastBatteryPercentage = pct

                if (pct <= 5 && !isCharging && !lowBattery5PercentAlerted) {
                    lowBattery5PercentAlerted = true
                    stateEngine.postMiniCapsuleEvent(
                        MiniCapsuleEventPayload(
                            id = UUID.randomUUID().toString(),
                            type = MiniCapsuleEventType.BATTERY_LOW_5_PERCENT,
                            title = "Batterie faible",
                            subtitle = "$pct % restants • Branchez le chargeur",
                            numericValue = pct,
                            accentColorArgb = 0xFFFF1744L,
                            durationMs = 4200L
                        )
                    )
                } else if (pct > 7 || isCharging) {
                    lowBattery5PercentAlerted = false
                }

                if (isCharging && plugged != 0) {
                    val plugLabel = when (plugged) {
                        BatteryManager.BATTERY_PLUGGED_AC -> "Charge secteur rapide"
                        BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Charge sans fil Qi"
                        else -> "Charge USB"
                    }
                    stateEngine.upsertDynamicCard(
                        ActiveDynamicCard.ChargingStatusCard(
                            batteryPercentage = pct,
                            chargingTypeLabel = plugLabel,
                            estimatedMinutesRemaining = ((100 - pct) * 0.65f).toInt().coerceAtLeast(1),
                            voltageMv = voltage,
                            temperatureTenthsC = temp
                        )
                    )
                } else {
                    stateEngine.removeDynamicCard(DynamicCardType.CHARGING_STATUS)
                }
            }
            PowerManager.ACTION_POWER_SAVE_MODE_CHANGED -> {
                val isPowerSave = powerManager?.isPowerSaveMode == true
                stateEngine.postMiniCapsuleEvent(
                    MiniCapsuleEventPayload(
                        id = UUID.randomUUID().toString(),
                        type = MiniCapsuleEventType.BATTERY_SAVER_CHANGED,
                        title = if (isPowerSave) "Économiseur actif" else "Économiseur désactivé",
                        subtitle = if (isPowerSave) "Autonomie prolongée" else "Performances normales",
                        isActiveState = isPowerSave,
                        accentColorArgb = 0xFFFF9100L
                    )
                )
            }
            Intent.ACTION_AIRPLANE_MODE_CHANGED -> {
                val enabled = intent.getBooleanExtra("state", false)
                stateEngine.postMiniCapsuleEvent(
                    MiniCapsuleEventPayload(
                        id = UUID.randomUUID().toString(),
                        type = MiniCapsuleEventType.AIRPLANE_MODE_CHANGED,
                        title = if (enabled) "Mode Avion" else "Réseau Rétabli",
                        subtitle = if (enabled) "Radios désactivées" else "Mode avion désactivé",
                        isActiveState = enabled,
                        accentColorArgb = 0xFF40C4FFL
                    )
                )
            }
            LocationManager.PROVIDERS_CHANGED_ACTION -> {
                val enabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    locationManager?.isLocationEnabled == true
                } else {
                    true
                }
                stateEngine.postMiniCapsuleEvent(
                    MiniCapsuleEventPayload(
                        id = UUID.randomUUID().toString(),
                        type = MiniCapsuleEventType.LOCATION_CHANGED,
                        title = if (enabled) "Localisation activée" else "Localisation coupée",
                        subtitle = if (enabled) "Services GPS actifs" else "Position désactivée",
                        isActiveState = enabled,
                        accentColorArgb = 0xFF00E5FFL
                    )
                )
            }
            ACTION_WIFI_AP_STATE_CHANGED -> {
                val state = intent.getIntExtra("wifi_state", 11)
                // WIFI_AP_STATE_ENABLED = 13, WIFI_AP_STATE_DISABLED = 11
                if (state == 13 || state == 11) {
                    val enabled = state == 13
                    stateEngine.postMiniCapsuleEvent(
                        MiniCapsuleEventPayload(
                            id = UUID.randomUUID().toString(),
                            type = MiniCapsuleEventType.HOTSPOT_CHANGED,
                            title = if (enabled) "Point d'accès actif" else "Point d'accès arrêté",
                            subtitle = if (enabled) "Partage de connexion activé" else "Hotspot désactivé",
                            isActiveState = enabled,
                            accentColorArgb = 0xFF00BFA5L
                        )
                    )
                }
            }
            AudioManager.RINGER_MODE_CHANGED_ACTION -> {
                val ringer = audioManager?.ringerMode ?: AudioManager.RINGER_MODE_NORMAL
                val (title, sub, mode, color) = when (ringer) {
                    AudioManager.RINGER_MODE_SILENT ->
                        Quadruple("Mode Silencieux", "Aucun son ni vibration", RingerStateMode.SILENT, 0xFFFF5252L)
                    AudioManager.RINGER_MODE_VIBRATE ->
                        Quadruple("Mode Vibreur", "Vibrations uniquement", RingerStateMode.VIBRATE, 0xFFFFB300L)
                    else ->
                        Quadruple("Mode Sonnerie", "Sonnerie et notifications actives", RingerStateMode.NORMAL, 0xFF00E676L)
                }
                stateEngine.postMiniCapsuleEvent(
                    MiniCapsuleEventPayload(
                        id = UUID.randomUUID().toString(),
                        type = MiniCapsuleEventType.RINGER_MODE_CHANGED,
                        title = title,
                        subtitle = sub,
                        ringerMode = mode,
                        accentColorArgb = color
                    )
                )
            }
            ACTION_VOLUME_CHANGED -> {
                val streamType = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", -1)
                if (streamType == AudioManager.STREAM_MUSIC) {
                    val isMusicActive = audioManager?.isMusicActive == true ||
                        stateEngine.uiState.value.activeDynamicCards.any { it.cardType == DynamicCardType.MEDIA_PLAYER }
                    if (isMusicActive) {
                        val currentVol = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 8
                        val maxVol = (audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15).coerceAtLeast(1)
                        stateEngine.upsertDynamicCard(
                            ActiveDynamicCard.VolumeControlCard(
                                currentVolume = currentVol,
                                maxVolume = maxVol,
                                outputDeviceName = "Audio Média",
                                isMuted = currentVol == 0
                            )
                        )
                    }
                }
            }
            NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED -> {
                val filterMode = notificationManager?.currentInterruptionFilter
                    ?: NotificationManager.INTERRUPTION_FILTER_ALL
                val dndActive = filterMode != NotificationManager.INTERRUPTION_FILTER_ALL
                stateEngine.postMiniCapsuleEvent(
                    MiniCapsuleEventPayload(
                        id = UUID.randomUUID().toString(),
                        type = MiniCapsuleEventType.DND_MODE_CHANGED,
                        title = if (dndActive) "Ne pas déranger" else "Notifications actives",
                        subtitle = if (dndActive) "Appels et alertes filtrés" else "Mode Ne pas déranger désactivé",
                        isActiveState = dndActive,
                        accentColorArgb = 0xFF7E57C2L
                    )
                )
            }
            BluetoothDevice.ACTION_ACL_CONNECTED,
            BluetoothDevice.ACTION_ACL_DISCONNECTED,
            ACTION_BLUETOOTH_BATTERY_CHANGED -> {
                val isConnected = action != BluetoothDevice.ACTION_ACL_DISCONNECTED
                val batteryLevel = intent.getIntExtra("android.bluetooth.device.extra.BATTERY_LEVEL", -1)
                val deviceName = runCatching {
                    val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    device?.name
                }.getOrNull() ?: "Audio Bluetooth"

                val subtitle = when {
                    !isConnected -> "Déconnecté"
                    batteryLevel in 0..100 -> "Connecté • Batterie $batteryLevel %"
                    else -> "Périphérique audio connecté"
                }
                stateEngine.postMiniCapsuleEvent(
                    MiniCapsuleEventPayload(
                        id = UUID.randomUUID().toString(),
                        type = MiniCapsuleEventType.BLUETOOTH_AUDIO_CHANGED,
                        title = deviceName,
                        subtitle = subtitle,
                        isActiveState = isConnected,
                        numericValue = batteryLevel.takeIf { it in 0..100 },
                        accentColorArgb = 0xFF448AFFL
                    )
                )
            }
            UsbManager.ACTION_USB_DEVICE_ATTACHED,
            UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                val attached = action == UsbManager.ACTION_USB_DEVICE_ATTACHED
                stateEngine.postMiniCapsuleEvent(
                    MiniCapsuleEventPayload(
                        id = UUID.randomUUID().toString(),
                        type = MiniCapsuleEventType.USB_DEVICE_CHANGED,
                        title = if (attached) "Périphérique USB monté" else "Périphérique USB retiré",
                        subtitle = if (attached) "Accessoire USB OTG détecté" else "Déconnexion sécurisée",
                        isActiveState = attached,
                        accentColorArgb = 0xFF66BB6AL
                    )
                )
            }
            ACTION_MATERIALPODS_STATUS,
            ACTION_OPENPODS_STATUS,
            ACTION_PODROID_STATUS,
            ACTION_CAPSULE_AIRPODS_BROADCAST -> {
                val left = intent.getIntExtra("left", intent.getIntExtra("leftPod", 85)).coerceIn(0, 100)
                val right = intent.getIntExtra("right", intent.getIntExtra("rightPod", 90)).coerceIn(0, 100)
                val casePct = intent.getIntExtra("case", intent.getIntExtra("casePod", 100)).coerceIn(0, 100)
                val name = intent.getStringExtra("name") ?: "AirPods Pro"
                stateEngine.postMiniCapsuleEvent(
                    MiniCapsuleEventPayload(
                        id = UUID.randomUUID().toString(),
                        type = MiniCapsuleEventType.AIRPODS_BATTERY_UPDATE,
                        title = name,
                        subtitle = "G: $left% • D: $right% • Boîtier: $casePct%",
                        accentColorArgb = 0xFF00E5FFL,
                        airPodsStatus = AirPodsStatusPayload(
                            deviceName = name,
                            leftPercent = left,
                            rightPercent = right,
                            casePercent = casePct,
                            isChargingLeft = intent.getBooleanExtra("chargingLeft", false),
                            isChargingRight = intent.getBooleanExtra("chargingRight", false),
                            isChargingCase = intent.getBooleanExtra("chargingCase", false)
                        ),
                        durationMs = 3800L
                    )
                )
            }
        }
    }

    private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

    companion object {
        private const val ACTION_WIFI_AP_STATE_CHANGED = "android.net.wifi.WIFI_AP_STATE_CHANGED"
        private const val ACTION_VOLUME_CHANGED = "android.media.VOLUME_CHANGED_ACTION"
        private const val ACTION_BLUETOOTH_BATTERY_CHANGED = "android.bluetooth.device.action.BATTERY_LEVEL_CHANGED"

        const val ACTION_MATERIALPODS_STATUS = "com.materialpods.ACTION_PODS_STATUS"
        const val ACTION_OPENPODS_STATUS = "io.github.openpods.ACTION_STATUS"
        const val ACTION_PODROID_STATUS = "podroid.intent.action.PODS_STATUS"
        const val ACTION_CAPSULE_AIRPODS_BROADCAST = "com.dynamiccapsule.AIRPODS_UPDATE"
    }
}
