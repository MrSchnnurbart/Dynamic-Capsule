package com.dynamiccapsule.feature.overlay.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.BluetoothAudio
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dynamiccapsule.core.model.ActiveDynamicCard
import com.dynamiccapsule.core.model.AirPodsStatusPayload
import com.dynamiccapsule.core.model.CameraCutoutPlacement
import com.dynamiccapsule.core.model.CapsuleOverlayUiState
import com.dynamiccapsule.core.model.DynamicCardType
import com.dynamiccapsule.core.model.MediaCommandType
import com.dynamiccapsule.core.model.MiniCapsuleEventPayload
import com.dynamiccapsule.core.model.MiniCapsuleEventType
import com.dynamiccapsule.core.model.RingerStateMode
import com.dynamiccapsule.core.model.ScreenProfile
import com.dynamiccapsule.core.model.tr
import com.dynamiccapsule.core.system.CapsuleServiceLocator
import com.dynamiccapsule.core.system.notification.CapsuleNotificationListenerService

/**
 * Au repos : capsule noire pure et sobre style Apple Dynamic Island,
 * sans points lumineux superflus.
 */
@Composable
fun IdleCutoutContent(state: CapsuleOverlayUiState) {
    val profile = state.currentProfile
    val lensAlignment = when (profile.cameraPlacement) {
        CameraCutoutPlacement.LEFT -> Alignment.CenterStart
        CameraCutoutPlacement.CENTER -> Alignment.Center
        CameraCutoutPlacement.RIGHT -> Alignment.CenterEnd
        CameraCutoutPlacement.NONE -> Alignment.Center
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp),
        contentAlignment = lensAlignment
    ) {
        // Reflet optique ultra-discret de la lentille style iPhone 15/16 Pro
        Box(
            modifier = Modifier
                .size(11.dp)
                .clip(CircleShape)
                .background(Color(0xFF0B0C10))
        )
    }
}

/**
 * Mise en page intelligente qui évite systématiquement la zone où se situe la caméra frontale
 * (Gauche, Centre ou Droite) afin qu'aucune icône ni aucun texte ne soit caché par le poinçon.
 */
@Composable
fun CameraAwareCompactLayout(
    profile: ScreenProfile,
    leftContent: @Composable () -> Unit,
    rightContent: @Composable () -> Unit
) {
    val reservedGapDp = (profile.cameraCutoutWidthDp + profile.cameraSafePaddingDp * 2).dp

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        when (profile.cameraPlacement) {
            CameraCutoutPlacement.CENTER -> {
                // Style Apple Dynamic Island : Aile gauche | Zone Caméra Centrale | Aile droite
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    leftContent()
                }
                Spacer(modifier = Modifier.width(reservedGapDp))
                Box(
                    contentAlignment = Alignment.CenterEnd
                ) {
                    rightContent()
                }
            }
            CameraCutoutPlacement.LEFT -> {
                // Caméra à gauche : on réserve l'espace à gauche, tout le contenu s'affiche à droite
                Spacer(modifier = Modifier.width(reservedGapDp))
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    leftContent()
                    Spacer(modifier = Modifier.width(8.dp))
                    rightContent()
                }
            }
            CameraCutoutPlacement.RIGHT -> {
                // Caméra à droite : tout le contenu s'affiche à gauche, espace réservé à droite
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    leftContent()
                    Spacer(modifier = Modifier.width(8.dp))
                    rightContent()
                }
                Spacer(modifier = Modifier.width(reservedGapDp))
            }
            CameraCutoutPlacement.NONE -> {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    leftContent()
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(contentAlignment = Alignment.CenterEnd) {
                    rightContent()
                }
            }
        }
    }
}

