package com.dynamiccapsule.feature.settings

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.dynamiccapsule.core.model.DynamicCardPriorityItem
import com.dynamiccapsule.core.model.tr
import com.dynamiccapsule.core.system.CapsuleServiceLocator
import kotlin.math.roundToInt

@Composable
fun DynamicCardsSettingsScreen(
    onBack: (() -> Unit)? = null
) {
    val view = LocalView.current
    val priorities by CapsuleServiceLocator.preferencesRepository.dynamicCardPriorities.collectAsState()
    val generalSettings by CapsuleServiceLocator.preferencesRepository.generalSettings.collectAsState()
    val lang = generalSettings.languageCode

    val orderedItems = remember(priorities) {
        priorities.sortedBy { it.priorityOrder }
    }

    var draggedIndex by remember { mutableIntStateOf(-1) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    fun swapItems(fromIndex: Int, toIndex: Int) {
        if (fromIndex !in orderedItems.indices || toIndex !in orderedItems.indices || fromIndex == toIndex) return
        val mutable = orderedItems.map { it.type }.toMutableList()
        val moved = mutable.removeAt(fromIndex)
        mutable.add(toIndex, moved)
        CapsuleServiceLocator.preferencesRepository.updateDynamicCardOrder(mutable)
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            ElevatedCard(
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = tr(lang, "Afficher sur l'écran de verrouillage", "Show on Lock Screen"),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = tr(
                                    lang,
                                    "Conserve les cartes actives sur l'écran verrouillé",
                                    "Keep live cards visible while the device is locked"
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = generalSettings.showCardsOnLockScreen,
                        onCheckedChange = { show ->
                            CapsuleServiceLocator.preferencesRepository.updateGeneralSettings {
                                it.copy(showCardsOnLockScreen = show)
                            }
                        }
                    )
                }
            }
        }

        item {
            Text(
                text = tr(
                    lang,
                    "Maintenez appuyé sur ≡ pour réorganiser l'ordre de priorité d'affichage.",
                    "Press and hold ≡ to drag and reorder display priority."
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }

        itemsIndexed(
            items = orderedItems,
            key = { _, item -> item.type.name }
        ) { index, item ->
            val isDragging = index == draggedIndex
            DynamicCardPriorityRow(
                rank = index + 1,
                item = item,
                lang = lang,
                isDragging = isDragging,
                dragOffsetY = if (isDragging) dragOffsetY else 0f,
                canMoveUp = index > 0,
                canMoveDown = index < orderedItems.lastIndex,
                onMoveUp = { swapItems(index, index - 1) },
                onMoveDown = { swapItems(index, index + 1) },
                onToggleEnabled = { enabled ->
                    CapsuleServiceLocator.preferencesRepository.setDynamicCardEnabled(item.type, enabled)
                },
                onDragStart = {
                    draggedIndex = index
                    dragOffsetY = 0f
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                },
                onDragDelta = { dy ->
                    dragOffsetY += dy
                    val rowHeightPx = 175f
                    if (dragOffsetY > rowHeightPx && draggedIndex < orderedItems.lastIndex) {
                        swapItems(draggedIndex, draggedIndex + 1)
                        draggedIndex += 1
                        dragOffsetY = 0f
                    } else if (dragOffsetY < -rowHeightPx && draggedIndex > 0) {
                        swapItems(draggedIndex, draggedIndex - 1)
                        draggedIndex -= 1
                        dragOffsetY = 0f
                    }
                },
                onDragEnd = {
                    draggedIndex = -1
                    dragOffsetY = 0f
                }
            )
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun DynamicCardPriorityRow(
    rank: Int,
    item: DynamicCardPriorityItem,
    lang: String,
    isDragging: Boolean,
    dragOffsetY: Float,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onDragStart: () -> Unit,
    onDragDelta: (Float) -> Unit,
    onDragEnd: () -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = if (isDragging) 10.dp else 1.dp
        ),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isDragging) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(if (isDragging) 10f else 0f)
            .offset { IntOffset(0, dragOffsetY.roundToInt()) }
            .pointerInput(item.type) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { onDragStart() },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragEnd() },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDragDelta(dragAmount.y)
                    }
                )
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.DragIndicator,
                    contentDescription = tr(lang, "Maintenir pour déplacer", "Hold to drag"),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "$rank. ${item.type.title(lang)}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = item.type.subtitle(lang),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onMoveUp, enabled = canMoveUp, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = tr(lang, "Monter", "Move up"), modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onMoveDown, enabled = canMoveDown, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.ArrowDownward, contentDescription = tr(lang, "Descendre", "Move down"), modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(4.dp))
                Switch(
                    checked = item.enabled,
                    onCheckedChange = onToggleEnabled
                )
            }
        }
    }
}
