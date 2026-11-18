package com.dynamiccapsule.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class DynamicCardType(
    val titleFr: String,
    val titleEn: String,
    val subtitleFr: String,
    val subtitleEn: String,
    val defaultPriority: Int
) {
    MEDIA_PLAYER(
        titleFr = "Lecteur média",
        titleEn = "Media Player",
        subtitleFr = "Pochette, titre, lecture/pause/suivant/précédent et barre de progression",
        subtitleEn = "Album art, title, play/pause/next/previous controls, and progress bar",
        defaultPriority = 0
    ),
    ACTIVE_CALL(
        titleFr = "Appels",
        titleEn = "Phone & VoIP Calls",
        subtitleFr = "Appels téléphoniques et messageries avec chronomètre et actions rapides",
        subtitleEn = "Ongoing phone and messenger calls with duration timer and quick actions",
        defaultPriority = 1
    ),
    TIMER_STOPWATCH(
        titleFr = "Minuteur & chronomètre",
        titleEn = "Timer & Stopwatch",
        subtitleFr = "Suivi en direct de l'horloge système ou du minuteur actif",
        subtitleEn = "Live countdown and stopwatch tracking from the system Clock",
        defaultPriority = 2
    ),
    LIVE_PROGRESS(
        titleFr = "Mises à jour en direct & progression",
        titleEn = "Live Updates & Progress",
        subtitleFr = "Notifications avec ProgressBar : Maps, Play Store, téléchargements, Telegram",
        subtitleEn = "Notifications with ProgressBar: Maps, Play Store, downloads, Telegram",
        defaultPriority = 3
    ),
    FLASHLIGHT(
        titleFr = "Lampe torche",
        titleEn = "Flashlight",
        subtitleFr = "Indicateur d'état actif, réglage d'intensité et extinction rapide",
        subtitleEn = "Active torch status, brightness intensity slider, and one-tap turn off",
        defaultPriority = 4
    ),
    VOLUME_CONTROL(
        titleFr = "Volume",
        titleEn = "Volume Control",
        subtitleFr = "Contrôle visuel fluide lorsque le volume change pendant qu'un média joue",
        subtitleEn = "Visual volume bar when adjusting audio while media is playing",
        defaultPriority = 5
    ),
    CHARGING_STATUS(
        titleFr = "Charge en cours",
        titleEn = "Charging Status",
        subtitleFr = "Pourcentage en direct, puissance estimée et animation d'onde",
        subtitleEn = "Live battery percentage, charging speed, and remaining time",
        defaultPriority = 6
    );

    fun title(lang: String): String = tr(lang, titleFr, titleEn)
    fun subtitle(lang: String): String = tr(lang, subtitleFr, subtitleEn)
}

@Serializable
data class DynamicCardPriorityItem(
    val type: DynamicCardType,
    val enabled: Boolean = true,
    val priorityOrder: Int = type.defaultPriority
)

@Serializable
sealed interface ActiveDynamicCard {
    val cardType: DynamicCardType
    val cardId: String

    @Serializable
    data class MediaPlayerCard(
        override val cardId: String = "media_player",
        val packageName: String = "com.spotify.music",
        val appName: String = "Music",
        val title: String,
        val artist: String,
        val album: String = "",
        val isPlaying: Boolean = true,
        val positionMs: Long = 0L,
        val durationMs: Long = 180_000L,
        val dominantColorArgb: Long = 0xFF7C4DFFL,
        val albumArtBytesBase64: String? = null
    ) : ActiveDynamicCard {
        override val cardType: DynamicCardType = DynamicCardType.MEDIA_PLAYER
        val progressFraction: Float
            get() = if (durationMs > 0L) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
    }

    @Serializable
    data class FlashlightCard(
        override val cardId: String = "flashlight",
        val isOn: Boolean = true,
        val strengthLevel: Int = 3,
        val maxStrengthLevel: Int = 5
    ) : ActiveDynamicCard {
        override val cardType: DynamicCardType = DynamicCardType.FLASHLIGHT
    }

    @Serializable
    data class VolumeControlCard(
        override val cardId: String = "volume_control",
        val currentVolume: Int,
        val maxVolume: Int,
        val outputDeviceName: String = "Speaker",
        val isMuted: Boolean = false
    ) : ActiveDynamicCard {
        override val cardType: DynamicCardType = DynamicCardType.VOLUME_CONTROL
        val volumeFraction: Float
            get() = if (maxVolume > 0) (currentVolume.toFloat() / maxVolume.toFloat()).coerceIn(0f, 1f) else 0f
    }

    @Serializable
    data class LiveProgressCard(
        override val cardId: String = "live_progress",
        val notificationKey: String,
        val sourcePackage: String,
        val appLabel: String,
        val title: String,
        val statusText: String,
        val progress: Int,
        val maxProgress: Int = 100,
        val isIndeterminate: Boolean = false,
        val accentColorArgb: Long = 0xFF0A84FFL
    ) : ActiveDynamicCard {
        override val cardType: DynamicCardType = DynamicCardType.LIVE_PROGRESS
        val progressFraction: Float
            get() = if (maxProgress > 0) (progress.toFloat() / maxProgress.toFloat()).coerceIn(0f, 1f) else 0f
    }

    @Serializable
    data class TimerStopwatchCard(
        override val cardId: String = "timer_stopwatch",
        val label: String = "Timer",
        val isCountDown: Boolean = true,
        val targetOrBaseEpochMs: Long,
        val remainingOrElapsedSeconds: Long,
        val totalDurationSeconds: Long = 300L,
        val isPaused: Boolean = false,
        val sourcePackage: String = "com.google.android.deskclock"
    ) : ActiveDynamicCard {
        override val cardType: DynamicCardType = DynamicCardType.TIMER_STOPWATCH
    }

    @Serializable
    data class ActiveCallCard(
        override val cardId: String = "active_call",
        val callerName: String,
        val phoneNumberOrApp: String = "HD Audio",
        val callStartEpochMs: Long = System.currentTimeMillis(),
        val elapsedSeconds: Long = 0L,
        val isMuted: Boolean = false,
        val isSpeakerOn: Boolean = false,
        val isIncomingRinging: Boolean = false,
        val notificationKey: String? = null
    ) : ActiveDynamicCard {
        override val cardType: DynamicCardType = DynamicCardType.ACTIVE_CALL
    }

    @Serializable
    data class ChargingStatusCard(
        override val cardId: String = "charging_status",
        val batteryPercentage: Int,
        val chargingTypeLabel: String = "USB-C Fast Charge",
        val estimatedMinutesRemaining: Int = 25,
        val voltageMv: Int = 4250,
        val temperatureTenthsC: Int = 310
    ) : ActiveDynamicCard {
        override val cardType: DynamicCardType = DynamicCardType.CHARGING_STATUS
    }
}