@Composable
fun MiniEventPopupView(
    event: MiniCapsuleEventPayload,
    profile: ScreenProfile = ScreenProfile(),
    lang: String = "fr"
) {
    val accent = Color(event.accentColorArgb)
    val airPods = event.airPodsStatus
    if (airPods != null) {
        AirPodsBatteryPopupContent(airPods = airPods, accent = accent, lang = lang)
        return
    }

    CameraAwareCompactLayout(
        profile = profile,
        leftContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = iconForMiniEvent(event),
                    contentDescription = event.title,
                    tint = accent,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.width(7.dp))
                Text(
                    text = event.title,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        rightContent = {
            val rightText = event.numericValue?.let { "$it%" } ?: event.subtitle
            Text(
                text = rightText,
                color = if (event.numericValue != null) accent else Color(0xFF98989F),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    )
}

@Composable
private fun AirPodsBatteryPopupContent(airPods: AirPodsStatusPayload, accent: Color, lang: String) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1C1C1E)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Headphones,
                    contentDescription = airPods.deviceName,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = airPods.deviceName,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = tr(lang, "Connecté", "Connected"),
                    color = Color(0xFF8E8E93),
                    fontSize = 11.sp
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CircularPodGauge(label = tr(lang, "G", "L"), percent = airPods.leftPercent, color = Color(0xFF30D158))
            CircularPodGauge(label = tr(lang, "D", "R"), percent = airPods.rightPercent, color = Color(0xFF30D158))
            CircularPodGauge(label = tr(lang, "Étui", "Case"), percent = airPods.casePercent, color = Color(0xFF30D158))
        }
    }
}

