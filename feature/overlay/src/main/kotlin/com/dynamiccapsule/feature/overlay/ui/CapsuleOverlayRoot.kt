package com.dynamiccapsule.feature.overlay.ui

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dynamiccapsule.core.model.CameraCutoutPlacement
import com.dynamiccapsule.core.model.CapsuleAction
import com.dynamiccapsule.core.model.CapsuleDisplayMode
import com.dynamiccapsule.core.model.CapsuleGestureType
import com.dynamiccapsule.core.model.CapsuleOverlayUiState
import com.dynamiccapsule.core.model.ScreenProfile
import com.dynamiccapsule.core.system.CapsuleServiceLocator
import com.dynamiccapsule.core.system.physics.SpringPhysicsParameters
import kotlin.math.abs

/**
 * Rendu principal de la Dynamic Capsule :
 * - Noir profond (#000000) style Apple Dynamic Island.
 * - Opacité au repos configurable (0% par défaut = invisible quand aucune activité n'est en cours,
 *   recommandé pour les écrans OLED, tout en conservant la détection des gestes sur la zone caméra).
 * - Cercle rouge de calibrage caméra (style MT Capsule) + ligne blanche d'alignement.
 * - Balayage vers le haut (Swipe up to close) pour replier toute carte étendue ou notification.
 */
