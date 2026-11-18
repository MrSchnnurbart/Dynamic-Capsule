package com.dynamiccapsule.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.dynamiccapsule.core.model.AccessibilityActionType
import com.dynamiccapsule.core.model.ActionDrawerTab
import com.dynamiccapsule.core.model.AiAssistantType
import com.dynamiccapsule.core.model.CapsuleAction
import com.dynamiccapsule.core.model.CapsuleGestureType
import com.dynamiccapsule.core.model.MediaCommandType
import com.dynamiccapsule.core.model.ScrollDirection
import com.dynamiccapsule.core.model.SystemSettingsTarget
import com.dynamiccapsule.core.model.tr
import com.dynamiccapsule.core.system.CapsuleServiceLocator
import kotlin.math.roundToInt

@Composable
fun GesturesAndActionsScreen(
    onBack: (() -> Unit)? = null
) {
    val gestureConfig by CapsuleServiceLocator.preferencesRepository.gestureConfig.collectAsState()
    val generalSettings by CapsuleServiceLocator.preferencesRepository.generalSettings.collectAsState()
    val lang = generalSettings.languageCode
    var editingGesture by remember { mutableStateOf<CapsuleGestureType?>(null) }

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
            Column(modifier = Modifier.padding(16.dp)) {
                CapsuleGestureType.entries.forEachIndexed { idx, gestureType ->
                    if (idx > 0) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                    }
                    val assignedAction = gestureConfig.actionFor(gestureType)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { editingGesture = gestureType },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = gestureType.title(lang),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = assignedAction.title(lang),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = tr(lang, "Configurer", "Configure"),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
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
                    text = tr(lang, "Balayage horizontal", "Horizontal Swipe"),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        tr(lang, "Inverser gauche et droite", "Invert left and right"),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Switch(
                        checked = gestureConfig.invertSwipeDirection,
                        onCheckedChange = { invert ->
                            CapsuleServiceLocator.preferencesRepository.updateSwipeParameters(
                                invertDirection = invert,
                                sensitivity = gestureConfig.swipeSensitivity
                            )
                        }
                    )
                }

                Text(
                    text = "${tr(lang, "Sensibilité", "Sensitivity")} : ${(gestureConfig.swipeSensitivity * 100).roundToInt()} %",
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = gestureConfig.swipeSensitivity,
                    onValueChange = { newSens ->
                        CapsuleServiceLocator.preferencesRepository.updateSwipeParameters(
                            invertDirection = gestureConfig.invertSwipeDirection,
                            sensitivity = newSens
                        )
                    },
                    valueRange = 0.4f..2.0f
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    editingGesture?.let { targetGesture ->
        ActionSelectionDialog(
            gestureType = targetGesture,
            currentAction = gestureConfig.actionFor(targetGesture),
            lang = lang,
            onDismiss = { editingGesture = null },
            onSelectAction = { newAction ->
                CapsuleServiceLocator.preferencesRepository.updateGestureAction(targetGesture, newAction)
                editingGesture = null
            }
        )
    }
}

@Composable
private fun ActionSelectionDialog(
    gestureType: CapsuleGestureType,
    currentAction: CapsuleAction,
    lang: String,
    onDismiss: () -> Unit,
    onSelectAction: (CapsuleAction) -> Unit
) {
    var customInput1 by remember { mutableStateOf("") }
    var customInput2 by remember { mutableStateOf("") }
    var isBarcode128 by remember { mutableStateOf(false) }

    val availableActions = remember(customInput1, customInput2, isBarcode128, lang) {
        listOf(
            CapsuleAction.ScrollPage(ScrollDirection.TOP),
            CapsuleAction.ScrollPage(ScrollDirection.BOTTOM),
            CapsuleAction.TakeScreenshot,
            CapsuleAction.LaunchApp(
                packageName = customInput1.ifBlank { "com.android.settings" },
                appLabel = customInput2.ifBlank { tr(lang, "Paramètres", "Settings") }
            ),
            CapsuleAction.OpenAppSearch,
            CapsuleAction.ToggleFlashlight,
            CapsuleAction.OpenShortcutsSlidersBarcodes(ActionDrawerTab.SLIDERS),
            CapsuleAction.OpenShortcutsSlidersBarcodes(ActionDrawerTab.SHORTCUTS),
            CapsuleAction.OpenShortcutsSlidersBarcodes(ActionDrawerTab.BARCODES),
            CapsuleAction.ShowWidget(label = customInput1.ifBlank { tr(lang, "Widget Horloge & Météo", "Clock & Weather Widget") }),
            CapsuleAction.MakeDirectCall(
                phoneNumber = customInput1.ifBlank { "112" },
                contactName = customInput2.ifBlank { tr(lang, "Contact Favori", "Favorite Contact") }
            ),
            CapsuleAction.SendCustomBroadcast(
                action = customInput1.ifBlank { "com.dynamiccapsule.CUSTOM_EVENT" },
                extraValue = customInput2.ifBlank { "capsule_trigger" }
            ),
            CapsuleAction.ShowQrCode(
                content = customInput1.ifBlank { "https://github.com/MrSchnnurbart/Dynamic-Capsule" },
                label = customInput2.ifBlank { tr(lang, "Mon QR / Code-barres", "My QR / Barcode") },
                isBarcode128 = isBarcode128
            ),
            CapsuleAction.MediaControl(MediaCommandType.PLAY_PAUSE),
            CapsuleAction.MediaControl(MediaCommandType.NEXT_TRACK),
            CapsuleAction.MediaControl(MediaCommandType.PREVIOUS_TRACK),
            CapsuleAction.PerformAccessibilityAction(AccessibilityActionType.NOTIFICATIONS),
            CapsuleAction.PerformAccessibilityAction(AccessibilityActionType.QUICK_SETTINGS),
            CapsuleAction.PerformAccessibilityAction(AccessibilityActionType.RECENTS),
            CapsuleAction.LaunchAiAssistant(AiAssistantType.CHATGPT_VOICE),
            CapsuleAction.LaunchAiAssistant(AiAssistantType.CHATGPT_TEXT),
            CapsuleAction.ToggleAutoBrightness,
            CapsuleAction.CycleVolumeRingerMode,
            CapsuleAction.LockScreen,
            CapsuleAction.OpenSystemSettings(SystemSettingsTarget.MAIN_SETTINGS),
            CapsuleAction.OpenSystemSettings(SystemSettingsTarget.ACCESSIBILITY),
            CapsuleAction.None
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = gestureType.title(lang),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = customInput1,
                    onValueChange = { customInput1 = it },
                    label = { Text(tr(lang, "Paramètre optionnel (URL QR, numéro, package)", "Optional parameter (QR URL, phone, package)")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = customInput2,
                    onValueChange = { customInput2 = it },
                    label = { Text(tr(lang, "Titre / Libellé optionnel", "Optional Title / Label")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !isBarcode128,
                        onClick = { isBarcode128 = false },
                        label = { Text("QR Code") }
                    )
                    FilterChip(
                        selected = isBarcode128,
                        onClick = { isBarcode128 = true },
                        label = { Text(tr(lang, "Code-barres 128", "Barcode 128")) }
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                availableActions.forEach { candidate ->
                    val isSelected = candidate::class == currentAction::class &&
                        candidate.actionTitleFr == currentAction.actionTitleFr
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectAction(candidate) }
                            .padding(vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { onSelectAction(candidate) }
                        )
                        Text(
                            text = candidate.title(lang),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(tr(lang, "Fermer", "Close"))
            }
        }
    )
}
