package com.dynamiccapsule.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class CapsuleGestureType(
    val titleFr: String,
    val titleEn: String,
    val subtitleFr: String,
    val subtitleEn: String
) {
    SINGLE_TAP(
        titleFr = "Appui simple",
        titleEn = "Single Tap",
        subtitleFr = "Exécuté lorsqu'aucune carte dynamique n'est active (ou ouvre la carte active)",
        subtitleEn = "Triggered when no dynamic card is active (or expands the active card)"
    ),
    LONG_PRESS(
        titleFr = "Appui long",
        titleEn = "Long Press",
        subtitleFr = "Maintien appuyé sur la capsule",
        subtitleEn = "Press and hold on the capsule"
    ),
    DOUBLE_TAP(
        titleFr = "Double appui",
        titleEn = "Double Tap",
        subtitleFr = "Deux appuis rapides consécutifs sur la capsule",
        subtitleEn = "Two quick consecutive taps on the capsule"
    ),
    SWIPE_LEFT(
        titleFr = "Balayage gauche",
        titleEn = "Swipe Left",
        subtitleFr = "Glissement horizontal vers la gauche sur la capsule",
        subtitleEn = "Horizontal swipe to the left across the capsule"
    ),
    SWIPE_RIGHT(
        titleFr = "Balayage droite",
        titleEn = "Swipe Right",
        subtitleFr = "Glissement horizontal vers la droite sur la capsule",
        subtitleEn = "Horizontal swipe to the right across the capsule"
    );

    fun title(lang: String): String = tr(lang, titleFr, titleEn)
    fun subtitle(lang: String): String = tr(lang, subtitleFr, subtitleEn)
}

@Serializable
enum class ScrollDirection(val labelFr: String, val labelEn: String) {
    TOP("Défiler vers le haut", "Scroll to top"),
    BOTTOM("Défiler vers le bas", "Scroll to bottom");

    fun label(lang: String): String = tr(lang, labelFr, labelEn)
}

@Serializable
enum class ActionDrawerTab(val labelFr: String, val labelEn: String) {
    SLIDERS("Curseurs (Volume & Luminosité)", "Sliders (Volume & Brightness)"),
    SHORTCUTS("Raccourcis rapides", "Quick Shortcuts"),
    BARCODES("Codes-barres & QR Codes", "Barcodes & QR Codes");

    fun label(lang: String): String = tr(lang, labelFr, labelEn)
}

@Serializable
enum class MediaCommandType(val labelFr: String, val labelEn: String) {
    PLAY_PAUSE("Lecture / Pause", "Play / Pause"),
    NEXT_TRACK("Piste suivante", "Next Track"),
    PREVIOUS_TRACK("Piste précédente", "Previous Track"),
    STOP("Arrêter", "Stop");

    fun label(lang: String): String = tr(lang, labelFr, labelEn)
}

@Serializable
enum class AccessibilityActionType(val labelFr: String, val labelEn: String) {
    NOTIFICATIONS("Ouvrir le panneau de notifications", "Open Notification Shade"),
    QUICK_SETTINGS("Ouvrir les réglages rapides", "Open Quick Settings"),
    RECENTS("Applications récentes", "Recent Apps"),
    HOME("Écran d'accueil", "Home Screen"),
    BACK("Action Retour", "Back Action"),
    POWER_DIALOG("Menu Marche / Arrêt", "Power Menu");

    fun label(lang: String): String = tr(lang, labelFr, labelEn)
}

@Serializable
enum class AiAssistantType(val labelFr: String, val labelEn: String) {
    CHATGPT_VOICE("ChatGPT — Mode Vocal", "ChatGPT — Voice Mode"),
    CHATGPT_TEXT("ChatGPT — Nouveau Chat", "ChatGPT — New Chat"),
    SYSTEM_DEFAULT("Assistant IA par défaut du système", "System Default AI Assistant");

    fun label(lang: String): String = tr(lang, labelFr, labelEn)
}

@Serializable
enum class SystemSettingsTarget(val labelFr: String, val labelEn: String) {
    MAIN_SETTINGS("Paramètres principaux", "Main Settings"),
    WIFI("Paramètres Wi-Fi", "Wi-Fi Settings"),
    BLUETOOTH("Paramètres Bluetooth", "Bluetooth Settings"),
    SOUND("Son et vibration", "Sound & Vibration"),
    DISPLAY("Affichage et luminosité", "Display & Brightness"),
    BATTERY("Batterie", "Battery"),
    ACCESSIBILITY("Accessibilité", "Accessibility");

    fun label(lang: String): String = tr(lang, labelFr, labelEn)
}

@Serializable
sealed interface CapsuleAction {
    val actionTitleFr: String
    val actionTitleEn: String

    fun title(lang: String): String = tr(lang, actionTitleFr, actionTitleEn)

    @Serializable
    data object None : CapsuleAction {
        override val actionTitleFr: String = "Aucune action"
        override val actionTitleEn: String = "None"
    }

    @Serializable
    data class ScrollPage(val direction: ScrollDirection = ScrollDirection.TOP) : CapsuleAction {
        override val actionTitleFr: String = "Défilement (${direction.labelFr})"
        override val actionTitleEn: String = "Scroll (${direction.labelEn})"
    }

    @Serializable
    data object TakeScreenshot : CapsuleAction {
        override val actionTitleFr: String = "Capture d'écran"
        override val actionTitleEn: String = "Take Screenshot"
    }

    @Serializable
    data class LaunchApp(
        val packageName: String = "com.android.settings",
        val appLabel: String = "Settings"
    ) : CapsuleAction {
        override val actionTitleFr: String = "Lancer une application ($appLabel)"
        override val actionTitleEn: String = "Launch Application ($appLabel)"
    }

