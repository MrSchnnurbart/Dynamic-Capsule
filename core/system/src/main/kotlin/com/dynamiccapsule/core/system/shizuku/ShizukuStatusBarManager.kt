package com.dynamiccapsule.core.system.shizuku

import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import com.dynamiccapsule.core.datastore.CapsulePreferencesRepository
import com.dynamiccapsule.core.model.tr
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ShizukuIntegrationStatus(val labelFr: String, val labelEn: String) {
    NOT_INSTALLED("Shizuku non installé (Fallback ADB disponible)", "Shizuku not installed (ADB fallback available)"),
    INSTALLED_NOT_RUNNING("Shizuku installé mais service arrêté", "Shizuku installed but service stopped"),
    PERMISSION_REQUIRED("Shizuku actif — Autorisation requise", "Shizuku running — Permission required"),
    READY_SHIZUKU("Shizuku actif et autorisé", "Shizuku running and authorized"),
    READY_WRITE_SECURE_SETTINGS("Autorisé via WRITE_SECURE_SETTINGS (ADB)", "Authorized via WRITE_SECURE_SETTINGS (ADB)");

    fun label(lang: String): String = tr(lang, labelFr, labelEn)
}

data class StatusBarIconSlot(
    val key: String,
    val labelFr: String,
    val labelEn: String,
    val descriptionFr: String,
    val descriptionEn: String
) {
    fun label(lang: String): String = tr(lang, labelFr, labelEn)
    fun description(lang: String): String = tr(lang, descriptionFr, descriptionEn)
}

/**
 * Gestionnaire d'intégration Shizuku + fallback WRITE_SECURE_SETTINGS permettant
 * de masquer certaines icônes de la barre d'état (`icon_blacklist`) pour laisser
 * plus d'espace autour de la Dynamic Capsule, avec restauration automatique.
 */
