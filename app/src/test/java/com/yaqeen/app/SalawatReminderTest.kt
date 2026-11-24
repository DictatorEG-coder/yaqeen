package com.yaqeen.app

import org.junit.Assert.assertEquals
import org.junit.Test

class SalawatReminderTest {
    @Test
    fun preservesSupportedIntervals() {
        listOf(1, 2, 3, 4, 6, 8, 12, 24).forEach {
            assertEquals(it, normalizeSalawatIntervalHours(it))
            assertEquals(it * 60L * 60L * 1000L, salawatIntervalMillis(it))
        }
    }

    @Test
    fun clampsIntervalsToSafeRange() {
        assertEquals(1, normalizeSalawatIntervalHours(-5))
        assertEquals(1, normalizeSalawatIntervalHours(0))
        assertEquals(24, normalizeSalawatIntervalHours(25))
        assertEquals(24, normalizeSalawatIntervalHours(100))
    }
}