@Composable
private fun CircularPodGauge(label: String, percent: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(26.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawArc(
                    color = Color(0xFF2C2C2E),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                )
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = 360f * (percent / 100f).coerceIn(0f, 1f),
                    useCenter = false,
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            Text(
                text = "$percent",
                color = Color.White,
                fontSize = 8.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Text(text = label, color = Color(0xFF8E8E93), fontSize = 8.sp)
    }
}

private fun iconForMiniEvent(event: MiniCapsuleEventPayload): ImageVector = when (event.type) {
    MiniCapsuleEventType.DEVICE_UNLOCKED -> Icons.Default.LockOpen
    MiniCapsuleEventType.CHARGING_STARTED,
    MiniCapsuleEventType.CHARGING_STOPPED -> Icons.Default.BatteryChargingFull
    MiniCapsuleEventType.BATTERY_LOW_5_PERCENT -> Icons.Default.BatteryAlert
    MiniCapsuleEventType.BATTERY_SAVER_CHANGED -> Icons.Default.BatterySaver
    MiniCapsuleEventType.WIFI_CONNECTED -> Icons.Default.Wifi
    MiniCapsuleEventType.HOTSPOT_CHANGED -> Icons.Default.WifiTethering
    MiniCapsuleEventType.VPN_CHANGED -> Icons.Default.VpnKey
    MiniCapsuleEventType.AIRPLANE_MODE_CHANGED -> Icons.Default.AirplanemodeActive
    MiniCapsuleEventType.LOCATION_CHANGED -> Icons.Default.LocationOn
    MiniCapsuleEventType.BLUETOOTH_AUDIO_CHANGED,
    MiniCapsuleEventType.AIRPODS_BATTERY_UPDATE -> Icons.Default.BluetoothAudio
    MiniCapsuleEventType.RINGER_MODE_CHANGED -> when (event.ringerMode) {
        RingerStateMode.SILENT -> Icons.Default.NotificationsOff
        RingerStateMode.VIBRATE -> Icons.Default.Vibration
        else -> Icons.Default.NotificationsActive
    }
    MiniCapsuleEventType.DND_MODE_CHANGED -> Icons.Default.DoNotDisturbOn
    MiniCapsuleEventType.ADAPTIVE_BRIGHTNESS_CHANGED -> Icons.Default.BrightnessAuto
    MiniCapsuleEventType.UI_NIGHT_MODE_CHANGED -> Icons.Default.DarkMode
    MiniCapsuleEventType.AUTO_ROTATE_CHANGED -> Icons.Default.ScreenRotation
    MiniCapsuleEventType.USB_DEVICE_CHANGED -> Icons.Default.Usb
}

@Composable
fun CompactDynamicCardView(
    card: ActiveDynamicCard,
    profile: ScreenProfile = ScreenProfile(),
    lang: String = "fr"
) {
    when (card) {
        is ActiveDynamicCard.MediaPlayerCard -> {
            CameraAwareCompactLayout(
                profile = profile,
                leftContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF2C2C2E)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        if (profile.cameraPlacement != CameraCutoutPlacement.CENTER) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = card.title,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                rightContent = {
                    AnimatedAudioWaveform(isPlaying = card.isPlaying, color = Color(0xFFFF9F0A))
                }
            )
        }

        is ActiveDynamicCard.FlashlightCard -> {
            CameraAwareCompactLayout(
                profile = profile,
                leftContent = {
                    Icon(
                        imageVector = Icons.Default.FlashlightOn,
                        contentDescription = null,
                        tint = Color(0xFFFFD60A),
                        modifier = Modifier.size(16.dp)
                    )
                },
                rightContent = {
                    Text(
                        text = "ON",
                        color = Color(0xFFFFD60A),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable {
                            CapsuleServiceLocator.flashlightController?.setFlashlightEnabled(false)
                        }
                    )
                }
            )
        }

        is ActiveDynamicCard.VolumeControlCard -> {
            CameraAwareCompactLayout(
                profile = profile,
                leftContent = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                },
                rightContent = {
                    Text(
                        text = "${(card.volumeFraction * 100).toInt()}%",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            )
        }

        is ActiveDynamicCard.LiveProgressCard -> {
            CameraAwareCompactLayout(
                profile = profile,
                leftContent = {
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = null,
                        tint = Color(0xFF0A84FF),
                        modifier = Modifier.size(16.dp)
                    )
                },
                rightContent = {
                    Text(
                        text = "${(card.progressFraction * 100).toInt()}%",
                        color = Color(0xFF0A84FF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            )
        }

        is ActiveDynamicCard.TimerStopwatchCard -> {
            val orange = Color(0xFFFF9F0A)
            CameraAwareCompactLayout(
                profile = profile,
                leftContent = {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = orange,
                        modifier = Modifier.size(16.dp)
                    )
                },
                rightContent = {
                    Text(
                        text = formatSecondsMinSec(card.remainingOrElapsedSeconds),
                        color = orange,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            )
        }

        is ActiveDynamicCard.ActiveCallCard -> {
            val green = Color(0xFF30D158)
            CameraAwareCompactLayout(
                profile = profile,
                leftContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = null,
                            tint = green,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = formatSecondsMinSec(card.elapsedSeconds),
                            color = green,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                rightContent = {
                    AnimatedAudioWaveform(isPlaying = true, color = green)
                }
            )
        }

        is ActiveDynamicCard.ChargingStatusCard -> {
            val green = Color(0xFF30D158)
            CameraAwareCompactLayout(
                profile = profile,
                leftContent = {
                    Text(
                        text = tr(lang, "En charge", "Charging"),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                },
                rightContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${card.batteryPercentage}%",
                            color = green,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.Default.BatteryChargingFull,
                            contentDescription = null,
                            tint = green,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            )
        }
    }
}

@Composable
fun SecondaryCardPillIcon(card: ActiveDynamicCard) {
    val (icon, tint) = when (card) {
        is ActiveDynamicCard.MediaPlayerCard -> Icons.Default.MusicNote to Color.White
        is ActiveDynamicCard.FlashlightCard -> Icons.Default.FlashlightOn to Color(0xFFFFD60A)
        is ActiveDynamicCard.VolumeControlCard -> Icons.AutoMirrored.Filled.VolumeUp to Color.White
        is ActiveDynamicCard.LiveProgressCard -> Icons.Default.Navigation to Color(0xFF0A84FF)
        is ActiveDynamicCard.TimerStopwatchCard -> Icons.Default.Timer to Color(0xFFFF9F0A)
        is ActiveDynamicCard.ActiveCallCard -> Icons.Default.Call to Color(0xFF30D158)
        is ActiveDynamicCard.ChargingStatusCard -> Icons.Default.BatteryChargingFull to Color(0xFF30D158)
    }
    Icon(
        imageVector = icon,
        contentDescription = card.cardType.titleFr,
        tint = tint,
        modifier = Modifier.size(16.dp)
    )
}

@Composable
private fun AnimatedAudioWaveform(isPlaying: Boolean, color: Color) {
    if (!isPlaying) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.height(14.dp)
        ) {
            repeat(4) {
                Box(
                    modifier = Modifier
                        .width(2.5.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(color.copy(alpha = 0.6f))
                )
            }
        }
        return
    }

    val transition = rememberInfiniteTransition(label = "waveform")
    val bar1 by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(420, easing = LinearEasing), RepeatMode.Reverse),
        label = "b1"
    )
    val bar2 by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(tween(340, easing = LinearEasing), RepeatMode.Reverse),
        label = "b2"
    )
    val bar3 by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(510, easing = LinearEasing), RepeatMode.Reverse),
        label = "b3"
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(14.dp)
    ) {
        listOf(bar1, bar2, bar3, bar1).forEach { fraction ->
            Box(
                modifier = Modifier
                    .width(2.5.dp)
                    .height((13 * fraction).dp.coerceAtLeast(3.dp))
                    .clip(RoundedCornerShape(50))
                    .background(color)
            )
        }
    }
}

