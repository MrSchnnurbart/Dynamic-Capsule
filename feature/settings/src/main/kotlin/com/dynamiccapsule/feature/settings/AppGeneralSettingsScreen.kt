package com.dynamiccapsule.feature.settings

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import com.dynamiccapsule.core.model.AppFontFamily
import com.dynamiccapsule.core.model.AppIconVariant
import com.dynamiccapsule.core.model.AppProjectInfo
import com.dynamiccapsule.core.model.AppThemeMode
import com.dynamiccapsule.core.model.AppUpdateStatus
import com.dynamiccapsule.core.model.DynamicCardType
import com.dynamiccapsule.core.model.MiniCapsuleEventType
import com.dynamiccapsule.core.model.ReleaseNotesCatalog
import com.dynamiccapsule.core.model.tr
import com.dynamiccapsule.core.system.CapsuleServiceLocator
import com.dynamiccapsule.feature.overlay.action.CapsuleActionExecutor
import com.dynamiccapsule.feature.overlay.service.CapsuleAccessibilityService
import com.dynamiccapsule.feature.overlay.ui.CapsuleOverlayRoot

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppGeneralSettingsScreen(
    onBack: (() -> Unit)? = null,
    onOpenWelcomeGuide: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val generalSettings by CapsuleServiceLocator.preferencesRepository.generalSettings.collectAsState()
    val updateStatus by CapsuleServiceLocator.updateManager.updateStatus.collectAsState()
    val lang = generalSettings.languageCode

    var showChangelogDialog by remember { mutableStateOf(false) }
    var showLicenseDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Apparence & Langue
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
                    tr(lang, "Apparence de l'application", "App Appearance"),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = generalSettings.themeMode == mode,
                            onClick = {
                                CapsuleServiceLocator.preferencesRepository.updateGeneralSettings {
                                    it.copy(themeMode = mode)
                                }
                            },
                            label = { Text(mode.label(lang)) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        tr(lang, "Couleurs dynamiques Material You", "Material You Dynamic Colors"),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Switch(
                        checked = generalSettings.dynamicColorsEnabled,
                        onCheckedChange = { enabled ->
                            CapsuleServiceLocator.preferencesRepository.updateGeneralSettings {
                                it.copy(dynamicColorsEnabled = enabled)
                            }
                        }
                    )
                }

                HorizontalDivider()

                Text(
                    tr(lang, "Police d'écriture", "Typography"),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppFontFamily.entries.forEach { font ->
                        FilterChip(
                            selected = generalSettings.fontFamily == font,
                            onClick = {
                                CapsuleServiceLocator.preferencesRepository.updateGeneralSettings {
                                    it.copy(fontFamily = font)
                                }
                            },
                            label = { Text(font.label(lang)) }
                        )
                    }
                }

                HorizontalDivider()

                Text(
                    tr(lang, "Icône sur l'écran d'accueil", "Launcher App Icon"),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppIconVariant.entries.forEach { variant ->
                        FilterChip(
                            selected = generalSettings.iconVariant == variant,
                            onClick = {
                                CapsuleServiceLocator.preferencesRepository.updateGeneralSettings {
                                    it.copy(iconVariant = variant)
                                }
                                applyLauncherIconVariant(context, variant, lang)
                            },
                            label = { Text(variant.label(lang)) }
                        )
                    }
                }

                HorizontalDivider()

                Text(
                    tr(lang, "Langue / Language", "Language / Langue"),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("fr" to "🇫🇷 Français", "en" to "🇬🇧 English").forEach { (code, label) ->
                        FilterChip(
                            selected = generalSettings.languageCode == code,
                            onClick = {
                                CapsuleServiceLocator.preferencesRepository.updateGeneralSettings {
                                    it.copy(languageCode = code)
                                }
                                runCatching {
                                    AppCompatDelegate.setApplicationLocales(
                                        LocaleListCompat.forLanguageTags(code)
                                    )
                                }
                            },
                            label = { Text(label) }
                        )
                    }
                }
            }
        }

        // 2. Mises à jour automatiques (GitHub Releases)
        ElevatedCard(
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.SystemUpdate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            tr(lang, "Mises à jour automatiques", "Auto-Updates (GitHub)"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "v${AppProjectInfo.VERSION_NAME}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        tr(lang, "Vérifier automatiquement au lancement", "Check automatically on startup"),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    )
                    Switch(
                        checked = generalSettings.autoCheckUpdatesOnLaunch,
                        onCheckedChange = { auto ->
                            CapsuleServiceLocator.preferencesRepository.updateGeneralSettings {
                                it.copy(autoCheckUpdatesOnLaunch = auto)
                            }
                        }
                    )
                }

                when (val status = updateStatus) {
                    AppUpdateStatus.Idle -> {
                        Text(
                            text = tr(
                                lang,
                                "Version installée : v${AppProjectInfo.VERSION_NAME} (Code ${AppProjectInfo.VERSION_CODE})",
                                "Installed version: v${AppProjectInfo.VERSION_NAME} (Code ${AppProjectInfo.VERSION_CODE})"
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    AppUpdateStatus.Checking -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                tr(lang, "Vérification sur GitHub en cours...", "Checking GitHub Releases..."),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    is AppUpdateStatus.UpToDate -> {
                        Text(
                            text = tr(
                                lang,
                                "Votre application est à jour (v${status.currentVersion}).",
                                "You are running the latest version (v${status.currentVersion})."
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF30D158),
                            fontWeight = FontWeight.Medium
                        )
                    }
                    is AppUpdateStatus.UpdateAvailable -> {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = tr(
                                        lang,
                                        "Nouvelle version disponible : v${status.latestVersion}",
                                        "New update available: v${status.latestVersion}"
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = status.releaseNotesBody.take(280),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    val url = status.apkDownloadUrl
                                    if (url != null) {
                                        Button(
                                            onClick = {
                                                CapsuleServiceLocator.updateManager.downloadAndInstallApk(
                                                    context = context,
                                                    downloadUrl = url,
                                                    latestVersion = status.latestVersion
                                                )
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(tr(lang, "Installer APK", "Install APK"))
                                        }
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            CapsuleServiceLocator.updateManager.openUrlInBrowser(
                                                context,
                                                status.htmlReleaseUrl
                                            )
                                        }
                                    ) {
                                        Text("GitHub")
                                    }
                                }
                            }
                        }
                    }
                    is AppUpdateStatus.Downloading -> {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                tr(
                                    lang,
                                    "Téléchargement de v${status.latestVersion} (${status.progressPercent} %)...",
                                    "Downloading v${status.latestVersion} (${status.progressPercent}%)..."
                                ),
                                style = MaterialTheme.typography.bodySmall
                            )
                            LinearProgressIndicator(
                                progress = { status.progressPercent / 100f },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    is AppUpdateStatus.ReadyToInstall -> {
                        Text(
                            tr(
                                lang,
                                "APK v${status.latestVersion} prêt pour l'installation.",
                                "APK v${status.latestVersion} ready to install."
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF30D158),
                            fontWeight = FontWeight.Medium
                        )
                    }
                    is AppUpdateStatus.Error -> {
                        Text(
                            text = status.message(lang),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                FilledTonalButton(
                    onClick = {
                        CapsuleServiceLocator.updateManager.checkForUpdates(AppProjectInfo.VERSION_NAME)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.SystemUpdate, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(tr(lang, "Vérifier les mises à jour maintenant", "Check for Updates Now"))
                }
            }
        }

        // 3. Aide, Notes de version, Code source & Licence
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
                    tr(lang, "Système, Code Source & Licence", "System, Source Code & License"),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                if (onOpenWelcomeGuide != null) {
                    FilledTonalButton(
                        onClick = onOpenWelcomeGuide,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(tr(lang, "Guide de démarrage & Aide", "Welcome Guide & Feature Tour"))
                    }
                }

                OutlinedButton(
                    onClick = {
                        runCatching {
                            context.startActivity(
                                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AccessibilityNew, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(tr(lang, "Ouvrir les paramètres d'Accessibilité", "Open Accessibility Settings"))
                }

                OutlinedButton(
                    onClick = { showChangelogDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.History, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("${tr(lang, "Notes de version", "Release Notes")} (v${AppProjectInfo.VERSION_NAME})")
                }

                OutlinedButton(
                    onClick = {
                        CapsuleServiceLocator.updateManager.openUrlInBrowser(
                            context,
                            AppProjectInfo.GITHUB_REPO_URL
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Code, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(tr(lang, "Code source sur GitHub", "Source Code on GitHub"))
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                }

                OutlinedButton(
                    onClick = { showLicenseDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Description, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("${tr(lang, "Licence Open Source", "Open Source License")} (${AppProjectInfo.LICENSE_NAME})")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showChangelogDialog) {
        AlertDialog(
            onDismissRequest = { showChangelogDialog = false },
            title = {
                Text(
                    tr(lang, "Notes de version", "Release Notes"),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 380.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ReleaseNotesCatalog.history.forEach { note ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "${note.title(lang)} (${note.dateIso})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            note.highlights(lang).forEach { line ->
                                Text(
                                    text = "• $line",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showChangelogDialog = false }) {
                    Text(tr(lang, "Fermer", "Close"))
                }
            }
        )
    }

    if (showLicenseDialog) {
        AlertDialog(
            onDismissRequest = { showLicenseDialog = false },
            title = {
                Text(
                    "${AppProjectInfo.LICENSE_NAME} — ${AppProjectInfo.GITHUB_OWNER}",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = AppProjectInfo.GITHUB_REPO_URL,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = AppProjectInfo.MIT_LICENSE_TEXT,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        CapsuleServiceLocator.updateManager.openUrlInBrowser(
                            context,
                            AppProjectInfo.GITHUB_REPO_URL
                        )
                    }
                ) {
                    Text(tr(lang, "Ouvrir GitHub", "Open GitHub"))
                }
            },
            confirmButton = {
                TextButton(onClick = { showLicenseDialog = false }) {
                    Text(tr(lang, "Fermer", "Close"))
                }
            }
        )
    }
}

private fun applyLauncherIconVariant(context: Context, selectedVariant: AppIconVariant, lang: String) {
    runCatching {
        val pm = context.packageManager
        AppIconVariant.entries.forEach { variant ->
            val state = if (variant == selectedVariant) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            pm.setComponentEnabledSetting(
                ComponentName(context.packageName, variant.aliasClassName),
                state,
                PackageManager.DONT_KILL_APP
            )
        }
        Toast.makeText(
            context,
            tr(
                lang,
                "Icône « ${selectedVariant.label(lang)} » appliquée",
                "Icon '${selectedVariant.label(lang)}' applied"
            ),
            Toast.LENGTH_SHORT
        ).show()
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SimulationCenterScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val overlayState by CapsuleServiceLocator.stateEngine.uiState.collectAsState()
    val lang = overlayState.generalSettings.languageCode

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tr(lang, "Aperçu des Activités", "Live Activity Preview"), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = tr(lang, "Retour", "Back"))
                    }
                },
                actions = {
                    IconButton(onClick = {
                        CapsuleServiceLocator.stateEngine.clearAllDynamicCards()
                        CapsuleServiceLocator.stateEngine.dismissCurrentPopupOrCollapse()
                    }) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = tr(lang, "Réinitialiser", "Reset"))
                    }
                }
            )
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
            ElevatedCard(
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
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
            }

            ElevatedCard(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        tr(lang, "Cartes en direct", "Live Cards"),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DynamicCardType.entries.forEach { cardType ->
                            val isActive = overlayState.activeDynamicCards.any { it.cardType == cardType }
                            FilterChip(
                                selected = isActive,
                                onClick = {
                                    CapsuleServiceLocator.stateEngine.toggleSimulatedDynamicCard(cardType)
                                },
                                label = { Text(cardType.title(lang)) }
                            )
                        }
                    }
                }
            }

            ElevatedCard(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        tr(lang, "Alertes rapides", "Quick Popups"),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MiniCapsuleEventType.entries.forEach { eventType ->
                            FilledTonalButton(
                                onClick = {
                                    CapsuleServiceLocator.stateEngine.simulateMiniEvent(eventType)
                                }
                            ) {
                                Text(eventType.title(lang))
                            }
                        }
                    }

                    HorizontalDivider()

                    Button(
                        onClick = {
                            CapsuleServiceLocator.stateEngine.simulateIncomingAppNotification()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Notifications, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(tr(lang, "Aperçu Notification Message", "Preview Message Notification"))
                    }
                }
            }
        }
    }
}