@Composable
fun CapsuleOverlayRoot(
    state: CapsuleOverlayUiState,
    onExecuteAction: (CapsuleAction) -> Unit,
    modifier: Modifier = Modifier,
    isPreviewMode: Boolean = false
) {
    if (state.displayMode == CapsuleDisplayMode.HIDDEN && !isPreviewMode) {
        return
    }

    val view = LocalView.current
    val physicsSpec = remember(state.animation) {
        SpringPhysicsParameters.calculateSpec(state.animation)
    }

    var previousMode by remember { mutableStateOf(state.displayMode) }
    var previousCardIndex by remember { mutableStateOf(state.selectedCardIndex) }

    LaunchedEffect(state.displayMode, state.selectedCardIndex) {
        val wasExpanded = previousMode == CapsuleDisplayMode.EXPANDED_CARD ||
            previousMode == CapsuleDisplayMode.ACTION_DRAWER ||
            previousMode == CapsuleDisplayMode.MINI_EVENT_POPUP
        val isNowExpanded = state.displayMode == CapsuleDisplayMode.EXPANDED_CARD ||
            state.displayMode == CapsuleDisplayMode.ACTION_DRAWER ||
            state.displayMode == CapsuleDisplayMode.MINI_EVENT_POPUP

        if (!wasExpanded && isNowExpanded && state.animation.hapticsOnOpen) {
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        } else if (wasExpanded && !isNowExpanded && state.animation.hapticsOnClose) {
            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
        } else if (previousCardIndex != state.selectedCardIndex && state.animation.hapticsOnCardChange) {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }
        previousMode = state.displayMode
        previousCardIndex = state.selectedCardIndex
    }

    val profile = state.currentProfile
    val scaleFactor = (profile.miniCapsuleScalePercent.coerceIn(85, 200) / 100f)
    val cameraGapBonusDp = if (profile.cameraPlacement != CameraCutoutPlacement.NONE) {
        (profile.cameraCutoutWidthDp / 2).coerceAtLeast(12)
    } else {
        0
    }

    val effectiveDisplayMode = if (isPreviewMode && state.displayMode == CapsuleDisplayMode.EXPANDED_CARD) {
        if (state.activeDynamicCards.size >= 2) {
            CapsuleDisplayMode.SPLIT_COMPACT_CARD
        } else {
            CapsuleDisplayMode.COMPACT_CARD
        }
    } else {
        state.displayMode
    }

    val (targetWidthDp, targetHeightDp) = when (effectiveDisplayMode) {
        CapsuleDisplayMode.HIDDEN,
        CapsuleDisplayMode.IDLE_CUTOUT ->
            (profile.compactWidthDp * scaleFactor).dp to (profile.compactHeightDp * scaleFactor).dp
        CapsuleDisplayMode.MINI_EVENT_POPUP -> {
            val isAirPods = state.currentMiniEvent?.airPodsStatus != null
            if (isAirPods) {
                326.dp to 68.dp
            } else {
                ((256 + cameraGapBonusDp) * scaleFactor).coerceAtMost(360f).dp to (42 * scaleFactor).coerceAtMost(64f).dp
            }
        }
        CapsuleDisplayMode.NOTIFICATION_POPUP -> 352.dp to 82.dp
        CapsuleDisplayMode.COMPACT_CARD ->
            ((profile.compactWidthDp + 72 + cameraGapBonusDp) * scaleFactor).coerceAtMost(356f).dp to
                ((profile.compactHeightDp + 2) * scaleFactor).coerceAtMost(58f).dp
        CapsuleDisplayMode.SPLIT_COMPACT_CARD ->
            ((profile.compactWidthDp + 58 + cameraGapBonusDp) * scaleFactor).coerceAtMost(310f).dp to
                ((profile.compactHeightDp + 2) * scaleFactor).coerceAtMost(58f).dp
        CapsuleDisplayMode.EXPANDED_CARD -> profile.expandedWidthDp.dp to profile.expandedHeightDp.dp
        CapsuleDisplayMode.ACTION_DRAWER -> profile.expandedWidthDp.dp to 232.dp
        CapsuleDisplayMode.LIVE_PROFILE_EDITOR -> profile.expandedWidthDp.dp to 216.dp
    }

    val dpAnimationSpec: FiniteAnimationSpec<Dp> = remember(physicsSpec) {
        if (physicsSpec.isSnapOnly) {
            snap()
        } else {
            spring(
                dampingRatio = physicsSpec.dampingRatio,
                stiffness = physicsSpec.stiffness
            )
        }
    }

    val animatedWidth by animateDpAsState(targetValue = targetWidthDp, animationSpec = dpAnimationSpec, label = "capsuleWidth")
    val animatedHeight by animateDpAsState(targetValue = targetHeightDp, animationSpec = dpAnimationSpec, label = "capsuleHeight")
    val animatedCornerRadius by animateDpAsState(
        targetValue = when (effectiveDisplayMode) {
            CapsuleDisplayMode.EXPANDED_CARD,
            CapsuleDisplayMode.ACTION_DRAWER,
            CapsuleDisplayMode.LIVE_PROFILE_EDITOR -> profile.cornerRadiusDp.dp
            else -> (targetHeightDp / 2).coerceAtLeast(17.dp)
        },
        animationSpec = dpAnimationSpec,
        label = "capsuleCorner"
    )

    val appearance = state.appearance
    val isIdleOnRealScreen = !isPreviewMode &&
        !appearance.showCameraSafeZoneGuide &&
        (effectiveDisplayMode == CapsuleDisplayMode.IDLE_CUTOUT || effectiveDisplayMode == CapsuleDisplayMode.HIDDEN)

    val idleVisibilityAlpha = if (isIdleOnRealScreen) {
        (appearance.idleOpacityPercent.coerceIn(0, 100) / 100f)
    } else {
        1.0f
    }

    val baseBgColor = if (state.isNightMode) {
        Color(appearance.nightBackgroundColorArgb).copy(alpha = appearance.nightBackgroundAlpha.coerceIn(0.5f, 1f))
    } else {
        Color(appearance.dayBackgroundColorArgb).copy(alpha = appearance.dayBackgroundAlpha.coerceIn(0.5f, 1f))
    }

    val subtleBorderColor = Color(appearance.borderColorArgb).copy(alpha = 0.65f)
    val capsuleShape = RoundedCornerShape(animatedCornerRadius)

    val swipeThresholdPx = remember(state.gestureConfig.swipeSensitivity) {
        (95f / state.gestureConfig.swipeSensitivity.coerceIn(0.4f, 2.0f)).coerceIn(35f, 200f)
    }
    var accumulatedDragX by remember { mutableFloatStateOf(0f) }
    var accumulatedDragY by remember { mutableFloatStateOf(0f) }

    Row(
        modifier = modifier
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .alpha(idleVisibilityAlpha),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .width(animatedWidth)
                .height(animatedHeight)
                .then(
                    if (idleVisibilityAlpha > 0.05f && appearance.shadowElevationDp > 0f) {
                        Modifier.shadow(
                            elevation = appearance.shadowElevationDp.dp,
                            shape = capsuleShape,
                            ambientColor = Color.Black,
                            spotColor = Color.Black
                        )
                    } else {
                        Modifier
                    }
                )
                .clip(capsuleShape)
                .background(baseBgColor)
                .then(
                    if (appearance.borderWidthDp > 0f && idleVisibilityAlpha > 0.05f) {
                        Modifier.border(
                            width = appearance.borderWidthDp.dp,
                            color = subtleBorderColor,
                            shape = capsuleShape
                        )
                    } else {
                        Modifier
                    }
                )
                .pointerInput(effectiveDisplayMode, state.gestureConfig) {
                    detectTapGestures(
                        onTap = {
                            when (effectiveDisplayMode) {
                                CapsuleDisplayMode.COMPACT_CARD,
                                CapsuleDisplayMode.SPLIT_COMPACT_CARD,
                                CapsuleDisplayMode.NOTIFICATION_POPUP,
                                CapsuleDisplayMode.EXPANDED_CARD -> {
                                    CapsuleServiceLocator.stateEngine.toggleExpandedCard()
                                }
                                CapsuleDisplayMode.MINI_EVENT_POPUP,
                                CapsuleDisplayMode.ACTION_DRAWER -> {
                                    CapsuleServiceLocator.stateEngine.dismissCurrentPopupOrCollapse()
                                }
                                CapsuleDisplayMode.IDLE_CUTOUT,
                                CapsuleDisplayMode.HIDDEN -> {
                                    onExecuteAction(state.gestureConfig.actionFor(CapsuleGestureType.SINGLE_TAP))
                                }
                                else -> Unit
                            }
                        },
                        onLongPress = {
                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            if (state.activeDynamicCards.isNotEmpty() && effectiveDisplayMode != CapsuleDisplayMode.EXPANDED_CARD) {
                                CapsuleServiceLocator.stateEngine.setExpandedCard(true)
                            } else {
                                onExecuteAction(state.gestureConfig.actionFor(CapsuleGestureType.LONG_PRESS))
                            }
                        },
                        onDoubleTap = {
                            onExecuteAction(state.gestureConfig.actionFor(CapsuleGestureType.DOUBLE_TAP))
                        }
                    )
                }
                .pointerInput(effectiveDisplayMode, state.gestureConfig, swipeThresholdPx) {
                    detectDragGestures(
                        onDragStart = {
                            accumulatedDragX = 0f
                            accumulatedDragY = 0f
                        },
                        onDragEnd = {
                            // Balayage vers le haut (Swipe up to close) pour fermer une carte étendue ou notification
                            if (accumulatedDragY < -50f && abs(accumulatedDragY) > abs(accumulatedDragX)) {
                                if (effectiveDisplayMode == CapsuleDisplayMode.EXPANDED_CARD ||
                                    effectiveDisplayMode == CapsuleDisplayMode.NOTIFICATION_POPUP ||
                                    effectiveDisplayMode == CapsuleDisplayMode.ACTION_DRAWER ||
                                    effectiveDisplayMode == CapsuleDisplayMode.MINI_EVENT_POPUP
                                ) {
                                    CapsuleServiceLocator.stateEngine.dismissCurrentPopupOrCollapse()
                                }
                            } else if (abs(accumulatedDragX) >= swipeThresholdPx) {
                                if (accumulatedDragX <= -swipeThresholdPx) {
                                    onExecuteAction(state.gestureConfig.actionFor(CapsuleGestureType.SWIPE_LEFT))
                                } else if (accumulatedDragX >= swipeThresholdPx) {
                                    onExecuteAction(state.gestureConfig.actionFor(CapsuleGestureType.SWIPE_RIGHT))
                                }
                            }
                            accumulatedDragX = 0f
                            accumulatedDragY = 0f
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            accumulatedDragX += dragAmount.x
                            accumulatedDragY += dragAmount.y
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = effectiveDisplayMode,
                transitionSpec = {
                    fadeIn(tween(150, easing = FastOutSlowInEasing)) togetherWith
                        fadeOut(tween(110))
                },
                label = "capsuleModeContent"
            ) { mode ->
                when (mode) {
                    CapsuleDisplayMode.HIDDEN,
                    CapsuleDisplayMode.IDLE_CUTOUT -> {
                        IdleCutoutContent(state = state)
                    }
                    CapsuleDisplayMode.MINI_EVENT_POPUP -> {
                        state.currentMiniEvent?.let { event ->
                            MiniEventPopupView(
                                event = event,
                                profile = profile,
                                lang = state.generalSettings.languageCode
                            )
                        } ?: IdleCutoutContent(state = state)
                    }
                    CapsuleDisplayMode.NOTIFICATION_POPUP -> {
                        state.currentNotification?.let { notif ->
                            NotificationCapsuleView(
                                notification = notif,
                                visualConfig = state.notificationStyle,
                                isExpanded = false,
                                lang = state.generalSettings.languageCode,
                                onExpandToggle = { CapsuleServiceLocator.stateEngine.toggleExpandedCard() },
                                onDismiss = { CapsuleServiceLocator.stateEngine.dismissCurrentPopupOrCollapse() }
                            )
                        } ?: IdleCutoutContent(state = state)
                    }
                    CapsuleDisplayMode.COMPACT_CARD,
                    CapsuleDisplayMode.SPLIT_COMPACT_CARD -> {
                        val primary = state.primaryDynamicCard
                        if (primary != null) {
                            CompactDynamicCardView(
                                card = primary,
                                profile = profile,
                                lang = state.generalSettings.languageCode
                            )
                        } else {
                            IdleCutoutContent(state = state)
                        }
                    }
                    CapsuleDisplayMode.EXPANDED_CARD -> {
                        val notif = state.currentNotification
                        if (notif != null) {
                            NotificationCapsuleView(
                                notification = notif,
                                visualConfig = state.notificationStyle,
                                isExpanded = true,
                                lang = state.generalSettings.languageCode,
                                onExpandToggle = { CapsuleServiceLocator.stateEngine.toggleExpandedCard() },
                                onDismiss = { CapsuleServiceLocator.stateEngine.dismissCurrentPopupOrCollapse() }
                            )
                        } else {
                            ExpandedDynamicCardContainer(
                                cards = state.activeDynamicCards,
                                selectedIndex = state.selectedCardIndex,
                                profile = profile,
                                lang = state.generalSettings.languageCode,
                                onSelectCard = { idx ->
                                    CapsuleServiceLocator.stateEngine.selectDynamicCardIndex(idx)
                                },
                                onCollapse = {
                                    CapsuleServiceLocator.stateEngine.setExpandedCard(false)
                                }
                            )
                        }
                    }
                    CapsuleDisplayMode.ACTION_DRAWER -> {
                        state.activeDrawerContent?.let { drawer ->
                            ActionDrawerView(
                                drawer = drawer,
                                lang = state.generalSettings.languageCode,
                                onClose = { CapsuleServiceLocator.stateEngine.closeActionDrawer() }
                            )
                        } ?: IdleCutoutContent(state = state)
                    }
                    CapsuleDisplayMode.LIVE_PROFILE_EDITOR -> {
                        LiveProfileEditorCardView(
                            profile = state.currentProfile,
                            lang = state.generalSettings.languageCode,
                            onUpdateProfile = { updated ->
                                CapsuleServiceLocator.databaseRepository.saveScreenProfile(updated)
                            },
                            onCloseEditor = {
                                CapsuleServiceLocator.stateEngine.setLiveProfileEditorOpen(false)
                            }
                        )
                    }
                }
            }

            // Cercle rouge de calibrage du poinçon caméra + ligne blanche d'alignement (inspiré de MT Capsule)
            if (appearance.showCameraSafeZoneGuide && profile.cameraPlacement != CameraCutoutPlacement.NONE) {
                CameraSafeZoneOverlayGuide(profile = profile)
            }
        }

        // Pastille secondaire détachée sobre (style Apple Dynamic Island scindé)
        val secondaryCard = state.secondaryDynamicCard
        if (effectiveDisplayMode == CapsuleDisplayMode.SPLIT_COMPACT_CARD && secondaryCard != null) {
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .size(((profile.compactHeightDp + 2) * scaleFactor).dp)
                    .shadow(4.dp, CircleShape)
                    .clip(CircleShape)
                    .background(baseBgColor)
                    .border(0.5.dp, subtleBorderColor, CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = if (appearance.touchRippleEffectEnabled) ripple(bounded = true) else null
                    ) {
                        CapsuleServiceLocator.stateEngine.cycleSelectedDynamicCard()
                    },
                contentAlignment = Alignment.Center
            ) {
                SecondaryCardPillIcon(card = secondaryCard)
            }
        }
    }
}