@Composable
fun ExpandedDynamicCardContainer(
    cards: List<ActiveDynamicCard>,
    selectedIndex: Int,
    profile: ScreenProfile = ScreenProfile(),
    lang: String = "fr",
    onSelectCard: (Int) -> Unit,
    onCollapse: () -> Unit
) {
    val current = cards.getOrNull(selectedIndex.coerceIn(0, (cards.size - 1).coerceAtLeast(0)))
        ?: cards.firstOrNull()
        ?: return

    // Marge supérieure adaptée au poinçon caméra pour que le contenu étendu ne soit jamais masqué
    val topSafePadding = if (profile.cameraPlacement != CameraCutoutPlacement.NONE) {
        (profile.cameraCutoutHeightDp.coerceIn(16, 36)).dp
    } else {
        14.dp
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 18.dp, end = 18.dp, top = topSafePadding, bottom = 14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        if (cards.size > 1) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    cards.forEachIndexed { idx, card ->
                        val selected = idx == selectedIndex
                        Surface(
                            color = if (selected) Color(0xFF2C2C2E) else Color(0xFF1C1C1E),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.clickable { onSelectCard(idx) }
                        ) {
                            Text(
                                text = card.cardType.title(lang),
                                color = if (selected) Color.White else Color(0xFF8E8E93),
                                fontSize = 10.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = tr(lang, "Réduire", "Collapse"),
                    tint = Color(0xFF8E8E93),
                    modifier = Modifier
                        .size(18.dp)
                        .clickable { onCollapse() }
                )
            }
        }

        when (current) {
            is ActiveDynamicCard.MediaPlayerCard -> ExpandedMediaPlayerCardView(current, lang)
            is ActiveDynamicCard.FlashlightCard -> ExpandedFlashlightCardView(current, lang)
            is ActiveDynamicCard.VolumeControlCard -> ExpandedVolumeCardView(current)
            is ActiveDynamicCard.LiveProgressCard -> ExpandedLiveProgressCardView(current)
            is ActiveDynamicCard.TimerStopwatchCard -> ExpandedTimerStopwatchCardView(current)
            is ActiveDynamicCard.ActiveCallCard -> ExpandedActiveCallCardView(current)
            is ActiveDynamicCard.ChargingStatusCard -> ExpandedChargingCardView(current, lang)
        }
    }
}