    @Serializable
    data object OpenAppSearch : CapsuleAction {
        override val actionTitleFr: String = "Recherche d'application"
        override val actionTitleEn: String = "Search Applications"
    }

    @Serializable
    data object ToggleFlashlight : CapsuleAction {
        override val actionTitleFr: String = "Lampe torche"
        override val actionTitleEn: String = "Flashlight"
    }

    @Serializable
    data class OpenShortcutsSlidersBarcodes(
        val initialTab: ActionDrawerTab = ActionDrawerTab.SLIDERS
    ) : CapsuleAction {
        override val actionTitleFr: String = "Raccourcis / Curseurs / Codes-barres (${initialTab.labelFr})"
        override val actionTitleEn: String = "Shortcuts / Sliders / Barcodes (${initialTab.labelEn})"
    }

    @Serializable
    data class ShowWidget(
        val appWidgetId: Int = -1,
        val providerPackage: String = "com.google.android.deskclock",
        val label: String = "Clock & Weather"
    ) : CapsuleAction {
        override val actionTitleFr: String = "Widget ($label)"
        override val actionTitleEn: String = "Widget ($label)"
    }

    @Serializable
    data class MakeDirectCall(
        val phoneNumber: String = "112",
        val contactName: String = "Favorite"
    ) : CapsuleAction {
        override val actionTitleFr: String = "Passer un appel ($contactName)"
        override val actionTitleEn: String = "Make Call ($contactName)"
    }

    @Serializable
    data class SendCustomBroadcast(
        val action: String = "com.dynamiccapsule.CUSTOM_EVENT",
        val targetPackage: String = "",
        val extraKey: String = "source",
        val extraValue: String = "capsule"
    ) : CapsuleAction {
        override val actionTitleFr: String = "Broadcast Intent personnalisé ($action)"
        override val actionTitleEn: String = "Custom Broadcast Intent ($action)"
    }

    @Serializable
    data class ShowQrCode(
        val content: String = "https://github.com/MrSchnnurbart/Dynamic-Capsule",
        val label: String = "QR Code",
        val isBarcode128: Boolean = false
    ) : CapsuleAction {
        override val actionTitleFr: String = if (isBarcode128) {
            "Afficher un Code-barres ($label)"
        } else {
            "Afficher un QR Code ($label)"
        }
        override val actionTitleEn: String = if (isBarcode128) {
            "Show Barcode ($label)"
        } else {
            "Show QR Code ($label)"
        }
    }

    @Serializable
    data class MediaControl(
        val command: MediaCommandType = MediaCommandType.PLAY_PAUSE
    ) : CapsuleAction {
        override val actionTitleFr: String = "Action média (${command.labelFr})"
        override val actionTitleEn: String = "Media Action (${command.labelEn})"
    }

    @Serializable
    data class PerformAccessibilityAction(
        val actionType: AccessibilityActionType = AccessibilityActionType.NOTIFICATIONS
    ) : CapsuleAction {
        override val actionTitleFr: String = "Action d'accessibilité (${actionType.labelFr})"
        override val actionTitleEn: String = "Accessibility Action (${actionType.labelEn})"
    }

    @Serializable
    data class LaunchAiAssistant(
        val assistantType: AiAssistantType = AiAssistantType.CHATGPT_VOICE
    ) : CapsuleAction {
        override val actionTitleFr: String = "Assistant IA (${assistantType.labelFr})"
        override val actionTitleEn: String = "AI Assistant (${assistantType.labelEn})"
    }

    @Serializable
    data object ToggleAutoBrightness : CapsuleAction {
        override val actionTitleFr: String = "Luminosité automatique"
        override val actionTitleEn: String = "Automatic Brightness"
    }

    @Serializable
    data object CycleVolumeRingerMode : CapsuleAction {
        override val actionTitleFr: String = "Mode volume (Sonnerie / Vibreur / Silencieux)"
        override val actionTitleEn: String = "Volume Mode (Ring / Vibrate / Silent)"
    }

    @Serializable
    data object LockScreen : CapsuleAction {
        override val actionTitleFr: String = "Verrouiller l'écran"
        override val actionTitleEn: String = "Lock Screen"
    }

    @Serializable
    data class OpenSystemSettings(
        val target: SystemSettingsTarget = SystemSettingsTarget.MAIN_SETTINGS
    ) : CapsuleAction {
        override val actionTitleFr: String = "Ouvrir les paramètres système (${target.labelFr})"
        override val actionTitleEn: String = "Open System Settings (${target.labelEn})"
    }
}

@Serializable
data class GestureConfiguration(
    val singleTapAction: CapsuleAction = CapsuleAction.OpenShortcutsSlidersBarcodes(ActionDrawerTab.SLIDERS),
    val longPressAction: CapsuleAction = CapsuleAction.OpenAppSearch,
    val doubleTapAction: CapsuleAction = CapsuleAction.TakeScreenshot,
    val swipeLeftAction: CapsuleAction = CapsuleAction.MediaControl(MediaCommandType.NEXT_TRACK),
    val swipeRightAction: CapsuleAction = CapsuleAction.ToggleFlashlight,
    val invertSwipeDirection: Boolean = false,
    val swipeSensitivity: Float = 1.0f
) {
    fun actionFor(gesture: CapsuleGestureType): CapsuleAction = when (gesture) {
        CapsuleGestureType.SINGLE_TAP -> singleTapAction
        CapsuleGestureType.LONG_PRESS -> longPressAction
        CapsuleGestureType.DOUBLE_TAP -> doubleTapAction
        CapsuleGestureType.SWIPE_LEFT -> if (invertSwipeDirection) swipeRightAction else swipeLeftAction
        CapsuleGestureType.SWIPE_RIGHT -> if (invertSwipeDirection) swipeLeftAction else swipeRightAction
    }
}
