package com.dynamiccapsule.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dynamiccapsule.core.model.MiniCapsuleEventType
import com.dynamiccapsule.core.model.MiniEventCategory
import com.dynamiccapsule.core.model.tr
import com.dynamiccapsule.core.system.CapsuleServiceLocator

@Composable
fun MiniEventsSettingsScreen(
    onBack: (() -> Unit)? = null
) {
    val enabledMap by CapsuleServiceLocator.preferencesRepository.enabledMiniEvents.collectAsState()
    val generalSettings by CapsuleServiceLocator.preferencesRepository.generalSettings.collectAsState()
    val lang = generalSettings.languageCode
    val grouped = MiniCapsuleEventType.entries.groupBy { it.category }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(MiniEventCategory.entries) { category ->
            val categoryEvents = grouped[category].orEmpty()
            val allEnabled = categoryEvents.all { enabledMap[it] != false }

            ElevatedCard(
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = category.title(lang),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        TextButton(
                            onClick = {
                                CapsuleServiceLocator.preferencesRepository.setCategoryMiniEventsEnabled(
                                    category = category,
                                    enabled = !allEnabled
                                )
                            }
                        ) {
                            Text(
                                if (allEnabled) {
                                    tr(lang, "Tout désactiver", "Disable all")
                                } else {
                                    tr(lang, "Tout activer", "Enable all")
                                }
                            )
                        }
                    }

                    categoryEvents.forEachIndexed { idx, eventType ->
                        if (idx > 0) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        }
                        val isEnabled = enabledMap[eventType] ?: true
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                Text(
                                    text = eventType.title(lang),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = eventType.description(lang),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = isEnabled,
                                onCheckedChange = { checked ->
                                    CapsuleServiceLocator.preferencesRepository.setMiniEventEnabled(
                                        type = eventType,
                                        enabled = checked
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}