/**
 * Cercle rouge de calibrage du poinçon caméra et ligne blanche horizontale :
 * L'utilisateur place le cercle rouge pile sous son objectif photo jusqu'à ce qu'il soit recouvert par la caméra.
 */
@Composable
private fun CameraSafeZoneOverlayGuide(profile: ScreenProfile) {
    val alignment = when (profile.cameraPlacement) {
        CameraCutoutPlacement.LEFT -> Alignment.CenterStart
        CameraCutoutPlacement.CENTER -> Alignment.Center
        CameraCutoutPlacement.RIGHT -> Alignment.CenterEnd
        CameraCutoutPlacement.NONE -> Alignment.Center
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        contentAlignment = alignment
    ) {
        // Ligne blanche fine d'alignement horizontal
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.White.copy(alpha = 0.45f))
        )
        // Cercle rouge de la taille exacte du poinçon caméra
        Box(
            modifier = Modifier
                .width(profile.cameraCutoutWidthDp.dp)
                .height(profile.cameraCutoutHeightDp.dp.coerceAtMost(30.dp))
                .clip(RoundedCornerShape(50))
                .background(Color(0xFFFF3B30).copy(alpha = 0.85f))
                .border(1.dp, Color.White, RoundedCornerShape(50)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "CAM",
                color = Color.White,
                fontSize = 7.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
