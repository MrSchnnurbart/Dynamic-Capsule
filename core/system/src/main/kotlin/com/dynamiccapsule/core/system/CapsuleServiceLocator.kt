package com.dynamiccapsule.core.system

import android.content.ComponentName
import android.content.Context
import android.os.PowerManager
import android.provider.Settings
import com.dynamiccapsule.core.database.CapsuleDatabaseRepository
import com.dynamiccapsule.core.datastore.CapsulePreferencesRepository
import com.dynamiccapsule.core.system.engine.CapsuleStateEngine
import com.dynamiccapsule.core.system.hardware.FlashlightController
import com.dynamiccapsule.core.system.monitor.SystemEventMonitor
import com.dynamiccapsule.core.system.shizuku.ShizukuIntegrationStatus
import com.dynamiccapsule.core.system.shizuku.ShizukuStatusBarManager
import com.dynamiccapsule.core.system.update.GitHubUpdateManager

data class SystemPermissionSnapshot(
    val isAccessibilityServiceEnabled: Boolean = false,
    val isNotificationListenerEnabled: Boolean = false,
    val isBatteryOptimizationIgnored: Boolean = false,
    val shizukuStatus: ShizukuIntegrationStatus = ShizukuIntegrationStatus.NOT_INSTALLED
)

/**
 * Conteneur d'injection de dépendances léger et thread-safe partageant les singletons
 * entre l'application de configuration, le Service d'Accessibilité et le NotificationListenerService.
 */
object CapsuleServiceLocator {

    @Volatile
    private var isInitialized = false

    lateinit var preferencesRepository: CapsulePreferencesRepository
        private set

    lateinit var databaseRepository: CapsuleDatabaseRepository
        private set

    lateinit var stateEngine: CapsuleStateEngine
        private set

    val updateManager: GitHubUpdateManager by lazy { GitHubUpdateManager() }

    var flashlightController: FlashlightController? = null
        private set

    var shizukuStatusBarManager: ShizukuStatusBarManager? = null
        private set

    var systemEventMonitor: SystemEventMonitor? = null
        private set

    @Synchronized
    fun initialize(context: Context) {
        if (isInitialized) return
        val appContext = context.applicationContext
        preferencesRepository = CapsulePreferencesRepository(appContext)
        databaseRepository = CapsuleDatabaseRepository(appContext)
        stateEngine = CapsuleStateEngine(preferencesRepository, databaseRepository)
        flashlightController = FlashlightController(appContext, stateEngine)
        shizukuStatusBarManager = ShizukuStatusBarManager(appContext, preferencesRepository)
        systemEventMonitor = SystemEventMonitor(appContext, stateEngine)
        isInitialized = true
    }

    fun checkPermissions(
        context: Context,
        accessibilityServiceClassName: String = "com.dynamiccapsule.feature.overlay.service.CapsuleAccessibilityService"
    ): SystemPermissionSnapshot {
        initialize(context)
        val appContext = context.applicationContext

        val enabledServices = Settings.Secure.getString(
            appContext.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ).orEmpty()
        val expectedComponent = ComponentName(appContext.packageName, accessibilityServiceClassName).flattenToString()
        val expectedShort = "${appContext.packageName}/${accessibilityServiceClassName}"
        val accessibilityGranted = enabledServices.contains(expectedComponent, ignoreCase = true) ||
            enabledServices.contains(expectedShort, ignoreCase = true) ||
            enabledServices.contains(accessibilityServiceClassName, ignoreCase = true)

        val enabledNotifListeners = Settings.Secure.getString(
            appContext.contentResolver,
            "enabled_notification_listeners"
        ).orEmpty()
        val notifGranted = enabledNotifListeners.contains(appContext.packageName, ignoreCase = true)

        val powerManager = appContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val batteryOptimizationIgnored = powerManager?.isIgnoringBatteryOptimizations(appContext.packageName) == true

        val shizukuStatus = shizukuStatusBarManager?.refreshStatus()
            ?: ShizukuIntegrationStatus.NOT_INSTALLED

        return SystemPermissionSnapshot(
            isAccessibilityServiceEnabled = accessibilityGranted,
            isNotificationListenerEnabled = notifGranted,
            isBatteryOptimizationIgnored = batteryOptimizationIgnored,
            shizukuStatus = shizukuStatus
        )
    }
}
