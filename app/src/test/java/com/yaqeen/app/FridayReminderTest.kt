package com.yaqeen.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class FridayReminderTest {
    @Test
    fun nextFridayUsesLocalCalendarAndConfiguredTime() {
        val monday = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 23, 10, 30, 0); set(Calendar.MILLISECOND, 0)
        }
        val result = Calendar.getInstance().apply { timeInMillis = nextFridayAt(monday.timeInMillis, 14, 25) }
        assertEquals(Calendar.FRIDAY, result.get(Calendar.DAY_OF_WEEK))
        assertEquals(14, result.get(Calendar.HOUR_OF_DAY))
        assertEquals(25, result.get(Calendar.MINUTE))
    }

    @Test
    fun sameDayPastTimeMovesToFollowingFriday() {
        val friday = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 27, 15, 0, 0); set(Calendar.MILLISECOND, 0)
        }
        val result = Calendar.getInstance().apply { timeInMillis = nextFridayAt(friday.timeInMillis, 9, 0) }
        assertEquals(Calendar.FRIDAY, result.get(Calendar.DAY_OF_WEEK))
        assertEquals(4, result.get(Calendar.DAY_OF_MONTH))
        assertEquals(9, result.get(Calendar.HOUR_OF_DAY))
    }

    @Test
    fun fridaySalawatRepeatsWithinFridayThenRestartsNextWeek() {
        val friday = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 27, 9, 0, 0); set(Calendar.MILLISECOND, 0)
        }
        val next = Calendar.getInstance().apply {
            timeInMillis = nextFridaySalawatAt(friday.timeInMillis, 9, 0, 3)
        }
        assertEquals(Calendar.FRIDAY, next.get(Calendar.DAY_OF_WEEK))
        assertEquals(12, next.get(Calendar.HOUR_OF_DAY))

        val lateFriday = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 27, 22, 30, 0); set(Calendar.MILLISECOND, 0)
        }
        val following = Calendar.getInstance().apply {
            timeInMillis = nextFridaySalawatAt(lateFriday.timeInMillis, 9, 0, 3)
        }
        assertTrue(following.timeInMillis > lateFriday.timeInMillis)
        assertEquals(Calendar.FRIDAY, following.get(Calendar.DAY_OF_WEEK))
        assertEquals(9, following.get(Calendar.HOUR_OF_DAY))
    }
}
