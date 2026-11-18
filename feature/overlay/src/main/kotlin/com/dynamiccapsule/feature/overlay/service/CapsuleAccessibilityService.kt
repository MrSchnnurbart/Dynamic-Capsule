package com.dynamiccapsule.feature.overlay.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.res.Configuration
import android.view.accessibility.AccessibilityEvent
import com.dynamiccapsule.core.system.CapsuleServiceLocator
import com.dynamiccapsule.feature.overlay.window.OverlayWindowController

/**
 * Service d'Accessibilité principal projetant la Dynamic Capsule via TYPE_ACCESSIBILITY_OVERLAY
 * par-dessus toutes les applications et la barre d'état.
 */
class CapsuleAccessibilityService : AccessibilityService() {

    private var overlayWindowController: OverlayWindowController? = null

    override fun onCreate() {
        super.onCreate()
        CapsuleServiceLocator.initialize(applicationContext)
        instance = this
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        CapsuleServiceLocator.initialize(applicationContext)

        // Démarrage des capteurs système et de la lampe torche
        CapsuleServiceLocator.systemEventMonitor?.start()
        CapsuleServiceLocator.flashlightController?.startMonitoring()

        // Application éventuelle du masquage d'icônes Shizuku si activé
        val generalSettings = CapsuleServiceLocator.preferencesRepository.generalSettings.value
        if (generalSettings.shizukuEnabled) {
            CapsuleServiceLocator.shizukuStatusBarManager?.applyHiddenStatusBarIcons(
                enabled = true,
                iconSlots = generalSettings.hiddenStatusBarIcons
            )
        }

        // Création et attachement de l'overlay Compose 120 fps
        if (overlayWindowController == null) {
            overlayWindowController = OverlayWindowController(this)
        }
        overlayWindowController?.attachOverlay()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        overlayWindowController?.onConfigurationChanged(newConfig)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Les événements d'accessibilité sont utilisés uniquement à la demande pour le Smart Scroll
    }

    override fun onInterrupt() {
        // Nettoyage léger en cas d'interruption temporaire
    }

    override fun onUnbind(intent: Intent?): Boolean {
        cleanupResources()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        cleanupResources()
        super.onDestroy()
    }

    private fun cleanupResources() {
        overlayWindowController?.detachOverlay()
        overlayWindowController = null
        CapsuleServiceLocator.systemEventMonitor?.stop()
        CapsuleServiceLocator.flashlightController?.stopMonitoring()
        CapsuleServiceLocator.shizukuStatusBarManager?.restoreOriginalStatusBarIcons()
        if (instance === this) {
            instance = null
        }
    }

    companion object {
        @Volatile
        var instance: CapsuleAccessibilityService? = null
            private set
    }
}
