package com.dynamiccapsule.feature.overlay.window

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import com.dynamiccapsule.core.model.ActiveDrawerContent
import com.dynamiccapsule.core.model.CapsuleDisplayMode
import com.dynamiccapsule.core.model.ScreenOrientationMode
import com.dynamiccapsule.core.system.CapsuleServiceLocator
import com.dynamiccapsule.feature.overlay.action.CapsuleActionExecutor
import com.dynamiccapsule.feature.overlay.ui.CapsuleOverlayRoot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Contrôleur de la fenêtre système TYPE_ACCESSIBILITY_OVERLAY :
 * - Gère l'attachement/détachement propre de la ComposeView au WindowManager.
 * - Désactive l'émission d'événements d'accessibilité auto-référentiels (IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS)
 *   pour garantir 0 % CPU au repos et aucune boucle d'événements avec system_server.
 */
class OverlayWindowController(
    private val service: AccessibilityService
) {
    private val windowManager = service.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var lifecycleOwner: OverlayLifecycleOwner? = null
    private var composeView: ComposeView? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var scope: CoroutineScope? = null
    private var isAttached = false

    fun attachOverlay() {
        if (isAttached) return
        val controllerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        scope = controllerScope

        val owner = OverlayLifecycleOwner().also { it.onCreate() }
        lifecycleOwner = owner

        syncScreenMetricsAndCutout(service.resources.configuration)

        val initialProfile = CapsuleServiceLocator.stateEngine.uiState.value.currentProfile
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            buildWindowFlags(needsKeyboardFocus = false, showOnLockScreen = true),
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            x = initialProfile.xOffsetPx
            y = initialProfile.yOffsetPx
            layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            title = "DynamicCapsuleOverlay"
        }
        layoutParams = params

        val view = ComposeView(service).apply {
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
            owner.attachToView(this)
            setContent {
                val uiState by CapsuleServiceLocator.stateEngine.uiState.collectAsState()
                CapsuleOverlayRoot(
                    state = uiState,
                    onExecuteAction = { action ->
                        CapsuleActionExecutor.executeAction(service, action, service)
                    }
                )
            }
        }
        composeView = view

        runCatching {
            windowManager.addView(view, params)
            isAttached = true
        }

        controllerScope.launch {
            CapsuleServiceLocator.stateEngine.uiState
                .map { state ->
                    WindowLayoutTarget(
                        xPx = state.currentProfile.xOffsetPx,
                        yPx = state.currentProfile.yOffsetPx,
                        needsKeyboard = state.displayMode == CapsuleDisplayMode.ACTION_DRAWER &&
                            state.activeDrawerContent is ActiveDrawerContent.AppSearchDrawer,
                        showOnLockScreen = state.generalSettings.showCardsOnLockScreen,
                        isHidden = state.displayMode == CapsuleDisplayMode.HIDDEN
                    )
                }
                .distinctUntilChanged()
                .collectLatest { target ->
                    updateWindowParameters(target)
                }
        }
    }

    fun onConfigurationChanged(newConfig: Configuration) {
        syncScreenMetricsAndCutout(newConfig)
        CapsuleServiceLocator.systemEventMonitor?.onConfigurationChanged(newConfig)
    }

    private fun syncScreenMetricsAndCutout(config: Configuration) {
        val metrics = windowManager.currentWindowMetrics
        val bounds = metrics.bounds
        val cutout = metrics.windowInsets.displayCutout
        val safeTopPx = cutout?.safeInsetTop?.takeIf { it > 0 }?.let { (it / 4).coerceAtLeast(8) } ?: 12

        val orientation = if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            ScreenOrientationMode.LANDSCAPE
        } else {
            ScreenOrientationMode.PORTRAIT
        }
        val isNight = (config.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES

        CapsuleServiceLocator.stateEngine.onDisplayConfigurationChanged(
            widthPx = bounds.width(),
            heightPx = bounds.height(),
            orientation = orientation,
            cutoutTopPx = safeTopPx,
            isNightMode = isNight
        )
    }

    private fun updateWindowParameters(target: WindowLayoutTarget) {
        val view = composeView ?: return
        val params = layoutParams ?: return
        if (!isAttached) return

        val desiredFlags = buildWindowFlags(
            needsKeyboardFocus = target.needsKeyboard,
            showOnLockScreen = target.showOnLockScreen,
            isPassThroughHidden = target.isHidden
        )

        if (params.x != target.xPx || params.y != target.yPx || params.flags != desiredFlags) {
            params.x = target.xPx
            params.y = target.yPx
            params.flags = desiredFlags
            runCatching {
                windowManager.updateViewLayout(view, params)
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun buildWindowFlags(
        needsKeyboardFocus: Boolean,
        showOnLockScreen: Boolean,
        isPassThroughHidden: Boolean = false
    ): Int {
        var flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED

        if (!needsKeyboardFocus) {
            flags = flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        }
        if (isPassThroughHidden) {
            flags = flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        }
        if (showOnLockScreen) {
            flags = flags or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
        }
        return flags
    }

    fun detachOverlay() {
        scope?.cancel()
        scope = null

        if (isAttached) {
            composeView?.let { view ->
                runCatching { windowManager.removeViewImmediate(view) }
            }
            isAttached = false
        }
        composeView = null
        layoutParams = null
        lifecycleOwner?.onDestroy()
        lifecycleOwner = null
    }

    private data class WindowLayoutTarget(
        val xPx: Int,
        val yPx: Int,
        val needsKeyboard: Boolean,
        val showOnLockScreen: Boolean,
        val isHidden: Boolean
    )
}
