package com.dynamiccapsule.feature.overlay.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Screenshot
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dynamiccapsule.core.model.ActionDrawerTab
import com.dynamiccapsule.core.model.ActiveDrawerContent
import com.dynamiccapsule.core.model.CapsuleAction
import com.dynamiccapsule.core.model.CapsuleNotificationPayload
import com.dynamiccapsule.core.model.NotificationButtonStyle
import com.dynamiccapsule.core.model.NotificationColorMode
import com.dynamiccapsule.core.model.NotificationIconShape
import com.dynamiccapsule.core.model.NotificationVisualConfig
import com.dynamiccapsule.core.model.ScreenProfile
import com.dynamiccapsule.core.model.tr
import com.dynamiccapsule.core.system.CapsuleServiceLocator
import com.dynamiccapsule.core.system.barcode.BarcodeMatrixGenerator
import com.dynamiccapsule.core.system.notification.CapsuleNotificationListenerService
import com.dynamiccapsule.feature.overlay.action.CapsuleActionExecutor
import com.dynamiccapsule.feature.overlay.service.CapsuleAccessibilityService
import kotlin.math.roundToInt

/**
 * Affichage des notifications d'applications sélectionnées directement dans la capsule
 * avec personnalisation de la forme de l'icône, des couleurs, de la taille du texte et des boutons d'action.
 */
