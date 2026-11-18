package com.dynamiccapsule.core.system.physics

import com.dynamiccapsule.core.model.AnimationOpenStyle
import com.dynamiccapsule.core.model.CapsuleAnimationConfig
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt

data class SpringPhysicsCalculatedSpec(
    val dampingRatio: Float,
    val stiffness: Float,
    val isSnapOnly: Boolean
)

object SpringPhysicsParameters {

    /**
     * Convertit la configuration d'animation utilisateur en paramètres physiques
     * compatibles avec AndroidX SpringAnimation et Jetpack Compose spring().
     */
    fun calculateSpec(config: CapsuleAnimationConfig): SpringPhysicsCalculatedSpec {
        if (config.reduceAnimations) {
            return SpringPhysicsCalculatedSpec(
                dampingRatio = 1.0f,
                stiffness = 10_000f,
                isSnapOnly = true
            )
        }

        val clampedDuration = config.durationMs.coerceIn(150, 900)
        val clampedBounce = config.bounceIntensity.coerceIn(0f, 1f)

        val dampingRatio = when (config.openStyle) {
            AnimationOpenStyle.FLUID -> 0.94f // Amortissement quasi-critique sans dépassement brusque
            AnimationOpenStyle.BOUNCE -> (0.82f - clampedBounce * 0.48f).coerceIn(0.32f, 0.85f)
        }

        // Plus la durée demandée est courte, plus la raideur (stiffness) du ressort est élevée
        val stiffness = (1150f * (340f / clampedDuration.toFloat())).coerceIn(180f, 2400f)

        return SpringPhysicsCalculatedSpec(
            dampingRatio = dampingRatio,
            stiffness = stiffness,
            isSnapOnly = false
        )
    }

    /**
     * Évalue analytiquement la position normalisée [0f -> 1f] d'un oscillateur harmonique amorti
     * (loi de Hooke F = -k*x - c*v) à l'instant tSeconds.
     */
    fun evaluateHarmonicSpring(
        spec: SpringPhysicsCalculatedSpec,
        timeSeconds: Float
    ): Float {
        if (spec.isSnapOnly || timeSeconds <= 0f) {
            return if (spec.isSnapOnly) 1f else 0f
        }
        val omega0 = sqrt(spec.stiffness.toDouble())
        val zeta = spec.dampingRatio.toDouble().coerceIn(0.1, 1.0)
        val t = timeSeconds.toDouble()

        val displacement = if (zeta < 1.0) {
            val omegaD = omega0 * sqrt(1.0 - zeta * zeta)
            val envelope = exp(-zeta * omega0 * t)
            val harmonic = cos(omegaD * t) + (zeta / sqrt(1.0 - zeta * zeta)) * sin(omegaD * t)
            1.0 - envelope * harmonic
        } else {
            val envelope = exp(-omega0 * t)
            1.0 - envelope * (1.0 + omega0 * t)
        }
        return displacement.toFloat()
    }
}