@Composable
private fun ExpandedMediaPlayerCardView(card: ActiveDynamicCard.MediaPlayerCard, lang: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF2C2C2E)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = card.title,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = card.artist,
                        color = Color(0xFF8E8E93),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            AnimatedAudioWaveform(isPlaying = card.isPlaying, color = Color.White)
        }

        Column {
            LinearProgressIndicator(
                progress = { card.progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(50)),
                color = Color.White,
                trackColor = Color(0xFF2C2C2E)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatSecondsMinSec(card.positionMs / 1000L),
                    color = Color(0xFF8E8E93),
                    fontSize = 10.sp
                )
                Text(
                    text = formatSecondsMinSec(card.durationMs / 1000L),
                    color = Color(0xFF8E8E93),
                    fontSize = 10.sp
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    CapsuleNotificationListenerService.instance?.dispatchMediaCommand(MediaCommandType.PREVIOUS_TRACK)
                        ?: CapsuleServiceLocator.stateEngine.upsertDynamicCard(
                            card.copy(title = "One More Time", positionMs = 0L)
                        )
                }
            ) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "Précédent", tint = Color.White, modifier = Modifier.size(26.dp))
            }
            IconButton(
                onClick = {
                    CapsuleNotificationListenerService.instance?.dispatchMediaCommand(MediaCommandType.PLAY_PAUSE)
                        ?: CapsuleServiceLocator.stateEngine.upsertDynamicCard(
                            card.copy(isPlaying = !card.isPlaying)
                        )
                }
            ) {
                Icon(
                    imageVector = if (card.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Lecture/Pause",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
            IconButton(
                onClick = {
                    CapsuleNotificationListenerService.instance?.dispatchMediaCommand(MediaCommandType.NEXT_TRACK)
                        ?: CapsuleServiceLocator.stateEngine.upsertDynamicCard(
                            card.copy(title = "Digital Love", positionMs = 0L)
                        )
                }
            ) {
                Icon(Icons.Default.SkipNext, contentDescription = "Suivant", tint = Color.White, modifier = Modifier.size(26.dp))
            }
        }
    }
}

@Composable
private fun ExpandedFlashlightCardView(card: ActiveDynamicCard.FlashlightCard, lang: String) {
    val yellow = Color(0xFFFFD60A)
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1C1C1E)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.FlashlightOn, contentDescription = null, tint = yellow)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        tr(lang, "Lampe torche", "Flashlight"),
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                    Text(
                        "${tr(lang, "Intensité", "Strength")} ${card.strengthLevel}/${card.maxStrengthLevel}",
                        color = Color(0xFF8E8E93),
                        fontSize = 12.sp
                    )
                }
            }

            Surface(
                color = Color(0xFF2C2C2E),
                shape = RoundedCornerShape(50),
                modifier = Modifier.clickable {
                    CapsuleServiceLocator.flashlightController?.setFlashlightEnabled(false)
                    CapsuleServiceLocator.stateEngine.removeDynamicCard(DynamicCardType.FLASHLIGHT)
                }
            ) {
                Text(
                    text = tr(lang, "Éteindre", "Turn off"),
                    color = yellow,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                )
            }
        }

        Slider(
            value = card.strengthLevel.toFloat(),
            onValueChange = { newVal ->
                val lvl = newVal.toInt().coerceIn(1, card.maxStrengthLevel)
                CapsuleServiceLocator.flashlightController?.setFlashlightStrength(lvl)
                CapsuleServiceLocator.stateEngine.upsertDynamicCard(card.copy(strengthLevel = lvl))
            },
            valueRange = 1f..card.maxStrengthLevel.coerceAtLeast(2).toFloat(),
            steps = (card.maxStrengthLevel - 2).coerceAtLeast(0),
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = yellow,
                inactiveTrackColor = Color(0xFF2C2C2E)
            )
        )
    }
}

@Composable
private fun ExpandedVolumeCardView(card: ActiveDynamicCard.VolumeControlCard) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "${card.outputDeviceName} • ${(card.volumeFraction * 100).toInt()}%",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        }
        Slider(
            value = card.currentVolume.toFloat(),
            onValueChange = { v ->
                CapsuleServiceLocator.stateEngine.upsertDynamicCard(
                    card.copy(currentVolume = v.toInt().coerceIn(0, card.maxVolume))
                )
            },
            valueRange = 0f..card.maxVolume.coerceAtLeast(1).toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color(0xFF2C2C2E)
            )
        )
    }
}

