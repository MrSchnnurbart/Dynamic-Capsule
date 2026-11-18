package com.dynamiccapsule.feature.overlay.physics

import android.view.Choreographer
import com.dynamiccapsule.core.model.CapsuleAnimationConfig
import com.dynamiccapsule.core.system.physics.SpringPhysicsParameters
import kotlin.math.abs

/**
 * Contrôleur d'animation physique basé sur la loi de Hooke (SpringAnimation : F = -k*x - c*v)
 * cadencé sur le VSYNC 60/120 fps via Choreographer.FrameCallback.
 */
class CapsuleSpringAnimationController(
    private val onValueUpdate: (Float) -> Unit,
    private val onEnd: (() -> Unit)? = null
) {
    private var currentValue: Float = 0f
    private var targetValue: Float = 0f
    private var velocity: Float = 0f
    private var lastFrameTimeNanos: Long = 0L
    private var isRunning: Boolean = false
    private var currentConfig: CapsuleAnimationConfig = CapsuleAnimationConfig()

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!isRunning) return
            if (lastFrameTimeNanos == 0L) {
                lastFrameTimeNanos = frameTimeNanos
                Choreographer.getInstance().postFrameCallback(this)
                return
            }

            // Pas de temps dt plafonné à 32 ms pour éviter toute instabilité numérique
            val dtSeconds = ((frameTimeNanos - lastFrameTimeNanos) / 1_000_000_000f).coerceIn(0.001f, 0.032f)
            lastFrameTimeNanos = frameTimeNanos

            val finished = stepPhysics(dtSeconds)
            onValueUpdate(currentValue)

            if (finished) {
                isRunning = false
                onEnd?.invoke()
            } else {
                Choreographer.getInstance().postFrameCallback(this)
            }
        }
    }

    fun animateToFinalPosition(
        start: Float,
        target: Float,
        config: CapsuleAnimationConfig,
        initialVelocity: Float = 0f
    ) {
        currentConfig = config
        val spec = SpringPhysicsParameters.calculateSpec(config)
        if (spec.isSnapOnly) {
            cancel()
            currentValue = target
            targetValue = target
            velocity = 0f
            onValueUpdate(target)
            onEnd?.invoke()
            return
        }

        currentValue = start
        targetValue = target
        velocity = initialVelocity
        if (!isRunning) {
            isRunning = true
            lastFrameTimeNanos = 0L
            runCatching {
                Choreographer.getInstance().postFrameCallback(frameCallback)
            }
        }
    }

    /**
     * Effectue un pas d'intégration symplectique d'Euler d'un système masse-ressort-amortisseur.
     * Retourne true lorsque le ressort a convergé à sa position d'équilibre.
     */
    internal fun stepPhysics(dtSeconds: Float): Boolean {
        val spec = SpringPhysicsParameters.calculateSpec(currentConfig)
        if (spec.isSnapOnly) {
            currentValue = targetValue
            velocity = 0f
            return true
        }

        val stiffness = spec.stiffness
        val dampingCoefficient = 2f * spec.dampingRatio * kotlin.math.sqrt(stiffness)

        val displacement = currentValue - targetValue
        val springForce = -stiffness * displacement
        val dampingForce = -dampingCoefficient * velocity
        val acceleration = springForce + dampingForce

        velocity += acceleration * dtSeconds
        currentValue += velocity * dtSeconds

        val isAtRest = abs(currentValue - targetValue) < 0.35f && abs(velocity) < 4.0f
        if (isAtRest) {
            currentValue = targetValue
            velocity = 0f
            return true
        }
        return false
    }

    fun cancel() {
        if (isRunning) {
            isRunning = false
            runCatching {
                Choreographer.getInstance().removeFrameCallback(frameCallback)
            }
        }
    }
}
