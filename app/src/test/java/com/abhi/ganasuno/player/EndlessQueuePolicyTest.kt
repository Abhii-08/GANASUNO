package com.abhi.ganasuno.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EndlessQueuePolicyTest {
    @Test
    fun automaticRefillRequiresActiveOptInWithinLowWaterMark() {
        assertTrue(EndlessQueuePolicy.shouldRefill(true, true, false, true, false, 5))
        assertFalse(EndlessQueuePolicy.shouldRefill(true, true, false, true, false, 6))
        assertFalse(EndlessQueuePolicy.shouldRefill(false, true, false, true, false, 0))
        assertFalse(EndlessQueuePolicy.shouldRefill(true, false, false, true, false, 0))
        assertFalse(EndlessQueuePolicy.shouldRefill(true, true, false, false, false, 0))
    }

    @Test
    fun explicitNextCanRequestRefillWhilePaused() {
        assertTrue(EndlessQueuePolicy.shouldRefill(true, false, true, true, false, 0))
        assertFalse(EndlessQueuePolicy.shouldRefill(false, false, true, true, false, 0))
        assertFalse(EndlessQueuePolicy.shouldRefill(true, false, true, true, true, 0))
    }

    @Test
    fun sequentialPruningKeepsHistoryAndDoesNotTouchShuffledOrder() {
        assertEquals(0, EndlessQueuePolicy.trimPrefix(40, false))
        assertEquals(50, EndlessQueuePolicy.trimPrefix(100, false))
        assertEquals(0, EndlessQueuePolicy.trimPrefix(100, true))
    }
}
