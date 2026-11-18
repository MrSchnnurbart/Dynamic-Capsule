package com.dynamiccapsule.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class MiniEventCategory(val titleFr: String, val titleEn: String) {
    DEVICE("Appareil", "Device"),
    BATTERY("Batterie", "Battery"),
    CONNECTIVITY("Connectivité", "Connectivity"),
    AUDIO_BLUETOOTH("Audio & Bluetooth", "Audio & Bluetooth"),
    DISPLAY_PERIPHERALS("Affichage & Périphériques", "Display & Peripherals"),
    THIRD_PARTY("Intégrations tierces", "Third-Party Integrations");

    fun title(lang: String): String = tr(lang, titleFr, titleEn)
}

@Serializable
enum class MiniCapsuleEventType(
    val category: MiniEventCategory,
    val titleFr: String,
    val titleEn: String,
    val descriptionFr: String,
    val descriptionEn: String,
    val defaultEnabled: Boolean = true
) {
    DEVICE_UNLOCKED(
        category = MiniEventCategory.DEVICE,
        titleFr = "Déverrouillage de l'écran",
        titleEn = "Device Unlock",
        descriptionFr = "Animation de cadenas ouvert au déverrouillage de l'appareil",
        descriptionEn = "Animated open lock when you unlock your device"
    ),
    CHARGING_STARTED(
        category = MiniEventCategory.BATTERY,
        titleFr = "Début de charge",
        titleEn = "Charging Started",
        descriptionFr = "Affiche le niveau de batterie et l'éclair de charge au branchement",
        descriptionEn = "Shows battery percentage and charging bolt when plugged in"
    ),
    CHARGING_STOPPED(
        category = MiniEventCategory.BATTERY,
        titleFr = "Fin de charge / Débranchement",
        titleEn = "Charging Stopped",
        descriptionFr = "Indique le pourcentage final lorsque le chargeur est retiré",
        descriptionEn = "Displays battery level when the charger is unplugged"
    ),
    BATTERY_LOW_5_PERCENT(
        category = MiniEventCategory.BATTERY,
        titleFr = "Alerte batterie faible (5 %)",
        titleEn = "Low Battery Warning (5%)",
        descriptionFr = "Alerte critique animée lorsque la batterie atteint 5 %",
        descriptionEn = "Critical animated alert when battery drops to 5%"
    ),
    BATTERY_SAVER_CHANGED(
        category = MiniEventCategory.BATTERY,
        titleFr = "Mode économiseur d'énergie",
        titleEn = "Battery Saver Mode",
        descriptionFr = "Indique l'activation ou la désactivation de l'économiseur de batterie",
        descriptionEn = "Shown when Battery Saver is enabled or disabled"
    ),
    WIFI_CONNECTED(
        category = MiniEventCategory.CONNECTIVITY,
        titleFr = "Wi-Fi connecté",
        titleEn = "Wi-Fi Connected",
        descriptionFr = "Affiche le réseau Wi-Fi lors de l'établissement de la connexion",
        descriptionEn = "Displays Wi-Fi status when connected to a network"
    ),
    HOTSPOT_CHANGED(
        category = MiniEventCategory.CONNECTIVITY,
        titleFr = "Point d'accès mobile (Hotspot)",
        titleEn = "Mobile Hotspot",
        descriptionFr = "Indique l'activation ou la désactivation du partage de connexion",
        descriptionEn = "Shown when mobile hotspot tethering is toggled"
    ),
    VPN_CHANGED(
        category = MiniEventCategory.CONNECTIVITY,
        titleFr = "VPN activé / désactivé",
        titleEn = "VPN Connected / Disconnected",
        descriptionFr = "Affiche une clé de sécurité lors de la connexion ou coupure d'un VPN",
        descriptionEn = "Shows a security key icon when a VPN tunnel connects or disconnects"
    ),
    AIRPLANE_MODE_CHANGED(
        category = MiniEventCategory.CONNECTIVITY,
        titleFr = "Mode Avion",
        titleEn = "Airplane Mode",
        descriptionFr = "Animation d'avion au basculement du mode Avion",
        descriptionEn = "Animated airplane indicator when Airplane mode is toggled"
    ),
    LOCATION_CHANGED(
        category = MiniEventCategory.CONNECTIVITY,
        titleFr = "Localisation (GPS)",
        titleEn = "Location (GPS)",
        descriptionFr = "Indique l'activation ou la désactivation des services de localisation",
        descriptionEn = "Shown when device location services are enabled or disabled"
    ),
    BLUETOOTH_AUDIO_CHANGED(
        category = MiniEventCategory.AUDIO_BLUETOOTH,
        titleFr = "Casque / Écouteurs Bluetooth",
        titleEn = "Bluetooth Headphones",
        descriptionFr = "Affiche le nom et le niveau de batterie à la connexion/déconnexion",
        descriptionEn = "Shows device name and battery level on Bluetooth audio connection"
    ),
    RINGER_MODE_CHANGED(
        category = MiniEventCategory.AUDIO_BLUETOOTH,
        titleFr = "Mode sonore (Sonnerie / Vibreur / Silencieux)",
        titleEn = "Sound Mode (Ring / Vibrate / Silent)",
        descriptionFr = "Retour visuel instantané au changement de profil audio",
        descriptionEn = "Instant visual feedback when switching between ring, vibrate, and silent"
    ),
    DND_MODE_CHANGED(
        category = MiniEventCategory.AUDIO_BLUETOOTH,
        titleFr = "Ne pas déranger (DND)",
        titleEn = "Do Not Disturb (DND)",
        descriptionFr = "Indique l'activation ou la désactivation du mode Ne pas déranger",
        descriptionEn = "Shown when Do Not Disturb mode is turned on or off"
    ),
    ADAPTIVE_BRIGHTNESS_CHANGED(
        category = MiniEventCategory.DISPLAY_PERIPHERALS,
        titleFr = "Luminosité adaptative",
        titleEn = "Adaptive Brightness",
        descriptionFr = "Affiche le basculement entre luminosité auto et manuelle",
        descriptionEn = "Shown when toggling automatic adaptive brightness"
    ),
    UI_NIGHT_MODE_CHANGED(
        category = MiniEventCategory.DISPLAY_PERIPHERALS,
        titleFr = "Thème Clair / Sombre",
        titleEn = "Light / Dark Theme",
        descriptionFr = "Indique le passage du système en mode Jour ou Nuit",
        descriptionEn = "Shown when the system switches between Light and Dark theme"
    ),
    AUTO_ROTATE_CHANGED(
        category = MiniEventCategory.DISPLAY_PERIPHERALS,
        titleFr = "Rotation automatique",
        titleEn = "Auto-Rotate",
        descriptionFr = "Indique le verrouillage ou déverrouillage de la rotation d'écran",
        descriptionEn = "Shown when screen auto-rotation is locked or unlocked"
    ),
    USB_DEVICE_CHANGED(
        category = MiniEventCategory.DISPLAY_PERIPHERALS,
        titleFr = "Périphérique USB monté / démonté",
        titleEn = "USB Device Mounted / Unmounted",
        descriptionFr = "Affiche la détection ou le retrait d'un périphérique USB OTG / stockage",
        descriptionEn = "Displays USB OTG or storage attachment and removal"
    ),
    AIRPODS_BATTERY_UPDATE(
        category = MiniEventCategory.THIRD_PARTY,
        titleFr = "Batterie AirPods (MaterialPods / OpenPods)",
        titleEn = "AirPods Battery (MaterialPods / OpenPods)",
        descriptionFr = "Affiche les jauges Gauche, Droite et Boîtier via Broadcast compatible",
        descriptionEn = "Displays Left, Right, and Case battery rings via compatible broadcasts"
    );

    fun title(lang: String): String = tr(lang, titleFr, titleEn)
    fun description(lang: String): String = tr(lang, descriptionFr, descriptionEn)
}

@Serializable
enum class RingerStateMode {
    NORMAL,
    VIBRATE,
    SILENT
}

@Serializable
data class AirPodsStatusPayload(
    val deviceName: String = "AirPods Pro",
    val leftPercent: Int = 85,
    val rightPercent: Int = 90,
    val casePercent: Int = 100,
    val isChargingLeft: Boolean = false,
    val isChargingRight: Boolean = false,
    val isChargingCase: Boolean = false
)

@Serializable
data class MiniCapsuleEventPayload(
    val id: String,
    val type: MiniCapsuleEventType,
    val title: String,
    val subtitle: String,
    val isActiveState: Boolean = true,
    val numericValue: Int? = null,
    val accentColorArgb: Long = 0xFF30D158L,
    val ringerMode: RingerStateMode? = null,
    val airPodsStatus: AirPodsStatusPayload? = null,
    val durationMs: Long = 2800L,
    val timestampMs: Long = System.currentTimeMillis()
)
