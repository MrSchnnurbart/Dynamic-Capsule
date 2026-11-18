package com.dynamiccapsule.app

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Crop169
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dynamiccapsule.app.theme.DynamicCapsuleTheme
import com.dynamiccapsule.core.model.AppProjectInfo
import com.dynamiccapsule.core.model.AppUpdateStatus
import com.dynamiccapsule.core.model.ReleaseNotesCatalog
import com.dynamiccapsule.core.model.tr
import com.dynamiccapsule.core.system.CapsuleServiceLocator
import com.dynamiccapsule.feature.onboarding.FirstLaunchWelcomeGuideScreen
import com.dynamiccapsule.feature.onboarding.OnboardingAndStatusScreen
import com.dynamiccapsule.feature.settings.AppGeneralSettingsScreen
import com.dynamiccapsule.feature.settings.AppNotificationsSettingsScreen
import com.dynamiccapsule.feature.settings.AppearanceAndProfilesScreen
import com.dynamiccapsule.feature.settings.DynamicCardsSettingsScreen
import com.dynamiccapsule.feature.settings.GesturesAndActionsScreen
import com.dynamiccapsule.feature.settings.HomeDashboardScreen
import com.dynamiccapsule.feature.settings.MiniEventsSettingsScreen
import com.dynamiccapsule.feature.settings.SettingsDestination
import com.dynamiccapsule.feature.settings.SimulationCenterScreen

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CapsuleServiceLocator.initialize(applicationContext)
        enableEdgeToEdge()

        setContent {
            val prefsLoaded by CapsuleServiceLocator.preferencesRepository.isLoaded.collectAsState()
            val generalSettings by CapsuleServiceLocator.preferencesRepository.generalSettings.collectAsState()
            val updateStatus by CapsuleServiceLocator.updateManager.updateStatus.collectAsState()

            LaunchedEffect(prefsLoaded, generalSettings.autoCheckUpdatesOnLaunch) {
                if (prefsLoaded && generalSettings.autoCheckUpdatesOnLaunch) {
                    CapsuleServiceLocator.updateManager.checkForUpdates(AppProjectInfo.VERSION_NAME)
                }
            }

            DynamicCapsuleTheme(settings = generalSettings) {
                if (!prefsLoaded) {
                    Surface(modifier = Modifier.fillMaxSize()) {}
                } else {
                    DynamicCapsuleMainShell(
                        lang = generalSettings.languageCode,
                        hasCompletedWelcomeGuide = generalSettings.hasCompletedWelcomeGuide,
                        lastSeenChangelogVersionCode = generalSettings.lastSeenChangelogVersionCode,
                        updateStatus = updateStatus
                    )
                }
            }
        }
    }
}

