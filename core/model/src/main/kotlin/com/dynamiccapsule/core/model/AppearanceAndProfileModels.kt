package com.dynamiccapsule.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class ScreenOrientationMode(val labelFr: String, val labelEn: String) {
    PORTRAIT("Portrait", "Portrait"),
    LANDSCAPE("Paysage", "Landscape");

    fun label(lang: String): String = tr(lang, labelFr, labelEn)
}

@Serializable
enum class CameraCutoutPlacement(val labelFr: String, val labelEn: String) {
    LEFT("Gauche", "Left"),
    CENTER("Centre", "Center"),
    RIGHT("Droite", "Right"),
    NONE("Sous barre", "Below bar");

    fun label(lang: String): String = tr(lang, labelFr, labelEn)
}

@Serializable
data class ScreenProfile(
    val profileKey: String = "1080x2400_PORTRAIT",
    val resolutionWidth: Int = 1080,
    val resolutionHeight: Int = 2400,
    val orientation: ScreenOrientationMode = ScreenOrientationMode.PORTRAIT,
    val xOffsetPx: Int = 0,
    val yOffsetPx: Int = 12,
    val compactWidthDp: Int = 124,
    val compactHeightDp: Int = 34,
    val expandedWidthDp: Int = 362,
    val expandedHeightDp: Int = 184,
    val cornerRadiusDp: Int = 32,
    val cutoutPaddingDp: Int = 8,
    val cameraPlacement: CameraCutoutPlacement = CameraCutoutPlacement.CENTER,
    val cameraCutoutWidthDp: Int = 30,
    val cameraCutoutHeightDp: Int = 28,
    val cameraSafePaddingDp: Int = 8,
    val miniCapsuleScalePercent: Int = 100
) {
    companion object {
        fun buildKey(width: Int, height: Int, orientation: ScreenOrientationMode): String =
            "${width}x${height}_${orientation.name}"
    }
}

@Serializable
enum class NotificationIconShape(val labelFr: String, val labelEn: String) {
    CIRCLE("Cercle", "Circle"),
    SQUIRCLE("Squircle (Apple)", "Squircle (Apple)"),
    ROUNDED_SQUARE("Carré arrondi", "Rounded Square"),
    TEARDROP("Goutte d'eau", "Teardrop"),
    HEXAGON("Hexagone", "Hexagon");

    fun label(lang: String): String = tr(lang, labelFr, labelEn)
}

@Serializable
enum class NotificationColorMode(val labelFr: String, val labelEn: String) {
    DEFAULT("Sobre (Blanc & Gris Apple)", "Sober (Apple White & Gray)"),
    ADAPTIVE_APP("Couleur de l'application", "Adaptive App Color"),
    MATERIAL_YOU("Dynamique Material You", "Dynamic Material You");

    fun label(lang: String): String = tr(lang, labelFr, labelEn)
}

@Serializable
enum class NotificationButtonStyle(val labelFr: String, val labelEn: String) {
    FILLED_PILL("Pilule iOS", "iOS Filled Pill"),
    OUTLINED("Contour fin", "Outlined"),
    CHIP("Verre sombre", "Dark Glass Chip"),
    TEXT_ONLY("Texte épuré", "Minimal Text");

    fun label(lang: String): String = tr(lang, labelFr, labelEn)
}

@Serializable
data class NotificationVisualConfig(
    val enabledGlobally: Boolean = true,
    val autoExpandOnArrival: Boolean = false,
    val swipeUpToDismiss: Boolean = true,
    val iconShape: NotificationIconShape = NotificationIconShape.CIRCLE,
    val colorMode: NotificationColorMode = NotificationColorMode.ADAPTIVE_APP,
    val actionButtonStyle: NotificationButtonStyle = NotificationButtonStyle.CHIP,
    val textSizeScale: Float = 1.0f,
    val displayDurationSeconds: Int = 5
)

@Serializable
data class CapsuleNotificationAction(
    val label: String,
    val actionIndex: Int
)

@Serializable
data class CapsuleNotificationPayload(
    val key: String,
    val packageName: String,
    val appName: String,
    val title: String,
    val message: String,
    val timestampMs: Long = System.currentTimeMillis(),
    val appAccentColorArgb: Long = 0xFF30D158L,
    val actions: List<CapsuleNotificationAction> = emptyList()
)

@Serializable
data class CapsuleAppearanceConfig(
    val dayBackgroundColorArgb: Long = 0xFF000000L,
    val dayBackgroundAlpha: Float = 1.0f,
    val nightBackgroundColorArgb: Long = 0xFF000000L,
    val nightBackgroundAlpha: Float = 1.0f,
    val borderWidthDp: Float = 0.5f,
    val borderColorArgb: Long = 0xFF1C1C1EL,
    val punchHoleGradientEnabled: Boolean = false,
    val gradientStartColorArgb: Long = 0xFF2C2C2EL,
    val gradientEndColorArgb: Long = 0xFF3A3A3CL,
    val shadowElevationDp: Float = 4f,
    val softGlowEnabled: Boolean = false,
    val touchRippleEffectEnabled: Boolean = true,
    val showIdleCutoutRing: Boolean = true,
    val idleOpacityPercent: Int = 0,
    val showCameraSafeZoneGuide: Boolean = false,
    val useAlbumArtColorInMedia: Boolean = false,
    val autoCollapseExpandedSeconds: Int = 0
)

@Serializable
enum class AnimationOpenStyle(val labelFr: String, val labelEn: String) {
    BOUNCE("Comme un Fruit (Ressort Apple)", "Like a Fruit (Apple Spring)"),
    FLUID("Comme un Robot (Fluide Android)", "Like a Robot (Smooth Android)");

