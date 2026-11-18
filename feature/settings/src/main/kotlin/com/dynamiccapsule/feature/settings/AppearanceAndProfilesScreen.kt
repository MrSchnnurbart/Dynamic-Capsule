package com.dynamiccapsule.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dynamiccapsule.core.model.AnimationOpenStyle
import com.dynamiccapsule.core.model.tr
import com.dynamiccapsule.core.system.CapsuleServiceLocator
import kotlin.math.roundToInt

/**
 * Onglet Réglages -> Style & Animations :
 * - Style d'expansion (« Comme un Fruit / Apple » vs « Comme un Robot / Android »).
 * - Préréglages de vitesse (0.5x .. 2x) et d'intensité du rebond (Aucun, Normal, Élevé, Max).
 * - Bascule Jour / Nuit (Soleil / Lune) pour éditer séparément les palettes Jour et Nuit.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppearanceAndProfilesScreen(
    onBack: (() -> Unit)? = null
) {
    val overlayState by CapsuleServiceLocator.stateEngine.uiState.collectAsState()
    val appearance = overlayState.appearance
    val animation = overlayState.animation
    val lang = overlayState.generalSettings.languageCode

    var editingNightPalette by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Style d'ouverture & Vitesse (inspiré de MT Capsule)
        ElevatedCard(
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = tr(lang, "Style d'ouverture & Physique", "Opening Style & Physics"),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AnimationOpenStyle.entries.forEach { style ->
                        FilterChip(
                            selected = animation.openStyle == style,
                            onClick = {
                                CapsuleServiceLocator.preferencesRepository.updateAnimationConfig {
                                    it.copy(openStyle = style)
                                }
                            },
                            label = { Text(style.label(lang)) }
                        )
                    }
                }

                Text(
                    tr(lang, "Vitesse d'animation :", "Animation speed:"),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        600 to tr(lang, "0.5x (Lent)", "0.5x (Slow)"),
                        460 to "0.7x",
                        340 to tr(lang, "1x (Normal)", "1x (Normal)"),
                        270 to tr(lang, "1.2x (Rapide)", "1.2x (Fast)"),
                        210 to "1.5x",
                        160 to tr(lang, "2x (Ultra)", "2x (Ultra)")
                    ).forEach { (ms, label) ->
                        FilterChip(
                            selected = kotlin.math.abs(animation.durationMs - ms) <= 25,
                            onClick = {
                                CapsuleServiceLocator.preferencesRepository.updateAnimationConfig {
                                    it.copy(durationMs = ms)
                                }
                            },
                            label = { Text(label, fontSize = 12.sp) }
                        )
                    }
                }

                Text(
                    tr(lang, "Intensité du rebond :", "Bounce intensity:"),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        0.05f to tr(lang, "Aucun rebond", "No bounce"),
                        0.42f to tr(lang, "Rebond normal (Apple)", "Normal bounce (Apple)"),
                        0.68f to tr(lang, "Rebond élevé", "High bounce"),
                        0.92f to tr(lang, "Rebond max", "Max bounce")
                    ).forEach { (bounceVal, label) ->
                        FilterChip(
                            selected = kotlin.math.abs(animation.bounceIntensity - bounceVal) <= 0.1f,
                            onClick = {
                                CapsuleServiceLocator.preferencesRepository.updateAnimationConfig {
                                    it.copy(bounceIntensity = bounceVal)
                                }
                            },
                            label = { Text(label, fontSize = 12.sp) }
                        )
                    }
                }

                ToggleRow(
                    title = tr(lang, "Réduire les animations", "Reduce animations"),
                    subtitle = tr(
                        lang,
                        "Utile pour économiser la batterie ou sur appareils moins puissants",
                        "Useful to save battery or improve speed on older devices"
                    ),
                    checked = animation.reduceAnimations,
                    onCheckedChange = { reduce ->
                        CapsuleServiceLocator.preferencesRepository.updateAnimationConfig {
                            it.copy(reduceAnimations = reduce)
                        }
                    }
                )

                HorizontalDivider()

                ToggleRow(
                    title = tr(lang, "Retours haptiques de la capsule", "Capsule haptic feedback"),
                    subtitle = tr(
                        lang,
                        "Vibration subtile à l'ouverture et au changement de carte",
                        "Subtle vibration when opening or switching cards"
                    ),
                    checked = animation.hapticsOnOpen,
                    onCheckedChange = { enabled ->
                        CapsuleServiceLocator.preferencesRepository.updateAnimationConfig {
                            it.copy(hapticsOnOpen = enabled, hapticsOnCardChange = enabled)
                        }
                    }
                )
            }
        }

        // 2. Fond de la Capsule avec bascule Soleil / Lune (Jour / Nuit)
        ElevatedCard(
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tr(lang, "Fond de la Capsule (Jour / Nuit)", "Capsule Background (Day / Night)"),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = !editingNightPalette,
                            onClick = { editingNightPalette = false },
                            leadingIcon = { Icon(Icons.Default.LightMode, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            label = { Text(tr(lang, "Jour", "Day"), fontSize = 12.sp) }
                        )
                        FilterChip(
                            selected = editingNightPalette,
                            onClick = { editingNightPalette = true },
                            leadingIcon = { Icon(Icons.Default.DarkMode, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            label = { Text(tr(lang, "Nuit", "Night"), fontSize = 12.sp) }
                        )
                    }
                }

                Text(
                    text = if (editingNightPalette) {
                        tr(
                            lang,
                            "Vous modifiez la palette du thème Nuit (#000000 Noir Apple par défaut).",
                            "Editing Night theme palette (#000000 Apple Black by default)."
                        )
                    } else {
                        tr(
                            lang,
                            "Vous modifiez la palette du thème Jour.",
                            "Editing Day theme palette."
                        )
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val selectedColor = if (editingNightPalette) {
                    appearance.nightBackgroundColorArgb
                } else {
                    appearance.dayBackgroundColorArgb
                }
                ColorPaletteRow(
                    colors = listOf(0xFF000000L, 0xFF0D0D0FL, 0xFF161618L, 0xFF1C1C1EL, 0xFF242428L),
                    selectedArgb = selectedColor,
                    onSelect = { color ->
                        CapsuleServiceLocator.preferencesRepository.updateAppearanceConfig {
                            if (editingNightPalette) {
                                it.copy(nightBackgroundColorArgb = color)
                            } else {
                                it.copy(dayBackgroundColorArgb = color)
                            }
                        }
                    }
                )

                val currentAlpha = if (editingNightPalette) appearance.nightBackgroundAlpha else appearance.dayBackgroundAlpha
                CleanSliderRow(
                    title = "${tr(lang, "Opacité du fond", "Background opacity")} (${if (editingNightPalette) tr(lang, "Nuit", "Night") else tr(lang, "Jour", "Day")})",
                    valueText = "${(currentAlpha * 100).roundToInt()} %",
                    value = currentAlpha,
                    range = 0.65f..1.0f,
                    onValueChange = { alpha ->
                        CapsuleServiceLocator.preferencesRepository.updateAppearanceConfig {
                            if (editingNightPalette) {
                                it.copy(nightBackgroundAlpha = alpha)
                            } else {
                                it.copy(dayBackgroundAlpha = alpha)
                            }
                        }
                    }
                )

                CleanSliderRow(
                    title = tr(lang, "Finesse du contour", "Border thickness"),
                    valueText = "${"%.1f".format(appearance.borderWidthDp)} dp",
                    value = appearance.borderWidthDp,
                    range = 0f..2f,
                    onValueChange = { width ->
                        CapsuleServiceLocator.preferencesRepository.updateAppearanceConfig {
                            it.copy(borderWidthDp = width)
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun ColorPaletteRow(
    colors: List<Long>,
    selectedArgb: Long,
    onSelect: (Long) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        colors.forEach { argb ->
            val isSelected = argb == selectedArgb
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(argb))
                    .border(
                        width = if (isSelected) 2.5.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.4f),
                        shape = CircleShape
                    )
                    .clickable { onSelect(argb) }
            )
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