private enum class MainBottomTab(
    val labelFr: String,
    val labelEn: String,
    val icon: ImageVector
) {
    CAPSULE("Capsule", "Capsule", Icons.Default.Crop169),
    ACTIVITIES("Activités", "Activities", Icons.Default.Layers),
    INTERACTIONS("Gestes & Apps", "Gestures & Apps", Icons.Default.TouchApp),
    SETTINGS("Réglages", "Settings", Icons.Default.Settings);

    fun label(lang: String): String = tr(lang, labelFr, labelEn)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DynamicCapsuleMainShell(
    lang: String,
    hasCompletedWelcomeGuide: Boolean,
    lastSeenChangelogVersionCode: Int,
    updateStatus: AppUpdateStatus
) {
    val context = LocalContext.current
    var selectedTab by rememberSaveable { mutableStateOf(MainBottomTab.CAPSULE) }
    var subDestination by rememberSaveable { mutableStateOf<SettingsDestination?>(null) }

    var activitiesSubTab by rememberSaveable { mutableIntStateOf(0) } // 0 = Cartes en direct, 1 = Alertes système
    var interactionsSubTab by rememberSaveable { mutableIntStateOf(0) } // 0 = Gestes, 1 = Notifications d'apps
    var settingsSubTab by rememberSaveable { mutableIntStateOf(0) } // 0 = Style & Animations, 1 = Permissions, 2 = App
    var dismissedUpdatePromptVersion by remember { mutableStateOf<String?>(null) }

    if (!hasCompletedWelcomeGuide || subDestination == SettingsDestination.WELCOME_GUIDE) {
        FirstLaunchWelcomeGuideScreen(
            onFinishGuide = {
                if (!hasCompletedWelcomeGuide) {
                    CapsuleServiceLocator.preferencesRepository.updateGeneralSettings {
                        it.copy(
                            hasCompletedWelcomeGuide = true,
                            lastSeenChangelogVersionCode = AppProjectInfo.VERSION_CODE
                        )
                    }
                }
                if (subDestination == SettingsDestination.WELCOME_GUIDE) {
                    subDestination = null
                }
            }
        )
        return
    }

    BackHandler(enabled = subDestination != null || selectedTab != MainBottomTab.CAPSULE) {
        if (subDestination != null) {
            subDestination = null
        } else {
            selectedTab = MainBottomTab.CAPSULE
        }
    }

    // Si une sous-page modale est ouverte (ex: Permissions depuis la bannière d'accueil)
    val activeModal = subDestination
    if (activeModal != null) {
        when (activeModal) {
            SettingsDestination.ONBOARDING_PERMISSIONS -> {
                OnboardingAndStatusScreen(
                    onBack = { subDestination = null },
                    onContinueToDashboard = { subDestination = null }
                )
            }
            SettingsDestination.SIMULATION_CENTER -> {
                SimulationCenterScreen(onBack = { subDestination = null })
            }
            else -> {
                subDestination = null
            }
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (selectedTab) {
                            MainBottomTab.CAPSULE -> "Dynamic Capsule"
                            MainBottomTab.ACTIVITIES -> tr(lang, "Activités & Popups", "Activities & Popups")
                            MainBottomTab.INTERACTIONS -> tr(lang, "Gestes & Notifications", "Gestures & Notifications")
                            MainBottomTab.SETTINGS -> tr(lang, "Style & Réglages", "Style & Settings")
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        },
        bottomBar = {
            NavigationBar {
                MainBottomTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(tab.icon, contentDescription = tab.label(lang)) },
                        label = { Text(tab.label(lang)) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Crossfade(targetState = selectedTab, label = "bottomTabCrossfade") { tab ->
                when (tab) {
                    MainBottomTab.CAPSULE -> {
                        HomeDashboardScreen(
                            onNavigate = { target -> subDestination = target }
                        )
                    }

                    MainBottomTab.ACTIVITIES -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            SingleChoiceSegmentedButtonRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                listOf(
                                    tr(lang, "Cartes en direct (7)", "Live Cards (7)"),
                                    tr(lang, "Alertes système (18)", "System Popups (18)")
                                ).forEachIndexed { idx, label ->
                                    SegmentedButton(
                                        selected = activitiesSubTab == idx,
                                        onClick = { activitiesSubTab = idx },
                                        shape = SegmentedButtonDefaults.itemShape(index = idx, count = 2)
                                    ) {
                                        Text(label)
                                    }
                                }
                            }
                            if (activitiesSubTab == 0) {
                                DynamicCardsSettingsScreen(onBack = null)
                            } else {
                                MiniEventsSettingsScreen(onBack = null)
                            }
                        }
                    }

                    MainBottomTab.INTERACTIONS -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            SingleChoiceSegmentedButtonRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                listOf(
                                    tr(lang, "Gestes tactiles", "Touch Gestures"),
                                    tr(lang, "Notifications d'apps", "App Notifications")
                                ).forEachIndexed { idx, label ->
                                    SegmentedButton(
                                        selected = interactionsSubTab == idx,
                                        onClick = { interactionsSubTab = idx },
                                        shape = SegmentedButtonDefaults.itemShape(index = idx, count = 2)
                                    ) {
                                        Text(label)
                                    }
                                }
                            }
                            if (interactionsSubTab == 0) {
                                GesturesAndActionsScreen(onBack = null)
                            } else {
                                AppNotificationsSettingsScreen(onBack = null)
                            }
                        }
                    }

                    MainBottomTab.SETTINGS -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            SingleChoiceSegmentedButtonRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                listOf(
                                    tr(lang, "Style & Anim.", "Style & Anim."),
                                    tr(lang, "Permissions", "Permissions"),
                                    tr(lang, "Application", "App & Info")
                                ).forEachIndexed { idx, label ->
                                    SegmentedButton(
                                        selected = settingsSubTab == idx,
                                        onClick = { settingsSubTab = idx },
                                        shape = SegmentedButtonDefaults.itemShape(index = idx, count = 3)
                                    ) {
                                        Text(label, style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                            when (settingsSubTab) {
                                0 -> AppearanceAndProfilesScreen(onBack = null)
                                1 -> OnboardingAndStatusScreen(onBack = null, onContinueToDashboard = null)
                                else -> AppGeneralSettingsScreen(
                                    onBack = null,
                                    onOpenWelcomeGuide = {
                                        subDestination = SettingsDestination.WELCOME_GUIDE
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Fenêtre "Nouveautés" automatique après une mise à jour de version
    if (lastSeenChangelogVersionCode < AppProjectInfo.VERSION_CODE) {
        val currentNote = ReleaseNotesCatalog.currentReleaseNote
        AlertDialog(
            onDismissRequest = {
                CapsuleServiceLocator.preferencesRepository.updateGeneralSettings {
                    it.copy(lastSeenChangelogVersionCode = AppProjectInfo.VERSION_CODE)
                }
            },
            title = {
                Text(
                    currentNote.title(lang),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    currentNote.highlights(lang).forEach { bullet ->
                        Text("• $bullet", style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        CapsuleServiceLocator.preferencesRepository.updateGeneralSettings {
                            it.copy(lastSeenChangelogVersionCode = AppProjectInfo.VERSION_CODE)
                        }
                    }
                ) {
                    Text(tr(lang, "Compris", "Got it"))
                }
            }
        )
    } else if (updateStatus is AppUpdateStatus.UpdateAvailable &&
        dismissedUpdatePromptVersion != updateStatus.latestVersion
    ) {
        AlertDialog(
            onDismissRequest = { dismissedUpdatePromptVersion = updateStatus.latestVersion },
            title = {
                Text(
                    tr(
                        lang,
                        "Mise à jour v${updateStatus.latestVersion} disponible",
                        "Update v${updateStatus.latestVersion} Available"
                    ),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(updateStatus.releaseTitle, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(updateStatus.releaseNotesBody.take(360), style = MaterialTheme.typography.bodySmall)
                }
            },
            dismissButton = {
                TextButton(onClick = { dismissedUpdatePromptVersion = updateStatus.latestVersion }) {
                    Text(tr(lang, "Plus tard", "Later"))
                }
            },
            confirmButton = {
                val apkUrl = updateStatus.apkDownloadUrl
                Button(
                    onClick = {
                        dismissedUpdatePromptVersion = updateStatus.latestVersion
                        if (apkUrl != null) {
                            CapsuleServiceLocator.updateManager.downloadAndInstallApk(
                                context = context,
                                downloadUrl = apkUrl,
                                latestVersion = updateStatus.latestVersion
                            )
                        } else {
                            CapsuleServiceLocator.updateManager.openUrlInBrowser(
                                context,
                                updateStatus.htmlReleaseUrl
                            )
                        }
                    }
                ) {
                    Text(tr(lang, "Télécharger", "Download"))
                }
            }
        )
    }
}
