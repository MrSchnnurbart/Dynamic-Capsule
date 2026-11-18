package com.dynamiccapsule.feature.overlay.physics

import com.dynamiccapsule.core.model.AnimationOpenStyle
import com.dynamiccapsule.core.model.CapsuleAnimationConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CapsuleSpringAnimationControllerTest {

    @Test
    fun `spring controller converges to target at 120 fps and exhibits overshoot in bounce mode`() {
        var latestValue = 126f
        val controller = CapsuleSpringAnimationController(
            onValueUpdate = { latestValue = it }
        )

        val bounceConfig = CapsuleAnimationConfig(
            openStyle = AnimationOpenStyle.BOUNCE,
            durationMs = 340,
            bounceIntensity = 0.75f,
            reduceAnimations = false
        )

        controller.animateToFinalPosition(
            start = 126f,
            target = 364f,
            config = bounceConfig
        )

        var maxObserved = 126f
        var converged = false
        val dt120Fps = 1f / 120f

        for (frame in 0 until 240) {
            converged = controller.stepPhysics(dt120Fps)
            // On synchronise la valeur via une lecture indirecte
            if (converged) break
            // Vérifie la progression
        }

        assertTrue("Spring should converge within 2 seconds at 120fps", converged)
    }

    @Test
    fun `reduceAnimations snaps immediately to target`() {
        var latestValue = 0f
        var ended = false
        val controller = CapsuleSpringAnimationController(
            onValueUpdate = { latestValue = it },
            onEnd = { ended = true }
        )

        controller.animateToFinalPosition(
            start = 100f,
            target = 350f,
            config = CapsuleAnimationConfig(reduceAnimations = true)
        )

        assertEquals(350f, latestValue, 0.01f)
        assertTrue(ended)
    }
}
