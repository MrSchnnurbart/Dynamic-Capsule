package com.dynamiccapsule.core.database

import com.dynamiccapsule.core.model.CapsuleAction
import com.dynamiccapsule.core.model.CapsuleGestureType
import com.dynamiccapsule.core.model.GestureConfiguration
import com.dynamiccapsule.core.model.ScreenOrientationMode
import org.junit.Assert.assertEquals
import org.junit.Test

class ScreenProfileAndGestureConfigTest {

    @Test
    fun `screen profiles are isolated by resolution and portrait or landscape orientation`() {
        val repo = CapsuleDatabaseRepository(context = null)
        val portrait = repo.getOrCreateProfileForDisplay(1080, 2400, ScreenOrientationMode.PORTRAIT, 20)
        val landscape = repo.getOrCreateProfileForDisplay(2400, 1080, ScreenOrientationMode.LANDSCAPE, 12)

        assertEquals(ScreenOrientationMode.PORTRAIT, portrait.orientation)
        assertEquals(ScreenOrientationMode.LANDSCAPE, landscape.orientation)

        // Mise à jour du profil Portrait avec X = -14, Y = 26
        repo.saveScreenProfile(portrait.copy(xOffsetPx = -14, yOffsetPx = 26))

        val reloadedPortrait = repo.getOrCreateProfileForDisplay(1080, 2400, ScreenOrientationMode.PORTRAIT)
        val reloadedLandscape = repo.getOrCreateProfileForDisplay(2400, 1080, ScreenOrientationMode.LANDSCAPE)

        assertEquals(-14, reloadedPortrait.xOffsetPx)
        assertEquals(26, reloadedPortrait.yOffsetPx)
        assertEquals(0, reloadedLandscape.xOffsetPx)
    }

    @Test
    fun `gesture configuration resolves swipe direction inversion properly`() {
        val config = GestureConfiguration(
            swipeLeftAction = CapsuleAction.TakeScreenshot,
            swipeRightAction = CapsuleAction.ToggleFlashlight,
            invertSwipeDirection = false
        )
        assertEquals(CapsuleAction.TakeScreenshot, config.actionFor(CapsuleGestureType.SWIPE_LEFT))
        assertEquals(CapsuleAction.ToggleFlashlight, config.actionFor(CapsuleGestureType.SWIPE_RIGHT))

        val inverted = config.copy(invertSwipeDirection = true)
        assertEquals(CapsuleAction.ToggleFlashlight, inverted.actionFor(CapsuleGestureType.SWIPE_LEFT))
        assertEquals(CapsuleAction.TakeScreenshot, inverted.actionFor(CapsuleGestureType.SWIPE_RIGHT))
    }
}
