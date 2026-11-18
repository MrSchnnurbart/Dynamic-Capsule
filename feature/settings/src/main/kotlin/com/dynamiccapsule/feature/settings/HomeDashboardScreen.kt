package com.dynamiccapsule.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.dynamiccapsule.core.model.CameraCutoutPlacement
import com.dynamiccapsule.core.model.DynamicCardType
import com.dynamiccapsule.core.model.MiniCapsuleEventType
import com.dynamiccapsule.core.model.ScreenOrientationMode
import com.dynamiccapsule.core.model.tr
import com.dynamiccapsule.core.system.CapsuleServiceLocator
import com.dynamiccapsule.feature.overlay.action.CapsuleActionExecutor
import com.dynamiccapsule.feature.overlay.service.CapsuleAccessibilityService
import com.dynamiccapsule.feature.overlay.ui.CapsuleOverlayRoot
import kotlin.math.roundToInt

enum class SettingsDestination {
    DASHBOARD,
    ONBOARDING_PERMISSIONS,
    MINI_EVENTS,
    DYNAMIC_CARDS,
    GESTURES_ACTIONS,
    APP_NOTIFICATIONS,
    APPEARANCE_PROFILES,
    APP_GENERAL_SETTINGS,
    SIMULATION_CENTER,
    WELCOME_GUIDE
}

