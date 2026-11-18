package com.dynamiccapsule.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dynamiccapsule.core.model.NotificationButtonStyle
import com.dynamiccapsule.core.model.NotificationColorMode
import com.dynamiccapsule.core.model.NotificationIconShape
import com.dynamiccapsule.core.model.tr
import com.dynamiccapsule.core.system.CapsuleServiceLocator
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppNotificationsSettingsScreen(
    onBack: (() -> Unit)? = null
) {
    val visualConfig by CapsuleServiceLocator.preferencesRepository.notificationVisualConfig.collectAsState()
    val allowedAppsMap by CapsuleServiceLocator.databaseRepository.allowedApps.collectAsState()
    val generalSettings by CapsuleServiceLocator.preferencesRepository.generalSettings.collectAsState()
    val lang = generalSettings.languageCode
    var searchQuery by remember { mutableStateOf("") }

    val filteredApps = remember(allowedAppsMap, searchQuery) {
        allowedAppsMap.values.filter {
            it.appLabel.contains(searchQuery, ignoreCase = true) ||
                it.packageName.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
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
                        text = tr(lang, "Notifications dans la capsule", "In-Capsule Notifications"),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Switch(
                        checked = visualConfig.enabledGlobally,
                        onCheckedChange = { enabled ->
                            CapsuleServiceLocator.preferencesRepository.updateNotificationVisualConfig {
                                it.copy(enabledGlobally = enabled)
                            }
                        }
                    )
                }

                HorizontalDivider()

                Text(tr(lang, "Forme des icônes", "Icon Shape"), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NotificationIconShape.entries.forEach { shape ->
                        FilterChip(
                            selected = visualConfig.iconShape == shape,
                            onClick = {
                                CapsuleServiceLocator.preferencesRepository.updateNotificationVisualConfig {
                                    it.copy(iconShape = shape)
                                }
                            },
                            label = { Text(shape.label(lang)) }
                        )
                    }
                }

                Text(tr(lang, "Couleurs", "Colors"), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NotificationColorMode.entries.forEach { mode ->
                        FilterChip(
                            selected = visualConfig.colorMode == mode,
                            onClick = {
                                CapsuleServiceLocator.preferencesRepository.updateNotificationVisualConfig {
                                    it.copy(colorMode = mode)
                                }
                            },
                            label = { Text(mode.label(lang)) }
                        )
                    }
                }

                Text(tr(lang, "Style des boutons d'action", "Action Button Style"), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NotificationButtonStyle.entries.forEach { btnStyle ->
                        FilterChip(
                            selected = visualConfig.actionButtonStyle == btnStyle,
                            onClick = {
                                CapsuleServiceLocator.preferencesRepository.updateNotificationVisualConfig {
                                    it.copy(actionButtonStyle = btnStyle)
                                }
                            },
                            label = { Text(btnStyle.label(lang)) }
                        )
                    }
                }

                Text(
                    text = "${tr(lang, "Taille du texte", "Text Size")} : ${(visualConfig.textSizeScale * 100).roundToInt()} %",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Slider(
                    value = visualConfig.textSizeScale,
                    onValueChange = { scale ->
                        CapsuleServiceLocator.preferencesRepository.updateNotificationVisualConfig {
                            it.copy(textSizeScale = scale)
                        }
                    },
                    valueRange = 0.85f..1.30f
                )
            }
        }

        ElevatedCard(
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = tr(lang, "Applications sélectionnées", "Selected Applications"),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(tr(lang, "Filtrer une application...", "Filter apps...")) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                filteredApps.forEachIndexed { idx, rule ->
                    if (idx > 0) {
                        HorizontalDivider()
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(rule.appLabel, fontWeight = FontWeight.Medium)
                            Text(
                                text = rule.packageName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = rule.enabled,
                            onCheckedChange = { enabled ->
                                CapsuleServiceLocator.databaseRepository.setAppNotificationAllowed(
                                    packageName = rule.packageName,
                                    appLabel = rule.appLabel,
                                    enabled = enabled,
                                    accentColorArgb = rule.customAccentColorArgb
                                )
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