class ShizukuStatusBarManager(
    private val context: Context,
    private val preferencesRepository: CapsulePreferencesRepository
) {
    private val _status = MutableStateFlow(ShizukuIntegrationStatus.NOT_INSTALLED)
    val status: StateFlow<ShizukuIntegrationStatus> = _status.asStateFlow()

    fun refreshStatus(): ShizukuIntegrationStatus {
        val hasWriteSecure = context.checkSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS) ==
            PackageManager.PERMISSION_GRANTED

        val shizukuInstalled = runCatching {
            context.packageManager.getPackageInfo(SHIZUKU_PACKAGE_NAME, 0)
            true
        }.getOrDefault(false)

        val shizukuBinderAlive = checkShizukuBinderViaReflection()
        val shizukuGranted = checkShizukuPermissionViaReflection()

        val resolved = when {
            shizukuBinderAlive && shizukuGranted -> ShizukuIntegrationStatus.READY_SHIZUKU
            hasWriteSecure -> ShizukuIntegrationStatus.READY_WRITE_SECURE_SETTINGS
            shizukuBinderAlive -> ShizukuIntegrationStatus.PERMISSION_REQUIRED
            shizukuInstalled -> ShizukuIntegrationStatus.INSTALLED_NOT_RUNNING
            else -> ShizukuIntegrationStatus.NOT_INSTALLED
        }
        _status.value = resolved
        return resolved
    }

    fun requestShizukuPermissionOrOpenApp(): Boolean {
        refreshStatus()
        // Tente d'invoker Shizuku.requestPermission(1001) si la bibliothèque cliente est chargée
        val requested = runCatching {
            val clazz = Class.forName("rikka.shizuku.Shizuku")
            val method = clazz.getMethod("requestPermission", Int::class.javaPrimitiveType)
            method.invoke(null, 1001)
            true
        }.getOrDefault(false)

        if (requested) return true

        // Sinon ouvre l'application Shizuku si elle est installée
        return runCatching {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE_NAME)
            if (launchIntent != null) {
                launchIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                true
            } else {
                false
            }
        }.getOrDefault(false)
    }

    /**
     * Applique la liste des icônes à masquer dans `Settings.Secure.icon_blacklist`
     * en sauvegardant la configuration initiale de l'utilisateur.
     */
    fun applyHiddenStatusBarIcons(enabled: Boolean, iconSlots: Set<String>): Boolean {
        if (!enabled) {
            return restoreOriginalStatusBarIcons()
        }

        val currentBlacklist = runCatching {
            Settings.Secure.getString(context.contentResolver, KEY_ICON_BLACKLIST)
        }.getOrNull()

        if (preferencesRepository.savedOriginalStatusBarBlacklist.value == null) {
            preferencesRepository.saveOriginalStatusBarBlacklist(currentBlacklist ?: "")
        }

        val existingTokens = (preferencesRepository.savedOriginalStatusBarBlacklist.value ?: "")
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val mergedValue = (existingTokens + iconSlots).distinct().joinToString(",")
        return writeSecureIconBlacklist(mergedValue)
    }

    /**
     * Restaure la liste `icon_blacklist` initiale (appelé à la désactivation ou dans onDestroy du service).
     */
    fun restoreOriginalStatusBarIcons(): Boolean {
        val original = preferencesRepository.savedOriginalStatusBarBlacklist.value ?: return true
        val restored = writeSecureIconBlacklist(original)
        if (restored) {
            preferencesRepository.saveOriginalStatusBarBlacklist(null)
        }
        return restored
    }

    fun getAdbFallbackCommand(): String =
        "adb shell pm grant ${context.packageName} android.permission.WRITE_SECURE_SETTINGS"

    private fun writeSecureIconBlacklist(blacklistCsv: String): Boolean {
        // 1. Essai direct via Settings.Secure (si WRITE_SECURE_SETTINGS accordée)
        val directSuccess = runCatching {
            Settings.Secure.putString(context.contentResolver, KEY_ICON_BLACKLIST, blacklistCsv)
        }.getOrDefault(false)

        if (directSuccess) return true

        // 2. Essai via Shizuku newProcess ("settings put secure icon_blacklist ...")
        return runCatching {
            val shizukuClass = Class.forName("rikka.shizuku.Shizuku")
            val newProcessMethod = shizukuClass.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            newProcessMethod.isAccessible = true
            val cmd = if (blacklistCsv.isBlank()) {
                arrayOf("sh", "-c", "settings delete secure icon_blacklist")
            } else {
                arrayOf("sh", "-c", "settings put secure icon_blacklist $blacklistCsv")
            }
            val process = newProcessMethod.invoke(null, cmd, null, null) as? Process
            process?.waitFor() == 0
        }.getOrDefault(false)
    }

    private fun checkShizukuBinderViaReflection(): Boolean = runCatching {
        val clazz = Class.forName("rikka.shizuku.Shizuku")
        val pingMethod = clazz.getMethod("pingBinder")
        pingMethod.invoke(null) as? Boolean ?: false
    }.getOrDefault(false)

    private fun checkShizukuPermissionViaReflection(): Boolean = runCatching {
        val clazz = Class.forName("rikka.shizuku.Shizuku")
        val checkMethod = clazz.getMethod("checkSelfPermission")
        (checkMethod.invoke(null) as? Int) == PackageManager.PERMISSION_GRANTED
    }.getOrDefault(false)

    companion object {
        const val SHIZUKU_PACKAGE_NAME = "moe.shizuku.privileged.api"
        private const val KEY_ICON_BLACKLIST = "icon_blacklist"

        val AVAILABLE_STATUS_BAR_SLOTS = listOf(
            StatusBarIconSlot("clock", "Horloge", "Clock", "Masque l'heure dans la barre d'état", "Hide time in the status bar"),
            StatusBarIconSlot("battery", "Icône Batterie", "Battery Icon", "Masque l'icône de batterie native", "Hide native battery icon"),
            StatusBarIconSlot("wifi", "Icône Wi-Fi", "Wi-Fi Icon", "Masque l'indicateur de signal Wi-Fi", "Hide Wi-Fi signal indicator"),
            StatusBarIconSlot("mobile", "Réseau Mobile", "Mobile Signal", "Masque les barres 4G/5G", "Hide 4G/5G signal bars"),
            StatusBarIconSlot("bluetooth", "Bluetooth", "Bluetooth", "Masque l'icône d'état Bluetooth", "Hide Bluetooth status icon"),
            StatusBarIconSlot("alarm_clock", "Réveil / Alarme", "Alarm", "Masque l'icône d'alarme programmée", "Hide scheduled alarm icon"),
            StatusBarIconSlot("volume", "Vibreur / Silencieux", "Volume / Mute", "Masque l'icône de mode sonore", "Hide ringer mode icon"),
            StatusBarIconSlot("zen", "Ne pas déranger", "Do Not Disturb", "Masque l'icône Lune / DND", "Hide Do Not Disturb moon icon")
        )
    }
}
