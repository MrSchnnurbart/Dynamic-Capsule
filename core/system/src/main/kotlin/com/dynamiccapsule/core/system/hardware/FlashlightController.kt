package com.dynamiccapsule.core.system.hardware

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.dynamiccapsule.core.model.ActiveDynamicCard
import com.dynamiccapsule.core.model.DynamicCardType
import com.dynamiccapsule.core.system.engine.CapsuleStateEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Contrôleur matériel de la lampe torche utilisant CameraManager.TorchCallback
 * avec support du réglage d'intensité sur Android 13+ (API 33+).
 */
class FlashlightController(
    context: Context,
    private val stateEngine: CapsuleStateEngine
) {
    private val cameraManager = context.applicationContext.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private var torchCameraId: String? = null
    private var maxStrengthLevel: Int = 1

    private val _isTorchOn = MutableStateFlow(false)
    val isTorchOn: StateFlow<Boolean> = _isTorchOn.asStateFlow()

    private val _currentStrength = MutableStateFlow(1)
    val currentStrength: StateFlow<Int> = _currentStrength.asStateFlow()

    private var isCallbackRegistered = false

    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            if (cameraId == torchCameraId || torchCameraId == null) {
                torchCameraId = cameraId
                _isTorchOn.value = enabled
                syncCardState()
            }
        }

        override fun onTorchStrengthLevelChanged(cameraId: String, newStrengthLevel: Int) {
            if (cameraId == torchCameraId) {
                _currentStrength.value = newStrengthLevel
                syncCardState()
            }
        }
    }

    fun startMonitoring() {
        val manager = cameraManager ?: return
        if (isCallbackRegistered) return
        runCatching {
            val id = manager.cameraIdList.firstOrNull { camId ->
                val chars = manager.getCameraCharacteristics(camId)
                chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
            torchCameraId = id
            if (id != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val chars = manager.getCameraCharacteristics(id)
                val maxLvl = chars.get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL) ?: 1
                maxStrengthLevel = maxLvl.coerceAtLeast(1)
                _currentStrength.value = maxStrengthLevel
            }
            manager.registerTorchCallback(torchCallback, mainHandler)
            isCallbackRegistered = true
        }
    }

    fun stopMonitoring() {
        if (!isCallbackRegistered) return
        runCatching {
            cameraManager?.unregisterTorchCallback(torchCallback)
        }
        isCallbackRegistered = false
    }

    fun toggleFlashlight() {
        setFlashlightEnabled(!_isTorchOn.value)
    }

    fun setFlashlightEnabled(enabled: Boolean) {
        val manager = cameraManager
        val camId = torchCameraId
        if (manager != null && camId != null) {
            runCatching {
                manager.setTorchMode(camId, enabled)
            }.onFailure {
                // Fallback état simulé si l'émulateur n'a pas de flash matériel
                _isTorchOn.value = enabled
                syncCardState()
            }
        } else {
            _isTorchOn.value = enabled
            syncCardState()
        }
    }

    fun setFlashlightStrength(level: Int) {
        val clamped = level.coerceIn(1, maxStrengthLevel.coerceAtLeast(5))
        _currentStrength.value = clamped
        val manager = cameraManager
        val camId = torchCameraId
        if (manager != null && camId != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && maxStrengthLevel > 1) {
            runCatching {
                manager.turnOnTorchWithStrengthLevel(camId, clamped)
                _isTorchOn.value = true
            }
        }
        syncCardState()
    }

    private fun syncCardState() {
        if (_isTorchOn.value) {
            stateEngine.upsertDynamicCard(
                ActiveDynamicCard.FlashlightCard(
                    isOn = true,
                    strengthLevel = _currentStrength.value,
                    maxStrengthLevel = maxStrengthLevel.coerceAtLeast(5)
                )
            )
        } else {
            stateEngine.removeDynamicCard(DynamicCardType.FLASHLIGHT)
        }
    }
}