@Composable
fun NotificationCapsuleView(
    notification: CapsuleNotificationPayload,
    visualConfig: NotificationVisualConfig,
    isExpanded: Boolean,
    lang: String = "fr",
    onExpandToggle: () -> Unit,
    onDismiss: () -> Unit
) {
    val accentColor = when (visualConfig.colorMode) {
        NotificationColorMode.DEFAULT -> Color(0xFF6C63FF)
        NotificationColorMode.ADAPTIVE_APP -> Color(notification.appAccentColorArgb)
        NotificationColorMode.MATERIAL_YOU -> Color(0xFF80D8FF)
    }

    val iconShape: Shape = when (visualConfig.iconShape) {
        NotificationIconShape.CIRCLE -> CircleShape
        NotificationIconShape.SQUIRCLE -> RoundedCornerShape(36)
        NotificationIconShape.ROUNDED_SQUARE -> RoundedCornerShape(18)
        NotificationIconShape.TEARDROP -> RoundedCornerShape(
            topStartPercent = 50,
            topEndPercent = 50,
            bottomEndPercent = 50,
            bottomStartPercent = 12
        )
        NotificationIconShape.HEXAGON -> CutCornerShape(percent = 28)
    }

    val scale = visualConfig.textSizeScale.coerceIn(0.85f, 1.30f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onExpandToggle() }
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(iconShape)
                        .background(accentColor.copy(alpha = 0.24f))
                        .border(1.dp, accentColor, iconShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = notification.appName,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = notification.title,
                            color = Color.White,
                            fontSize = (13f * scale).sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "• ${notification.appName}",
                            color = accentColor,
                            fontSize = (10f * scale).sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                    Text(
                        text = notification.message,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = (12f * scale).sp,
                        maxLines = if (isExpanded) 3 else 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = tr(lang, "Fermer", "Close"),
                tint = Color.White.copy(alpha = 0.6f),
                modifier = Modifier
                    .size(18.dp)
                    .clickable { onDismiss() }
            )
        }

        if (isExpanded && notification.actions.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                notification.actions.forEach { action ->
                    NotificationStyledActionButton(
                        label = action.label,
                        style = visualConfig.actionButtonStyle,
                        accentColor = accentColor,
                        textScale = scale,
                        onClick = {
                            val listener = CapsuleNotificationListenerService.instance
                            if (listener != null) {
                                listener.triggerNotificationAction(notification.key, action.actionIndex)
                            } else {
                                onDismiss()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationStyledActionButton(
    label: String,
    style: NotificationButtonStyle,
    accentColor: Color,
    textScale: Float,
    onClick: () -> Unit
) {
    when (style) {
        NotificationButtonStyle.FILLED_PILL -> {
            Surface(
                color = accentColor,
                shape = RoundedCornerShape(50),
                modifier = Modifier.clickable { onClick() }
            ) {
                Text(
                    text = label,
                    color = Color.Black,
                    fontSize = (11f * textScale).sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }
        NotificationButtonStyle.OUTLINED -> {
            Surface(
                color = Color.Transparent,
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, accentColor),
                modifier = Modifier.clickable { onClick() }
            ) {
                Text(
                    text = label,
                    color = accentColor,
                    fontSize = (11f * textScale).sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }
        NotificationButtonStyle.CHIP -> {
            Surface(
                color = Color.White.copy(alpha = 0.14f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.clickable { onClick() }
            ) {
                Text(
                    text = label,
                    color = Color.White,
                    fontSize = (11f * textScale).sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
        NotificationButtonStyle.TEXT_ONLY -> {
            Text(
                text = label.uppercase(),
                color = accentColor,
                fontSize = (11f * textScale).sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable { onClick() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

/**
 * Tiroir interactif intégré à la capsule pour les actions de gestes :
 * Curseurs, Raccourcis, Codes-barres/QR Codes, Recherche d'application et Widgets.
 */
@Composable
fun ActionDrawerView(
    drawer: ActiveDrawerContent,
    lang: String = "fr",
    onClose: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        when (drawer) {
            is ActiveDrawerContent.SlidersShortcutsBarcodes -> {
                var selectedTab by remember(drawer.selectedTab) { mutableStateOf(drawer.selectedTab) }
                var volPct by remember { mutableStateOf(drawer.mediaVolumePercent.toFloat()) }
                var brightPct by remember { mutableStateOf(drawer.brightnessPercent.toFloat()) }
                val barcodes by CapsuleServiceLocator.databaseRepository.savedBarcodes.collectAsState()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ActionDrawerTab.entries.forEach { tab ->
                            val isSelected = tab == selectedTab
                            Surface(
                                color = if (isSelected) Color(0xFF6C63FF) else Color.White.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.clickable { selectedTab = tab }
                            ) {
                                Text(
                                    text = when (tab) {
                                        ActionDrawerTab.SLIDERS -> tr(lang, "Curseurs", "Sliders")
                                        ActionDrawerTab.SHORTCUTS -> tr(lang, "Raccourcis", "Shortcuts")
                                        ActionDrawerTab.BARCODES -> tr(lang, "Codes-barres", "Barcodes")
                                    },
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = tr(lang, "Fermer", "Close"),
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { onClose() }
                    )
                }

                when (selectedTab) {
                    ActionDrawerTab.SLIDERS -> {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Volume : ${volPct.roundToInt()}%", color = Color.White, fontSize = 12.sp)
                            }
                            Slider(
                                value = volPct,
                                onValueChange = { volPct = it },
                                valueRange = 0f..100f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF00E5FF),
                                    activeTrackColor = Color(0xFF00E5FF)
                                )
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Brightness6, contentDescription = null, tint = Color(0xFFFFD54F), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("${tr(lang, "Luminosité", "Brightness")} : ${brightPct.roundToInt()}%", color = Color.White, fontSize = 12.sp)
                            }
                            Slider(
                                value = brightPct,
                                onValueChange = { brightPct = it },
                                valueRange = 5f..100f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFFFFD54F),
                                    activeTrackColor = Color(0xFFFFD54F)
                                )
                            )
                        }
                    }

                    ActionDrawerTab.SHORTCUTS -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            QuickCapsuleTile(tr(lang, "Torche", "Torch"), Icons.Default.FlashlightOn, Color(0xFFFFD54F)) {
                                CapsuleActionExecutor.executeAction(context, CapsuleAction.ToggleFlashlight)
                            }
                            QuickCapsuleTile(tr(lang, "Capture", "Shot"), Icons.Default.Screenshot, Color(0xFF00E5FF)) {
                                CapsuleActionExecutor.executeAction(
                                    context,
                                    CapsuleAction.TakeScreenshot,
                                    CapsuleAccessibilityService.instance
                                )
                            }
                            QuickCapsuleTile(tr(lang, "Verrouiller", "Lock"), Icons.Default.Lock, Color(0xFFFF5252)) {
                                CapsuleActionExecutor.executeAction(
                                    context,
                                    CapsuleAction.LockScreen,
                                    CapsuleAccessibilityService.instance
                                )
                            }
                            QuickCapsuleTile(tr(lang, "Réglages", "Settings"), Icons.Default.Settings, Color(0xFFB388FF)) {
                                CapsuleActionExecutor.executeAction(
                                    context,
                                    CapsuleAction.OpenSystemSettings()
                                )
                            }
                        }
                    }

                    ActionDrawerTab.BARCODES -> {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(barcodes) { item ->
                                Surface(
                                    color = Color.White.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.clickable {
                                        CapsuleServiceLocator.stateEngine.openActionDrawer(
                                            ActiveDrawerContent.QrBarcodeViewer(
                                                content = item.content,
                                                label = item.label,
                                                isBarcode128 = item.isBarcode128
                                            )
                                        )
                                    }
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(Icons.Default.QrCode2, contentDescription = null, tint = Color(0xFF00E676))
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(item.label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            is ActiveDrawerContent.AppSearchDrawer -> {
                var query by remember { mutableStateOf(drawer.query) }
                val sampleApps = remember(lang) {
                    listOf(
                        tr(lang, "Paramètres", "Settings") to "com.android.settings",
                        tr(lang, "Horloge", "Clock") to "com.google.android.deskclock",
                        tr(lang, "Téléphone", "Phone") to "com.google.android.dialer",
                        "Messages" to "com.google.android.apps.messaging",
                        tr(lang, "Appareil Photo", "Camera") to "com.android.camera",
                        tr(lang, "Calculatrice", "Calculator") to "com.google.android.calculator"
                    )
                }
                val filtered = sampleApps.filter { it.first.contains(query, ignoreCase = true) }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        tr(lang, "Recherche rapide d'application", "Quick App Search"),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = tr(lang, "Fermer", "Close"),
                        tint = Color.White,
                        modifier = Modifier
                            .size(18.dp)
                            .clickable { onClose() }
                    )
                }

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(tr(lang, "Nom de l'application...", "App name..."), fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF6C63FF),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.25f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filtered) { (label, pkg) ->
                        Surface(
                            color = Color.White.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.clickable {
                                CapsuleActionExecutor.launchPackage(context, pkg)
                                onClose()
                            }
                        ) {
                            Text(
                                text = label,
                                color = Color.White,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            is ActiveDrawerContent.QrBarcodeViewer -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(drawer.label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(drawer.content, color = Color.White.copy(alpha = 0.65f), fontSize = 10.sp, maxLines = 1)
                    }
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Fermer",
                        tint = Color.White,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { onClose() }
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White)
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (drawer.isBarcode128) {
                        val bars = remember(drawer.content) {
                            BarcodeMatrixGenerator.generateCode128Bars(drawer.content)
                        }
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val barWidth = size.width / bars.size.coerceAtLeast(1)
                            bars.forEachIndexed { idx, isDark ->
                                if (isDark) {
                                    drawRect(
                                        color = Color.Black,
                                        topLeft = Offset(idx * barWidth, 0f),
                                        size = Size(barWidth.coerceAtLeast(1.5f), size.height)
                                    )
                                }
                            }
                        }
                    } else {
                        val matrix = remember(drawer.content) {
                            BarcodeMatrixGenerator.generateQrMatrix(drawer.content, 25)
                        }
                        Canvas(modifier = Modifier.size(120.dp)) {
                            val dim = matrix.size
                            val cellSize = size.width / dim
                            for (r in 0 until dim) {
                                for (c in 0 until dim) {
                                    if (matrix[r][c]) {
                                        drawRect(
                                            color = Color.Black,
                                            topLeft = Offset(c * cellSize, r * cellSize),
                                            size = Size(cellSize + 0.5f, cellSize + 0.5f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            is ActiveDrawerContent.WidgetContainer -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Widgets, contentDescription = null, tint = Color(0xFF00E5FF))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(drawer.label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = tr(lang, "Fermer", "Close"),
                        tint = Color.White,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { onClose() }
                    )
                }

                Surface(
                    color = Color.White.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(145.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                tr(lang, "Météo & Horloge Système", "System Clock & Weather"),
                                color = Color(0xFF00E5FF),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                tr(lang, "21°C • Ensoleillé", "21°C • Sunny"),
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                tr(lang, "Prochaine alarme : Demain 07:30", "Next alarm: Tomorrow 07:30"),
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 11.sp
                            )
                        }
                        Surface(
                            color = Color(0xFF6C63FF),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.clickable {
                                CapsuleActionExecutor.launchPackage(context, drawer.providerPackage)
                                onClose()
                            }
                        ) {
                            Text(
                                text = tr(lang, "Ouvrir", "Open"),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickCapsuleTile(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = color)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, color = Color.White, fontSize = 11.sp)
    }
}

/**
 * Éditeur visuel en direct affiché directement dans l'overlay pour ajuster X, Y, Largeur et Hauteur
 * au pixel près ou par glissement tactile.
 */
@Composable
fun LiveProfileEditorCardView(
    profile: ScreenProfile,
    lang: String = "fr",
    onUpdateProfile: (ScreenProfile) -> Unit,
    onCloseEditor: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(profile) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onUpdateProfile(
                        profile.copy(
                            xOffsetPx = profile.xOffsetPx + dragAmount.x.roundToInt(),
                            yOffsetPx = (profile.yOffsetPx + dragAmount.y.roundToInt()).coerceAtLeast(0)
                        )
                    )
                }
            }
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Tune, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${tr(lang, "Éditeur en direct", "Live Editor")} (${profile.orientation.label(lang)})",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
            Surface(
                color = Color(0xFF00E676),
                shape = RoundedCornerShape(50),
                modifier = Modifier.clickable { onCloseEditor() }
            ) {
                Text(
                    text = tr(lang, "Terminer", "Done"),
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }

        Text(
            text = "${tr(lang, "Glissez cette capsule ou ajustez au pixel près", "Drag this capsule or fine-tune per pixel")} • ${profile.profileKey}",
            color = Color.White.copy(alpha = 0.72f),
            fontSize = 10.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            PixelNudgeControl("Pos X (${profile.xOffsetPx}px)", onMinus = {
                onUpdateProfile(profile.copy(xOffsetPx = profile.xOffsetPx - 1))
            }, onPlus = {
                onUpdateProfile(profile.copy(xOffsetPx = profile.xOffsetPx + 1))
            })

            PixelNudgeControl("Pos Y (${profile.yOffsetPx}px)", onMinus = {
                onUpdateProfile(profile.copy(yOffsetPx = (profile.yOffsetPx - 1).coerceAtLeast(0)))
            }, onPlus = {
                onUpdateProfile(profile.copy(yOffsetPx = profile.yOffsetPx + 1))
            })
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            PixelNudgeControl("${tr(lang, "Larg.", "Width")} (${profile.compactWidthDp}dp)", onMinus = {
                onUpdateProfile(profile.copy(compactWidthDp = (profile.compactWidthDp - 2).coerceIn(80, 260)))
            }, onPlus = {
                onUpdateProfile(profile.copy(compactWidthDp = (profile.compactWidthDp + 2).coerceIn(80, 260)))
            })

            PixelNudgeControl("${tr(lang, "Haut.", "Height")} (${profile.compactHeightDp}dp)", onMinus = {
                onUpdateProfile(profile.copy(compactHeightDp = (profile.compactHeightDp - 1).coerceIn(24, 68)))
            }, onPlus = {
                onUpdateProfile(profile.copy(compactHeightDp = (profile.compactHeightDp + 1).coerceIn(24, 68)))
            })
        }
    }
}

@Composable
private fun PixelNudgeControl(
    label: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Surface(
            color = Color.White.copy(alpha = 0.16f),
            shape = CircleShape,
            modifier = Modifier
                .size(24.dp)
                .clickable { onMinus() }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("-", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
        Text(label, color = Color.White, fontSize = 11.sp)
        Surface(
            color = Color.White.copy(alpha = 0.16f),
            shape = CircleShape,
            modifier = Modifier
                .size(24.dp)
                .clickable { onPlus() }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("+", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}
