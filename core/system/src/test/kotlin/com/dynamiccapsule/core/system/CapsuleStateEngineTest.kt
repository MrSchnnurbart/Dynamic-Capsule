package com.dynamiccapsule.core.system

import com.dynamiccapsule.core.database.CapsuleDatabaseRepository
import com.dynamiccapsule.core.datastore.CapsulePreferencesRepository
import com.dynamiccapsule.core.model.ActiveDynamicCard
import com.dynamiccapsule.core.model.CapsuleDisplayMode
import com.dynamiccapsule.core.model.DynamicCardType
import com.dynamiccapsule.core.model.MiniCapsuleEventPayload
import com.dynamiccapsule.core.model.MiniCapsuleEventType
import com.dynamiccapsule.core.system.engine.CapsuleStateEngine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CapsuleStateEngineTest {

    @Test
    fun `dynamic cards follow user drag and drop priority order and activate split mode when 2+ cards active`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(dispatcher)
        val prefs = CapsulePreferencesRepository(context = null, externalScope = scope)
        val db = CapsuleDatabaseRepository(context = null)
        val engine = CapsuleStateEngine(prefs, db, scope)

        advanceUntilIdle()
        assertEquals(CapsuleDisplayMode.IDLE_CUTOUT, engine.uiState.value.displayMode)

        // Active Média (priorité par défaut 0) et Charge (priorité par défaut 6)
        val chargingCard = ActiveDynamicCard.ChargingStatusCard(batteryPercentage = 80)
        val mediaCard = ActiveDynamicCard.MediaPlayerCard(title = "Veridis Quo", artist = "Daft Punk")
        engine.upsertDynamicCard(chargingCard)
        advanceUntilIdle()
        assertEquals(CapsuleDisplayMode.COMPACT_CARD, engine.uiState.value.displayMode)

        engine.upsertDynamicCard(mediaCard)
        advanceUntilIdle()

        // 2 cartes actives -> SPLIT_COMPACT_CARD, et MEDIA_PLAYER est prioritaire
        assertEquals(CapsuleDisplayMode.SPLIT_COMPACT_CARD, engine.uiState.value.displayMode)
        assertEquals(DynamicCardType.MEDIA_PLAYER, engine.uiState.value.primaryDynamicCard?.cardType)
        assertEquals(DynamicCardType.CHARGING_STATUS, engine.uiState.value.secondaryDynamicCard?.cardType)

        // Réorganisation Drag & Drop : CHARGING_STATUS passe devant MEDIA_PLAYER
        val reordered = listOf(
            DynamicCardType.CHARGING_STATUS,
            DynamicCardType.MEDIA_PLAYER,
            DynamicCardType.ACTIVE_CALL,
            DynamicCardType.TIMER_STOPWATCH,
            DynamicCardType.LIVE_PROGRESS,
            DynamicCardType.FLASHLIGHT,
            DynamicCardType.VOLUME_CONTROL
        )
        prefs.updateDynamicCardOrder(reordered)
        advanceUntilIdle()

        assertEquals(DynamicCardType.CHARGING_STATUS, engine.uiState.value.primaryDynamicCard?.cardType)
        assertEquals(DynamicCardType.MEDIA_PLAYER, engine.uiState.value.secondaryDynamicCard?.cardType)
    }

    @Test
    fun `mini capsule event temporarily interrupts active card and restores it after timeout`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(dispatcher)
        val prefs = CapsulePreferencesRepository(context = null, externalScope = scope)
        val db = CapsuleDatabaseRepository(context = null)
        val engine = CapsuleStateEngine(prefs, db, scope)

        engine.upsertDynamicCard(
            ActiveDynamicCard.TimerStopwatchCard(
                label = "Minuteur Cuisine",
                targetOrBaseEpochMs = 120_000L,
                remainingOrElapsedSeconds = 120L
            )
        )
        advanceUntilIdle()
        assertEquals(CapsuleDisplayMode.COMPACT_CARD, engine.uiState.value.displayMode)

        // Envoi d'une alerte batterie faible 5 % (durée 2500 ms)
        engine.postMiniCapsuleEvent(
            MiniCapsuleEventPayload(
                id = "evt_low_bat",
                type = MiniCapsuleEventType.BATTERY_LOW_5_PERCENT,
                title = "Batterie faible",
                subtitle = "5 %",
                numericValue = 5,
                durationMs = 2500L
            )
        )
        advanceTimeBy(100L)
        assertEquals(CapsuleDisplayMode.MINI_EVENT_POPUP, engine.uiState.value.displayMode)
        assertNotNull(engine.uiState.value.currentMiniEvent)

        // Après expiration du popup, la capsule reprend automatiquement la carte Minuteur
        advanceTimeBy(2600L)
        advanceUntilIdle()
        assertNull(engine.uiState.value.currentMiniEvent)
        assertEquals(CapsuleDisplayMode.COMPACT_CARD, engine.uiState.value.displayMode)
        assertEquals(DynamicCardType.TIMER_STOPWATCH, engine.uiState.value.primaryDynamicCard?.cardType)
    }

    @Test
    fun `disabled mini event is filtered out and lock screen visibility setting is respected`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(dispatcher)
        val prefs = CapsulePreferencesRepository(context = null, externalScope = scope)
        val db = CapsuleDatabaseRepository(context = null)
        val engine = CapsuleStateEngine(prefs, db, scope)

        prefs.setMiniEventEnabled(MiniCapsuleEventType.VPN_CHANGED, false)
        advanceUntilIdle()

        engine.postMiniCapsuleEvent(
            MiniCapsuleEventPayload(
                id = "vpn_1",
                type = MiniCapsuleEventType.VPN_CHANGED,
                title = "VPN",
                subtitle = "Actif"
            )
        )
        advanceUntilIdle()
        assertNull(engine.uiState.value.currentMiniEvent)

        // Test visibilité écran verrouillé
        engine.upsertDynamicCard(ActiveDynamicCard.FlashlightCard(isOn = true))
        advanceUntilIdle()
        assertTrue(engine.uiState.value.activeDynamicCards.isNotEmpty())

        prefs.updateGeneralSettings { it.copy(showCardsOnLockScreen = false) }
        engine.setDeviceLocked(true)
        advanceUntilIdle()
        assertTrue(engine.uiState.value.activeDynamicCards.isEmpty())
    }
}
