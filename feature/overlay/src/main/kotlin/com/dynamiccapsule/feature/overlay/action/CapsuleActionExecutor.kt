package com.dynamiccapsule.feature.overlay.action

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.content.Intent
import android.graphics.Path
import android.media.AudioManager
import android.net.Uri
import android.provider.Settings
import android.view.accessibility.AccessibilityNodeInfo
import com.dynamiccapsule.core.model.AccessibilityActionType
import com.dynamiccapsule.core.model.ActiveDrawerContent
import com.dynamiccapsule.core.model.AiAssistantType
import com.dynamiccapsule.core.model.CapsuleAction
import com.dynamiccapsule.core.model.MiniCapsuleEventPayload
import com.dynamiccapsule.core.model.MiniCapsuleEventType
import com.dynamiccapsule.core.model.RingerStateMode
import com.dynamiccapsule.core.model.ScrollDirection
import com.dynamiccapsule.core.model.SystemSettingsTarget
import com.dynamiccapsule.core.system.CapsuleServiceLocator
import com.dynamiccapsule.core.system.notification.CapsuleNotificationListenerService
import java.util.UUID

/**
 * Exécuteur des 16 catégories d'actions assignables aux gestes de la capsule.
 */
object CapsuleActionExecutor {

    fun executeAction(
        context: Context,
        action: CapsuleAction,
        accessibilityService: AccessibilityService? = null
    ) {
        val appContext = context.applicationContext
        val engine = CapsuleServiceLocator.stateEngine

        when (action) {
            CapsuleAction.None -> Unit

            is CapsuleAction.ScrollPage -> {
                if (accessibilityService != null) {
                    performSmartScroll(accessibilityService, action.direction)
                }
            }

            CapsuleAction.TakeScreenshot -> {
                accessibilityService?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT)
            }

            is CapsuleAction.LaunchApp -> {
                launchPackage(appContext, action.packageName)
            }

            CapsuleAction.OpenAppSearch -> {
                engine.openActionDrawer(ActiveDrawerContent.AppSearchDrawer())
            }

            CapsuleAction.ToggleFlashlight -> {
                CapsuleServiceLocator.flashlightController?.toggleFlashlight()
            }

            is CapsuleAction.OpenShortcutsSlidersBarcodes -> {
                val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                val curVol = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 10
                val maxVol = (audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15).coerceAtLeast(1)
                val volPct = ((curVol * 100f) / maxVol).toInt().coerceIn(0, 100)

                val curBright = runCatching {
                    Settings.System.getInt(appContext.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
                }.getOrDefault(180)
                val brightPct = ((curBright * 100f) / 255f).toInt().coerceIn(5, 100)
                val isAuto = runCatching {
                    Settings.System.getInt(appContext.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE) ==
                        Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
                }.getOrDefault(true)

                engine.openActionDrawer(
                    ActiveDrawerContent.SlidersShortcutsBarcodes(
                        selectedTab = action.initialTab,
                        brightnessPercent = brightPct,
                        isAutoBrightness = isAuto,
                        mediaVolumePercent = volPct
                    )
                )
            }

            is CapsuleAction.ShowWidget -> {
                engine.openActionDrawer(
                    ActiveDrawerContent.WidgetContainer(
                        appWidgetId = action.appWidgetId,
                        providerPackage = action.providerPackage,
                        label = action.label
                    )
                )
            }

            is CapsuleAction.MakeDirectCall -> {
                runCatching {
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(action.phoneNumber)}")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    appContext.startActivity(intent)
                }
            }

            is CapsuleAction.SendCustomBroadcast -> {
                runCatching {
                    val intent = Intent(action.action).apply {
                        if (action.targetPackage.isNotBlank()) {
                            setPackage(action.targetPackage)
                        }
                        if (action.extraKey.isNotBlank()) {
                            putExtra(action.extraKey, action.extraValue)
                        }
                    }
                    appContext.sendBroadcast(intent)
                }
            }

            is CapsuleAction.ShowQrCode -> {
                engine.openActionDrawer(
                    ActiveDrawerContent.QrBarcodeViewer(
                        content = action.content,
                        label = action.label,
                        isBarcode128 = action.isBarcode128
                    )
                )
            }

            is CapsuleAction.MediaControl -> {
                val listener = CapsuleNotificationListenerService.instance
                if (listener != null) {
                    listener.dispatchMediaCommand(action.command)
                } else {
                    // Fallback via AudioManager MediaKeyEvent
                    val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                    val keyCode = when (action.command) {
                        com.dynamiccapsule.core.model.MediaCommandType.PLAY_PAUSE -> android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                        com.dynamiccapsule.core.model.MediaCommandType.NEXT_TRACK -> android.view.KeyEvent.KEYCODE_MEDIA_NEXT
                        com.dynamiccapsule.core.model.MediaCommandType.PREVIOUS_TRACK -> android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS
                        com.dynamiccapsule.core.model.MediaCommandType.STOP -> android.view.KeyEvent.KEYCODE_MEDIA_STOP
                    }
                    audioManager?.dispatchMediaKeyEvent(
                        android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, keyCode)
                    )
                    audioManager?.dispatchMediaKeyEvent(
                        android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, keyCode)
                    )
                }
            }

            is CapsuleAction.PerformAccessibilityAction -> {
                val globalId = when (action.actionType) {
                    AccessibilityActionType.NOTIFICATIONS -> AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS
                    AccessibilityActionType.QUICK_SETTINGS -> AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS
                    AccessibilityActionType.RECENTS -> AccessibilityService.GLOBAL_ACTION_RECENTS
                    AccessibilityActionType.HOME -> AccessibilityService.GLOBAL_ACTION_HOME
                    AccessibilityActionType.BACK -> AccessibilityService.GLOBAL_ACTION_BACK
                    AccessibilityActionType.POWER_DIALOG -> AccessibilityService.GLOBAL_ACTION_POWER_DIALOG
                }
                accessibilityService?.performGlobalAction(globalId)
            }

            is CapsuleAction.LaunchAiAssistant -> {
                launchAiAssistant(appContext, action.assistantType)
            }

            CapsuleAction.ToggleAutoBrightness -> {
                val canWrite = Settings.System.canWrite(appContext)
                if (canWrite) {
                    val current = runCatching {
                        Settings.System.getInt(appContext.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE)
                    }.getOrDefault(Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
                    val next = if (current == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
                        Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
                    } else {
                        Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
                    }
                    runCatching {
                        Settings.System.putInt(appContext.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, next)
                    }
                } else {
                    val isAutoNow = engine.uiState.value.currentMiniEvent?.isActiveState != true
                    engine.postMiniCapsuleEvent(
                        MiniCapsuleEventPayload(
                            id = UUID.randomUUID().toString(),
                            type = MiniCapsuleEventType.ADAPTIVE_BRIGHTNESS_CHANGED,
                            title = if (isAutoNow) "Luminosité Auto" else "Luminosité Manuelle",
                            subtitle = "Basculement rapide",
                            isActiveState = isAutoNow,
                            accentColorArgb = 0xFFFFD54FL
                        ),
                        bypassFilter = true
                    )
                }
            }

            CapsuleAction.CycleVolumeRingerMode -> {
                val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                val currentMode = audioManager?.ringerMode ?: AudioManager.RINGER_MODE_NORMAL
                val nextMode = when (currentMode) {
                    AudioManager.RINGER_MODE_NORMAL -> AudioManager.RINGER_MODE_VIBRATE
                    AudioManager.RINGER_MODE_VIBRATE -> AudioManager.RINGER_MODE_SILENT
                    else -> AudioManager.RINGER_MODE_NORMAL
                }
                runCatching {
                    audioManager?.ringerMode = nextMode
                }.onFailure {
                    engine.postMiniCapsuleEvent(
                        MiniCapsuleEventPayload(
                            id = UUID.randomUUID().toString(),
                            type = MiniCapsuleEventType.RINGER_MODE_CHANGED,
                            title = "Mode Vibreur",
                            subtitle = "Profil sonore modifié",
                            ringerMode = RingerStateMode.VIBRATE,
                            accentColorArgb = 0xFFFFB300L
                        ),
                        bypassFilter = true
                    )
                }
            }

            CapsuleAction.LockScreen -> {
                accessibilityService?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)
            }

            is CapsuleAction.OpenSystemSettings -> {
                val settingsAction = when (action.target) {
                    SystemSettingsTarget.MAIN_SETTINGS -> Settings.ACTION_SETTINGS
                    SystemSettingsTarget.WIFI -> Settings.ACTION_WIFI_SETTINGS
                    SystemSettingsTarget.BLUETOOTH -> Settings.ACTION_BLUETOOTH_SETTINGS
                    SystemSettingsTarget.SOUND -> Settings.ACTION_SOUND_SETTINGS
                    SystemSettingsTarget.DISPLAY -> Settings.ACTION_DISPLAY_SETTINGS
                    SystemSettingsTarget.BATTERY -> Intent.ACTION_POWER_USAGE_SUMMARY
                    SystemSettingsTarget.ACCESSIBILITY -> Settings.ACTION_ACCESSIBILITY_SETTINGS
                }
                runCatching {
                    appContext.startActivity(
                        Intent(settingsAction).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                    )
                }
            }
        }
    }

    private fun performSmartScroll(service: AccessibilityService, direction: ScrollDirection) {
        val root = service.rootInActiveWindow
        val scrollable = findFirstScrollableNode(root)
        if (scrollable != null) {
            val action = if (direction == ScrollDirection.TOP) {
                AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
            } else {
                AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
            }
            scrollable.performAction(action)
            return
        }

        // Fallback : injection d'un geste de défilement vertical via GestureDescription
        val metrics = service.resources.displayMetrics
        val centerX = metrics.widthPixels / 2f
        val startY = if (direction == ScrollDirection.TOP) metrics.heightPixels * 0.3f else metrics.heightPixels * 0.75f
        val endY = if (direction == ScrollDirection.TOP) metrics.heightPixels * 0.8f else metrics.heightPixels * 0.25f

        val path = Path().apply {
            moveTo(centerX, startY)
            lineTo(centerX, endY)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0L, 260L))
            .build()
        service.dispatchGesture(gesture, null, null)
    }

    private fun findFirstScrollableNode(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.isScrollable) return node
        for (i in 0 until node.childCount) {
            val found = findFirstScrollableNode(node.getChild(i))
            if (found != null) return found
        }
        return null
    }

    fun launchPackage(context: Context, packageName: String) {
        runCatching {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
                ?: Intent(Settings.ACTION_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    private fun launchAiAssistant(context: Context, assistantType: AiAssistantType) {
        val chatGptPackage = "com.openai.chatgpt"
        val chatGptIntent = context.packageManager.getLaunchIntentForPackage(chatGptPackage)
        if (assistantType != AiAssistantType.SYSTEM_DEFAULT && chatGptIntent != null) {
            chatGptIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { context.startActivity(chatGptIntent) }
            return
        }
        runCatching {
            val voiceIntent = Intent(Intent.ACTION_VOICE_COMMAND).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(voiceIntent)
        }.onFailure {
            runCatching {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://chatgpt.com")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
            }
        }
    }
}
