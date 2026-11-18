package com.dynamiccapsule.core.system

import com.dynamiccapsule.core.model.AnimationOpenStyle
import com.dynamiccapsule.core.model.CapsuleAnimationConfig
import com.dynamiccapsule.core.system.barcode.BarcodeMatrixGenerator
import com.dynamiccapsule.core.system.physics.SpringPhysicsParameters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpringPhysicsAndBarcodeTest {

    @Test
    fun `spring physics calculation respects fluid, bounce and reduceAnimations`() {
        val bounceConfig = CapsuleAnimationConfig(
            openStyle = AnimationOpenStyle.BOUNCE,
            durationMs = 340,
            bounceIntensity = 0.8f,
            reduceAnimations = false
        )
        val bounceSpec = SpringPhysicsParameters.calculateSpec(bounceConfig)
        assertFalse(bounceSpec.isSnapOnly)
        assertTrue("Underdamped for bounce", bounceSpec.dampingRatio < 0.6f)

        val fluidConfig = bounceConfig.copy(openStyle = AnimationOpenStyle.FLUID)
        val fluidSpec = SpringPhysicsParameters.calculateSpec(fluidConfig)
        assertTrue("Near critical damping for fluid", fluidSpec.dampingRatio > 0.9f)

        val reducedConfig = bounceConfig.copy(reduceAnimations = true)
        val reducedSpec = SpringPhysicsParameters.calculateSpec(reducedConfig)
        assertTrue(reducedSpec.isSnapOnly)

        // Vérifie la convergence de l'oscillateur harmonique vers 1.0f
        val posAtZero = SpringPhysicsParameters.evaluateHarmonicSpring(bounceSpec, 0f)
        val posAtSettled = SpringPhysicsParameters.evaluateHarmonicSpring(bounceSpec, 1.5f)
        assertEquals(0f, posAtZero, 0.001f)
        assertEquals(1f, posAtSettled, 0.02f)
    }

    @Test
    fun `qr matrix and code128 barcode generators produce valid structures`() {
        val qr = BarcodeMatrixGenerator.generateQrMatrix("https://dynamiccapsule.app", 25)
        assertEquals(25, qr.size)
        assertEquals(25, qr[0].size)
        // Les coins des Finder Patterns (0,0), (0,24), (24,0) doivent être noirs (true)
        assertTrue(qr[0][0])
        assertTrue(qr[0][24])
        assertTrue(qr[24][0])
        // Le centre (3,3) du Finder Pattern haut-gauche doit être noir (true)
        assertTrue(qr[3][3])

        val code128 = BarcodeMatrixGenerator.generateCode128Bars("CAPSULE-2026")
        assertTrue(code128.size > 40)
        assertTrue(code128.any { it })
        assertTrue(code128.any { !it })
    }
}