@Composable
private fun ExpandedLiveProgressCardView(card: ActiveDynamicCard.LiveProgressCard) {
    val blue = Color(0xFF0A84FF)
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF1C1C1E)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Navigation, contentDescription = null, tint = blue)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = card.appLabel,
                    color = Color(0xFF8E8E93),
                    fontSize = 11.sp
                )
                Text(
                    text = card.title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = card.statusText,
                    color = Color(0xFF8E8E93),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = "${(card.progressFraction * 100).toInt()}%",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        LinearProgressIndicator(
            progress = { card.progressFraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(50)),
            color = blue,
            trackColor = Color(0xFF2C2C2E)
        )
    }
}

@Composable
private fun ExpandedTimerStopwatchCardView(card: ActiveDynamicCard.TimerStopwatchCard) {
    val orange = Color(0xFFFF9F0A)
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(
                shape = CircleShape,
                color = orange.copy(alpha = 0.22f),
                modifier = Modifier
                    .size(42.dp)
                    .clickable {
                        CapsuleServiceLocator.stateEngine.upsertDynamicCard(
                            card.copy(isPaused = !card.isPaused)
                        )
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (card.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = "Pause",
                        tint = orange
                    )
                }
            }
            Surface(
                shape = CircleShape,
                color = Color(0xFF2C2C2E),
                modifier = Modifier
                    .size(42.dp)
                    .clickable {
                        CapsuleServiceLocator.stateEngine.removeDynamicCard(DynamicCardType.TIMER_STOPWATCH)
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Close, contentDescription = "Arrêter", tint = Color.White)
                }
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = card.label,
                color = orange,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = formatSecondsMinSec(card.remainingOrElapsedSeconds),
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.Light
            )
        }
    }
}

@Composable
private fun ExpandedActiveCallCardView(card: ActiveDynamicCard.ActiveCallCard) {
    val green = Color(0xFF30D158)
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1C1C1E)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Call, contentDescription = null, tint = green)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = card.callerName,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Text(
                    text = formatSecondsMinSec(card.elapsedSeconds),
                    color = Color(0xFF8E8E93),
                    fontSize = 12.sp
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(
                shape = CircleShape,
                color = if (card.isSpeakerOn) Color.White else Color(0xFF2C2C2E),
                modifier = Modifier
                    .size(42.dp)
                    .clickable {
                        CapsuleServiceLocator.stateEngine.upsertDynamicCard(card.copy(isSpeakerOn = !card.isSpeakerOn))
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "HP",
                        tint = if (card.isSpeakerOn) Color.Black else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Surface(
                shape = CircleShape,
                color = if (card.isMuted) Color.White else Color(0xFF2C2C2E),
                modifier = Modifier
                    .size(42.dp)
                    .clickable {
                        CapsuleServiceLocator.stateEngine.upsertDynamicCard(card.copy(isMuted = !card.isMuted))
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.MicOff,
                        contentDescription = "Muet",
                        tint = if (card.isMuted) Color.Black else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Surface(
                shape = CircleShape,
                color = Color(0xFFFF453A),
                modifier = Modifier
                    .size(42.dp)
                    .clickable {
                        CapsuleServiceLocator.stateEngine.removeDynamicCard(DynamicCardType.ACTIVE_CALL)
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.CallEnd, contentDescription = "Fin", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun ExpandedChargingCardView(card: ActiveDynamicCard.ChargingStatusCard, lang: String) {
    val green = Color(0xFF30D158)
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = card.chargingTypeLabel,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${tr(lang, "Pleine dans", "Full in")} ~${card.estimatedMinutesRemaining} min",
                    color = Color(0xFF8E8E93),
                    fontSize = 12.sp
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${card.batteryPercentage}%",
                    color = green,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.Default.BatteryChargingFull, contentDescription = null, tint = green)
            }
        }

        LinearProgressIndicator(
            progress = { (card.batteryPercentage / 100f).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(50)),
            color = green,
            trackColor = Color(0xFF2C2C2E)
        )
    }
}

private fun formatSecondsMinSec(totalSeconds: Long): String {
    val mins = (totalSeconds / 60L).coerceAtLeast(0L)
    val secs = (totalSeconds % 60L).coerceAtLeast(0L)
    return "%02d:%02d".format(mins, secs)
}
