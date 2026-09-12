package com.zive.moneymanager

import com.zive.moneymanager.data.reminder.ExpiryReminderWorker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.LocalDateTime

class ExpiryReminderScheduleTest {

    @Test
    fun `waits until later today when the hour has not passed`() {
        val now = LocalDateTime.of(2026, 9, 13, 7, 30)
        assertEquals(
            Duration.ofMinutes(90),
            ExpiryReminderWorker.delayUntilNext(hour = 9, now = now),
        )
    }

    @Test
    fun `rolls to tomorrow when the hour has already passed`() {
        val now = LocalDateTime.of(2026, 9, 13, 10, 0)
        assertEquals(
            Duration.ofHours(23),
            ExpiryReminderWorker.delayUntilNext(hour = 9, now = now),
        )
    }

    @Test
    fun `exactly on the hour schedules the next day rather than firing twice`() {
        val now = LocalDateTime.of(2026, 9, 13, 9, 0)
        assertEquals(
            Duration.ofHours(24),
            ExpiryReminderWorker.delayUntilNext(hour = 9, now = now),
        )
    }

    @Test
    fun `delay is always inside a single day`() {
        (0..23).forEach { hour ->
            (0..23).forEach { nowHour ->
                val delay = ExpiryReminderWorker.delayUntilNext(
                    hour = hour,
                    now = LocalDateTime.of(2026, 9, 13, nowHour, 17),
                )
                assertTrue("hour=$hour now=$nowHour gave $delay", !delay.isNegative)
                assertTrue("hour=$hour now=$nowHour gave $delay", delay <= Duration.ofHours(24))
            }
        }
    }

    @Test
    fun `out of range hours are clamped rather than throwing`() {
        val now = LocalDateTime.of(2026, 9, 13, 12, 0)
        assertEquals(
            ExpiryReminderWorker.delayUntilNext(hour = 23, now = now),
            ExpiryReminderWorker.delayUntilNext(hour = 99, now = now),
        )
        assertEquals(
            ExpiryReminderWorker.delayUntilNext(hour = 0, now = now),
            ExpiryReminderWorker.delayUntilNext(hour = -5, now = now),
        )
    }
}
