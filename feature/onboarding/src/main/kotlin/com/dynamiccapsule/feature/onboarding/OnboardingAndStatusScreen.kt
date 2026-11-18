package com.dynamiccapsule.feature.onboarding

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.dynamiccapsule.core.model.tr
import com.dynamiccapsule.core.system.CapsuleServiceLocator
import com.dynamiccapsule.core.system.shizuku.ShizukuStatusBarManager

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun OnboardingAndStatusScreen(
    onBack: (() -> Unit)? = null,
    onContinueToDashboard: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var permissionSnapshot by remember {
        mutableStateOf(CapsuleServiceLocator.checkPermissions(context))
    }
    val generalSettings by CapsuleServiceLocator.preferencesRepository.generalSettings.collectAsState()
    val lang = generalSettings.languageCode

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionSnapshot = CapsuleServiceLocator.checkPermissions(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            if (onBack != null) {
                TopAppBar(
                    title = {
                        Text(
                            tr(lang, "Permissions & État Système", "System Permissions & Status"),
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = tr(lang, "Retour", "Back"))
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            permissionSnapshot = CapsuleServiceLocator.checkPermissions(context)
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = tr(lang, "Actualiser", "Refresh"))
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = tr(
                    lang,
                    "Configurez les accès système nécessaires pour afficher la Dynamic Capsule par-dessus toutes les applications et lire vos activités en direct.",
                    "Configure the required system permissions to render the Dynamic Capsule above all apps and track your live activities."
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // 1. Service d'Accessibilité (TYPE_ACCESSIBILITY_OVERLAY)
            PermissionGuideCard(
                title = tr(lang, "1. Service d'Accessibilité (Overlay)", "1. Accessibility Service (Overlay)"),
                description = tr(
                    lang,
                    "Indispensable pour dessiner la capsule flottante (TYPE_ACCESSIBILITY_OVERLAY) par-dessus toutes les applications et la barre d'état à 60/120 fps.",
                    "Required to draw the floating capsule (TYPE_ACCESSIBILITY_OVERLAY) on top of all apps and the status bar at 60/120 fps."
                ),
                icon = Icons.Default.AccessibilityNew,
                isGranted = permissionSnapshot.isAccessibilityServiceEnabled,
                statusText = if (permissionSnapshot.isAccessibilityServiceEnabled) {
                    tr(lang, "État : Activé", "Status: Enabled")
                } else {
                    tr(lang, "État : Action requise", "Status: Action required")
                },
                actionLabel = if (permissionSnapshot.isAccessibilityServiceEnabled) {
                    tr(lang, "Ouvrir l'Accessibilité", "Open Accessibility")
                } else {
                    tr(lang, "Activer le Service", "Enable Service")
                },
                onActionClick = {
                    runCatching {
                        context.startActivity(
                            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                        )
                    }
                }
            )

            // 2. Accès aux Notifications & Sessions Média
            PermissionGuideCard(
                title = tr(lang, "2. Accès aux Notifications & Médias", "2. Notifications & Media Access"),
                description = tr(
                    lang,
                    "Permet de lire les sessions média actives (MediaSessionManager), les barres de progression (Maps, Play Store, Telegram), les minuteurs et les notifications d'apps choisies.",
                    "Allows reading active media sessions (MediaSessionManager), progress bars (Maps, Play Store, Telegram), timers, and selected app notifications."
                ),
                icon = Icons.Default.NotificationsActive,
                isGranted = permissionSnapshot.isNotificationListenerEnabled,
                statusText = if (permissionSnapshot.isNotificationListenerEnabled) {
                    tr(lang, "État : Activé", "Status: Enabled")
                } else {
                    tr(lang, "État : Action requise", "Status: Action required")
                },
                actionLabel = if (permissionSnapshot.isNotificationListenerEnabled) {
                    tr(lang, "Gérer l'accès", "Manage Access")
                } else {
                    tr(lang, "Autoriser les Notifications", "Allow Notifications")
                },
                onActionClick = {
                    runCatching {
                        context.startActivity(
                            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                        )
                    }
                }
            )

            // 3. Exemption d'optimisation batterie
            PermissionGuideCard(
                title = tr(lang, "3. Exemption d'Optimisation Batterie", "3. Battery Optimization Exemption"),
                description = tr(
                    lang,
                    "Empêche le système Android de suspendre le moteur d'overlay en arrière-plan afin d'assurer une réactivité instantanée.",
                    "Prevents Android from suspending the overlay engine in the background to ensure instant responsiveness."
                ),
                icon = Icons.Default.BatteryChargingFull,
                isGranted = permissionSnapshot.isBatteryOptimizationIgnored,
                statusText = if (permissionSnapshot.isBatteryOptimizationIgnored) {
                    tr(lang, "État : Activé", "Status: Enabled")
                } else {
                    tr(lang, "État : Action requise", "Status: Action required")
                },
                actionLabel = if (permissionSnapshot.isBatteryOptimizationIgnored) {
                    tr(lang, "Déjà exempté", "Already Exempted")
                } else {
                    tr(lang, "Désactiver l'optimisation", "Disable Optimization")
                },
                onActionClick = {
                    runCatching {
                        val intent = Intent(
                            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                            Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                    }.onFailure {
                        runCatching {
                            context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                        }
                    }
                }
            )

            // 4. Intégration optionnelle Shizuku
            ElevatedCard(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = tr(lang, "4. Intégration Shizuku (Optionnel)", "4. Shizuku Integration (Optional)"),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = permissionSnapshot.shizukuStatus.label(lang),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Switch(
                            checked = generalSettings.shizukuEnabled,
                            onCheckedChange = { enabled ->
                                CapsuleServiceLocator.preferencesRepository.updateGeneralSettings {
                                    it.copy(shizukuEnabled = enabled)
                                }
                                CapsuleServiceLocator.shizukuStatusBarManager?.applyHiddenStatusBarIcons(
                                    enabled = enabled,
                                    iconSlots = generalSettings.hiddenStatusBarIcons
                                )
                            }
                        )
                    }

                    Text(
                        text = tr(
                            lang,
                            "Masque sélectivement les icônes de la barre d'état système (icon_blacklist) pour libérer de la place autour de la capsule, avec sauvegarde et restauration automatique.",
                            "Selectively hides system status bar icons (icon_blacklist) to give the capsule more room, with automatic backup and restore."
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = tr(lang, "Icônes de la barre d'état à masquer :", "Status bar icons to hide:"),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ShizukuStatusBarManager.AVAILABLE_STATUS_BAR_SLOTS.forEach { slot ->
                            val selected = slot.key in generalSettings.hiddenStatusBarIcons
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    val updatedSet = generalSettings.hiddenStatusBarIcons.toMutableSet().apply {
                                        if (selected) remove(slot.key) else add(slot.key)
                                    }
                                    CapsuleServiceLocator.preferencesRepository.updateGeneralSettings {
                                        it.copy(hiddenStatusBarIcons = updatedSet)
                                    }
                                    if (generalSettings.shizukuEnabled) {
                                        CapsuleServiceLocator.shizukuStatusBarManager?.applyHiddenStatusBarIcons(
                                            enabled = true,
                                            iconSlots = updatedSet
                                        )
                                    }
                                },
                                label = { Text(slot.label(lang)) }
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = {
                                val ok = CapsuleServiceLocator.shizukuStatusBarManager?.requestShizukuPermissionOrOpenApp() == true
                                if (!ok) {
                                    Toast.makeText(
                                        context,
                                        tr(
                                            lang,
                                            "Shizuku non détecté — utilisez la commande ADB ou installez Shizuku",
                                            "Shizuku not detected — use the ADB command or install Shizuku"
                                        ),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                permissionSnapshot = CapsuleServiceLocator.checkPermissions(context)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(tr(lang, "Autoriser Shizuku", "Authorize Shizuku"))
                        }

                        OutlinedButton(
                            onClick = {
                                val cmd = CapsuleServiceLocator.shizukuStatusBarManager?.getAdbFallbackCommand().orEmpty()
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                clipboard?.setPrimaryClip(ClipData.newPlainText("ADB Command", cmd))
                                Toast.makeText(
                                    context,
                                    tr(lang, "Commande ADB copiée", "ADB command copied to clipboard"),
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(tr(lang, "Copier ADB", "Copy ADB"))
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            CapsuleServiceLocator.shizukuStatusBarManager?.restoreOriginalStatusBarIcons()
                            CapsuleServiceLocator.preferencesRepository.updateGeneralSettings {
                                it.copy(shizukuEnabled = false)
                            }
                            Toast.makeText(
                                context,
                                tr(lang, "Icônes de la barre d'état restaurées", "Status bar icons restored"),
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(tr(lang, "Restaurer immédiatement les icônes système", "Restore system icons immediately"))
                    }
                }
            }

            if (onContinueToDashboard != null) {
                Button(
                    onClick = onContinueToDashboard,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        tr(lang, "Retour à Dynamic Capsule", "Back to Dynamic Capsule"),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionGuideCard(
    title: String,
    description: String,
    icon: ImageVector,
    isGranted: Boolean,
    statusText: String,
    actionLabel: String,
    onActionClick: () -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isGranted) {
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isGranted) Color(0xFF00C853) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Icon(
                    imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = if (isGranted) Color(0xFF00C853) else Color(0xFFFFAB00)
                )
            }

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isGranted) Color(0xFF00C853) else Color(0xFFFFAB00),
                    fontWeight = FontWeight.Bold
                )
                Button(onClick = onActionClick) {
                    Text(actionLabel)
                }
            }
        }
    }
}