/**
 * Onglet 1 : Écran principal épuré inspiré de MT Capsule :
 * - Silhouette téléphone avec sélecteur d'état d'aperçu (Repos, Musique, Appel, Minuteur, AirPods, Notification).
 * - Calibrage du Cercle Rouge de la Caméra ("Placez le cercle rouge directement sous le poinçon caméra")
 *   avec choix de l'emplacement (Gauche, Centre, Droite, Indépendante).
 * - Réglage de l'opacité au repos (0% Recommandé pour écrans OLED), de l'échelle et de la position X/Y.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeDashboardScreen(
    onNavigate: (SettingsDestination) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val overlayState by CapsuleServiceLocator.stateEngine.uiState.collectAsState()
    val profile = overlayState.currentProfile
    val appearance = overlayState.appearance
    val lang = overlayState.generalSettings.languageCode

    var permissions by remember {
        mutableStateOf(CapsuleServiceLocator.checkPermissions(context))
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME || event == Lifecycle.Event.ON_START) {
                permissions = CapsuleServiceLocator.checkPermissions(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Rafraîchit également lorsque l'état de l'overlay change
    androidx.compose.runtime.LaunchedEffect(overlayState) {
        permissions = CapsuleServiceLocator.checkPermissions(context)
    }

    val allCorePermissionsGranted = permissions.isAccessibilityServiceEnabled &&
        permissions.isNotificationListenerEnabled

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Scène d'aperçu en direct inspirée de MT Capsule
        ElevatedCard(
            shape = RoundedCornerShape(28.dp),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF1C1C1E),
                                Color(0xFF121214)
                            )
                        )
                    )
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Silhouette supérieure du smartphone
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 14.dp, bottomEnd = 14.dp))
                        .background(Color(0xFF28282C))
                        .border(
                            width = 1.5.dp,
                            color = Color.White.copy(alpha = 0.09f),
                            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 14.dp, bottomEnd = 14.dp)
                        )
                        .padding(top = 12.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    CapsuleOverlayRoot(
                        state = overlayState,
                        onExecuteAction = { action ->
                            CapsuleActionExecutor.executeAction(
                                context,
                                action,
                                CapsuleAccessibilityService.instance
                            )
                        },
                        isPreviewMode = true
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (overlayState.isMasterEnabled) {
                                tr(lang, "Dynamic Capsule active", "Dynamic Capsule Active")
                            } else {
                                tr(lang, "Dynamic Capsule désactivée", "Dynamic Capsule Disabled")
                            },
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${profile.resolutionWidth}×${profile.resolutionHeight} • ${profile.orientation.label(lang)}",
                            color = Color(0xFF8E8E93),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Switch(
                        checked = overlayState.isMasterEnabled,
                        onCheckedChange = { enabled ->
                            CapsuleServiceLocator.preferencesRepository.setMasterEnabled(enabled)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Sélecteur d'état d'aperçu horizontal ("Preview state")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isIdle = overlayState.activeDynamicCards.isEmpty() &&
                        overlayState.currentMiniEvent == null &&
                        overlayState.currentNotification == null
                    PreviewPillChip(
                        label = tr(lang, "Repos", "Idle"),
                        selected = isIdle && !appearance.showCameraSafeZoneGuide,
                        onClick = {
                            CapsuleServiceLocator.stateEngine.clearAllDynamicCards()
                            CapsuleServiceLocator.stateEngine.dismissCurrentPopupOrCollapse()
                            CapsuleServiceLocator.preferencesRepository.updateAppearanceConfig {
                                it.copy(showCameraSafeZoneGuide = false)
                            }
                        }
                    )
                    PreviewPillChip(
                        label = tr(lang, "Cercle Caméra", "Camera Circle"),
                        selected = appearance.showCameraSafeZoneGuide,
                        onClick = {
                            CapsuleServiceLocator.preferencesRepository.updateAppearanceConfig {
                                it.copy(showCameraSafeZoneGuide = !it.showCameraSafeZoneGuide)
                            }
                        }
                    )
                    val isMedia = overlayState.activeDynamicCards.any { it.cardType == DynamicCardType.MEDIA_PLAYER }
                    PreviewPillChip(
                        label = tr(lang, "Musique", "Music"),
                        selected = isMedia,
                        onClick = {
                            CapsuleServiceLocator.stateEngine.toggleSimulatedDynamicCard(DynamicCardType.MEDIA_PLAYER)
                        }
                    )
                    val isCall = overlayState.activeDynamicCards.any { it.cardType == DynamicCardType.ACTIVE_CALL }
                    PreviewPillChip(
                        label = tr(lang, "Appel", "Call"),
                        selected = isCall,
                        onClick = {
                            CapsuleServiceLocator.stateEngine.toggleSimulatedDynamicCard(DynamicCardType.ACTIVE_CALL)
                        }
                    )
                    val isTimer = overlayState.activeDynamicCards.any { it.cardType == DynamicCardType.TIMER_STOPWATCH }
                    PreviewPillChip(
                        label = tr(lang, "Minuteur", "Timer"),
                        selected = isTimer,
                        onClick = {
                            CapsuleServiceLocator.stateEngine.toggleSimulatedDynamicCard(DynamicCardType.TIMER_STOPWATCH)
                        }
                    )
                    PreviewPillChip(
                        label = "AirPods",
                        selected = false,
                        onClick = {
                            CapsuleServiceLocator.stateEngine.simulateMiniEvent(MiniCapsuleEventType.AIRPODS_BATTERY_UPDATE)
                        }
                    )
                    PreviewPillChip(
                        label = tr(lang, "Message", "Message"),
                        selected = overlayState.currentNotification != null,
                        onClick = {
                            CapsuleServiceLocator.stateEngine.simulateIncomingAppNotification()
                        }
                    )
                }
            }
        }

        // 2. Bannière d'état des services système
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = if (allCorePermissionsGranted) {
                MaterialTheme.colorScheme.surfaceContainerLow
            } else {
                Color(0xFFFF9F0A).copy(alpha = 0.14f)
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigate(SettingsDestination.ONBOARDING_PERMISSIONS) }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = if (allCorePermissionsGranted) Icons.Default.CheckCircle else Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = if (allCorePermissionsGranted) Color(0xFF30D158) else Color(0xFFFF9F0A),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (allCorePermissionsGranted) {
                                tr(lang, "Services système opérationnels", "System Services Ready")
                            } else {
                                tr(lang, "Autorisations système requises", "System Permissions Required")
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (allCorePermissionsGranted) {
                                tr(lang, "Accessibilité, notifications et Shizuku", "Accessibility, notifications & Shizuku")
                            } else {
                                tr(
                                    lang,
                                    "Touchez pour activer l'Accessibilité et les Notifications",
                                    "Tap to enable Accessibility and Notification Access"
                                )
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Bannière d'accès au Guide d'aide & tutoriel
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigate(SettingsDestination.WELCOME_GUIDE) }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = tr(lang, "Guide de démarrage & Aide", "Welcome Guide & Help"),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = tr(
                                lang,
                                "Découvrir le calibrage caméra, les gestes et les cartes",
                                "Learn camera calibration, gestures & live cards"
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 3. Calibrage de la Zone Caméra (Cercle Rouge & Emplacement anti-masquage)
        ElevatedCard(
            shape = RoundedCornerShape(22.dp),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = tr(lang, "Zone Caméra", "Camera Zone"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }

                    FilterChip(
                        selected = appearance.showCameraSafeZoneGuide,
                        onClick = {
                            CapsuleServiceLocator.preferencesRepository.updateAppearanceConfig {
                                it.copy(showCameraSafeZoneGuide = !it.showCameraSafeZoneGuide)
                            }
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        label = { Text(tr(lang, "Cercle rouge", "Red circle"), fontSize = 12.sp, maxLines = 1) }
                    )
                }

                Text(
                    text = tr(
                        lang,
                        "Activez le « Cercle rouge » et placez-le directement sous le poinçon de votre caméra frontale. Les icônes et textes s'écarteront automatiquement de cette zone sur n'importe quel téléphone.",
                        "Enable the 'Red Circle' and align it directly under your front camera punch-hole. Icons and text automatically part around this zone on any Android phone."
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val placements = listOf(
                        CameraCutoutPlacement.LEFT to CameraCutoutPlacement.LEFT.label(lang),
                        CameraCutoutPlacement.CENTER to CameraCutoutPlacement.CENTER.label(lang),
                        CameraCutoutPlacement.RIGHT to CameraCutoutPlacement.RIGHT.label(lang),
                        CameraCutoutPlacement.NONE to CameraCutoutPlacement.NONE.label(lang)
                    )
                    placements.forEachIndexed { index, (placement, shortLabel) ->
                        SegmentedButton(
                            selected = profile.cameraPlacement == placement,
                            onClick = {
                                CapsuleServiceLocator.databaseRepository.saveScreenProfile(
                                    profile.copy(cameraPlacement = placement)
                                )
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = placements.size)
                        ) {
                            Text(shortLabel, fontSize = 11.sp, maxLines = 1)
                        }
                    }
                }

                if (profile.cameraPlacement != CameraCutoutPlacement.NONE) {
                    CleanSliderRow(
                        title = tr(lang, "Largeur du poinçon caméra", "Camera cutout width"),
                        valueText = "${profile.cameraCutoutWidthDp} dp",
                        value = profile.cameraCutoutWidthDp.toFloat(),
                        range = 16f..84f,
                        onValueChange = { v ->
                            CapsuleServiceLocator.databaseRepository.saveScreenProfile(
                                profile.copy(cameraCutoutWidthDp = v.roundToInt())
                            )
                        }
                    )

                    CleanSliderRow(
                        title = tr(lang, "Marge de sécurité autour de la caméra", "Safe padding around camera"),
                        valueText = "${profile.cameraSafePaddingDp} dp",
                        value = profile.cameraSafePaddingDp.toFloat(),
                        range = 2f..24f,
                        onValueChange = { v ->
                            CapsuleServiceLocator.databaseRepository.saveScreenProfile(
                                profile.copy(cameraSafePaddingDp = v.roundToInt())
                            )
                        }
                    )
                }
            }
        }

        // 4. Profil d'écran, Position X/Y, Échelle & Opacité au repos
        ElevatedCard(
            shape = RoundedCornerShape(22.dp),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = tr(lang, "Position, Échelle & Repos", "Position, Scale & Idle"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ScreenOrientationMode.entries.forEach { mode ->
                            FilterChip(
                                selected = profile.orientation == mode,
                                onClick = {
                                    val w = if (mode == ScreenOrientationMode.PORTRAIT) 1080 else 2400
                                    val h = if (mode == ScreenOrientationMode.PORTRAIT) 2400 else 1080
                                    CapsuleServiceLocator.stateEngine.onDisplayConfigurationChanged(
                                        widthPx = w,
                                        heightPx = h,
                                        orientation = mode
                                    )
                                },
                                label = { Text(mode.label(lang), fontSize = 12.sp) }
                            )
                        }
                    }
                }

                // Opacité au repos (0% Recommandé pour les téléphones OLED comme dans MT Capsule)
                Text(
                    text = tr(
                        lang,
                        "Opacité au repos (quand aucune activité n'est en cours) :",
                        "Idle opacity (when no activity is running):"
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        0 to tr(lang, "0% (Recommandé)", "0% (Recommended)"),
                        20 to "20%",
                        40 to "40%",
                        80 to "80%",
                        100 to tr(lang, "100% (Toujours visible)", "100% (Always visible)")
                    ).forEach { (pct, label) ->
                        FilterChip(
                            selected = appearance.idleOpacityPercent == pct,
                            onClick = {
                                CapsuleServiceLocator.preferencesRepository.updateAppearanceConfig {
                                    it.copy(idleOpacityPercent = pct)
                                }
                            },
                            label = { Text(label, fontSize = 12.sp) }
                        )
                    }
                }

                HorizontalDivider()

                CleanSliderRowWithStepButtons(
                    title = tr(lang, "Position X", "X Position"),
                    value = profile.xOffsetPx,
                    unit = "px",
                    range = -240..240,
                    onValueChange = { x ->
                        CapsuleServiceLocator.databaseRepository.saveScreenProfile(profile.copy(xOffsetPx = x))
                    }
                )

                CleanSliderRowWithStepButtons(
                    title = tr(lang, "Position Y", "Y Position"),
                    value = profile.yOffsetPx,
                    unit = "px",
                    range = 0..120,
                    onValueChange = { y ->
                        CapsuleServiceLocator.databaseRepository.saveScreenProfile(profile.copy(yOffsetPx = y))
                    }
                )

                HorizontalDivider()

                Text(
                    text = tr(lang, "Échelle de la Mini-Capsule :", "Mini-Capsule Scale:"),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(100, 110, 120, 135, 150, 175).forEach { scalePct ->
                        FilterChip(
                            selected = profile.miniCapsuleScalePercent == scalePct,
                            onClick = {
                                CapsuleServiceLocator.databaseRepository.saveScreenProfile(
                                    profile.copy(miniCapsuleScalePercent = scalePct)
                                )
                            },
                            label = {
                                Text(
                                    if (scalePct == 100) tr(lang, "100% (Défaut)", "100% (Default)") else "$scalePct%",
                                    fontSize = 12.sp
                                )
                            }
                        )
                    }
                }

                CleanSliderRow(
                    title = tr(lang, "Largeur de base", "Base Width"),
                    valueText = "${profile.compactWidthDp} dp",
                    value = profile.compactWidthDp.toFloat(),
                    range = 90f..220f,
                    onValueChange = { w ->
                        CapsuleServiceLocator.databaseRepository.saveScreenProfile(
                            profile.copy(compactWidthDp = w.roundToInt())
                        )
                    }
                )

                CleanSliderRow(
                    title = tr(lang, "Hauteur de base", "Base Height"),
                    valueText = "${profile.compactHeightDp} dp",
                    value = profile.compactHeightDp.toFloat(),
                    range = 26f..54f,
                    onValueChange = { h ->
                        CapsuleServiceLocator.databaseRepository.saveScreenProfile(
                            profile.copy(compactHeightDp = h.roundToInt())
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun PreviewPillChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) Color.White else Color(0xFF2C2C2E),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            color = if (selected) Color.Black else Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
internal fun CleanSliderRow(
    title: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(
                text = valueText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range
        )
    }
}

@Composable
internal fun CleanSliderRowWithStepButtons(
    title: String,
    value: Int,
    unit: String,
    range: IntRange,
    onValueChange: (Int) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "$title : $value $unit", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier
                        .size(28.dp)
                        .clickable { onValueChange((value - 1).coerceIn(range)) }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("-", fontWeight = FontWeight.Bold)
                    }
                }
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier
                        .size(28.dp)
                        .clickable { onValueChange((value + 1).coerceIn(range)) }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("+", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.roundToInt().coerceIn(range)) },
            valueRange = range.first.toFloat()..range.last.toFloat()
        )
    }
}