    fun label(lang: String): String = tr(lang, labelFr, labelEn)
}

@Serializable
data class CapsuleAnimationConfig(
    val openStyle: AnimationOpenStyle = AnimationOpenStyle.BOUNCE,
    val durationMs: Int = 340,
    val bounceIntensity: Float = 0.42f,
    val reduceAnimations: Boolean = false,
    val hapticsOnOpen: Boolean = true,
    val hapticsOnClose: Boolean = false,
    val hapticsOnCardChange: Boolean = true
)

@Serializable
enum class AppThemeMode(val labelFr: String, val labelEn: String) {
    SYSTEM("Automatique", "Follow System"),
    LIGHT("Clair", "Light"),
    DARK("Noir (OLED)", "Black (OLED)");

    fun label(lang: String): String = tr(lang, labelFr, labelEn)
}

@Serializable
enum class AppFontFamily(val labelFr: String, val labelEn: String) {
    SYSTEM_SANS("SF / Sans-Serif", "SF / Sans-Serif"),
    ROUNDED_GEOMETRIC("Arrondie", "Rounded"),
    TECH_MONOSPACE("Monospace", "Monospace"),
    ELEGANT_SERIF("Serif", "Serif");

    fun label(lang: String): String = tr(lang, labelFr, labelEn)
}

@Serializable
enum class AppIconVariant(val labelFr: String, val labelEn: String, val aliasClassName: String) {
    DEFAULT("Classique Noir", "Classic Black", "com.dynamiccapsule.app.MainActivityDefaultAlias"),
    NEON_PULSE("Bleu Nuit", "Midnight Blue", "com.dynamiccapsule.app.MainActivityNeonAlias"),
    MINIMAL_MONO("Monochrome", "Monochrome", "com.dynamiccapsule.app.MainActivityMinimalAlias"),
    AMOLED_GOLD("Titane Or", "Titanium Gold", "com.dynamiccapsule.app.MainActivityGoldAlias");

    fun label(lang: String): String = tr(lang, labelFr, labelEn)
}

@Serializable
data class AppGeneralSettings(
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val dynamicColorsEnabled: Boolean = false,
    val fontFamily: AppFontFamily = AppFontFamily.SYSTEM_SANS,
    val iconVariant: AppIconVariant = AppIconVariant.DEFAULT,
    val languageCode: String = "fr",
    val showCardsOnLockScreen: Boolean = true,
    val shizukuEnabled: Boolean = false,
    val hiddenStatusBarIcons: Set<String> = setOf("clock", "battery", "wifi"),
    val hasCompletedWelcomeGuide: Boolean = false,
    val lastSeenChangelogVersionCode: Int = AppProjectInfo.VERSION_CODE,
    val autoCheckUpdatesOnLaunch: Boolean = true
)

@Serializable
enum class CapsuleDisplayMode {
    HIDDEN,
    IDLE_CUTOUT,
    MINI_EVENT_POPUP,
    NOTIFICATION_POPUP,
    COMPACT_CARD,
    SPLIT_COMPACT_CARD,
    EXPANDED_CARD,
    ACTION_DRAWER,
    LIVE_PROFILE_EDITOR
}

@Serializable
sealed interface ActiveDrawerContent {
    @Serializable
    data class SlidersShortcutsBarcodes(
        val selectedTab: ActionDrawerTab = ActionDrawerTab.SLIDERS,
        val brightnessPercent: Int = 70,
        val isAutoBrightness: Boolean = true,
        val mediaVolumePercent: Int = 65
    ) : ActiveDrawerContent

    @Serializable
    data class AppSearchDrawer(
        val query: String = ""
    ) : ActiveDrawerContent

    @Serializable
    data class QrBarcodeViewer(
        val content: String,
        val label: String,
        val isBarcode128: Boolean
    ) : ActiveDrawerContent

    @Serializable
    data class WidgetContainer(
        val appWidgetId: Int,
        val providerPackage: String,
        val label: String
    ) : ActiveDrawerContent
}

@Serializable
data class CapsuleOverlayUiState(
    val isMasterEnabled: Boolean = true,
    val displayMode: CapsuleDisplayMode = CapsuleDisplayMode.IDLE_CUTOUT,
    val activeDynamicCards: List<ActiveDynamicCard> = emptyList(),
    val selectedCardIndex: Int = 0,
    val currentMiniEvent: MiniCapsuleEventPayload? = null,
    val currentNotification: CapsuleNotificationPayload? = null,
    val activeDrawerContent: ActiveDrawerContent? = null,
    val isLiveProfileEditorOpen: Boolean = false,
    val currentProfile: ScreenProfile = ScreenProfile(),
    val appearance: CapsuleAppearanceConfig = CapsuleAppearanceConfig(),
    val animation: CapsuleAnimationConfig = CapsuleAnimationConfig(),
    val notificationStyle: NotificationVisualConfig = NotificationVisualConfig(),
    val gestureConfig: GestureConfiguration = GestureConfiguration(),
    val generalSettings: AppGeneralSettings = AppGeneralSettings(),
    val isNightMode: Boolean = true,
    val isDeviceLocked: Boolean = false
) {
    val primaryDynamicCard: ActiveDynamicCard?
        get() = activeDynamicCards.getOrNull(selectedCardIndex.coerceIn(0, (activeDynamicCards.size - 1).coerceAtLeast(0)))
            ?: activeDynamicCards.firstOrNull()

    val secondaryDynamicCard: ActiveDynamicCard?
        get() = activeDynamicCards.withIndex()
            .firstOrNull { (idx, _) -> idx != selectedCardIndex }
            ?.value
}
