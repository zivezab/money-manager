package com.zive.moneymanager

import com.zive.moneymanager.core.Period
import com.zive.moneymanager.core.PeriodConfig
import com.zive.moneymanager.core.PeriodType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class PeriodTest {

    private val calendar = PeriodConfig(DayOfWeek.MONDAY, monthStartDay = 1)

    @Test
    fun `calendar month covers the whole month`() {
        val period = Period.of(PeriodType.MONTH, LocalDate.of(2026, 2, 14), calendar)
        assertEquals(LocalDate.of(2026, 2, 1), period.start)
        assertEquals(LocalDate.of(2026, 2, 28), period.endInclusive)
        assertEquals(28, period.dayCount)
    }

    @Test
    fun `month starting on payday runs 25th to 24th`() {
        val payday = PeriodConfig(DayOfWeek.MONDAY, monthStartDay = 25)

        // The 20th still belongs to the cycle that began the previous month.
        val early = Period.of(PeriodType.MONTH, LocalDate.of(2026, 3, 20), payday)
        assertEquals(LocalDate.of(2026, 2, 25), early.start)
        assertEquals(LocalDate.of(2026, 3, 24), early.endInclusive)

        val onPayday = Period.of(PeriodType.MONTH, LocalDate.of(2026, 3, 25), payday)
        assertEquals(LocalDate.of(2026, 3, 25), onPayday.start)
        assertEquals(LocalDate.of(2026, 4, 24), onPayday.endInclusive)
    }

    @Test
    fun `month start day is clamped to short months`() {
        val endOfMonth = PeriodConfig(DayOfWeek.MONDAY, monthStartDay = 28)
        val period = Period.of(PeriodType.MONTH, LocalDate.of(2027, 3, 1), endOfMonth)
        // February 2027 has 28 days, so the cycle opens on the 28th.
        assertEquals(LocalDate.of(2027, 2, 28), period.start)
    }

    @Test
    fun `week honours the configured first day`() {
        // 2026-09-13 is a Sunday.
        val mondayWeek = Period.of(PeriodType.WEEK, LocalDate.of(2026, 9, 13), calendar)
        assertEquals(LocalDate.of(2026, 9, 7), mondayWeek.start)
        assertEquals(LocalDate.of(2026, 9, 13), mondayWeek.endInclusive)

        val sundayConfig = PeriodConfig(DayOfWeek.SUNDAY, monthStartDay = 1)
        val sundayWeek = Period.of(PeriodType.WEEK, LocalDate.of(2026, 9, 13), sundayConfig)
        assertEquals(LocalDate.of(2026, 9, 13), sundayWeek.start)
        assertEquals(LocalDate.of(2026, 9, 19), sundayWeek.endInclusive)
    }

    @Test
    fun `quarter spans three months from the quarter start`() {
        val period = Period.of(PeriodType.QUARTER, LocalDate.of(2026, 8, 5), calendar)
        assertEquals(LocalDate.of(2026, 7, 1), period.start)
        assertEquals(LocalDate.of(2026, 9, 30), period.endInclusive)
    }

    @Test
    fun `year spans twelve months`() {
        val period = Period.of(PeriodType.YEAR, LocalDate.of(2026, 8, 5), calendar)
        assertEquals(LocalDate.of(2026, 1, 1), period.start)
        assertEquals(LocalDate.of(2026, 12, 31), period.endInclusive)
    }

    @Test
    fun `stepping back and forward returns to the same period`() {
        listOf(PeriodType.DAY, PeriodType.WEEK, PeriodType.MONTH, PeriodType.QUARTER, PeriodType.YEAR)
            .forEach { type ->
                val start = Period.of(type, LocalDate.of(2026, 3, 31), calendar)
                val roundTrip = start.previous(calendar).next(calendar)
                assertEquals("round trip failed for $type", start, roundTrip)
            }
    }

    @Test
    fun `stepping through a year of months never skips or repeats`() {
        var period = Period.of(PeriodType.MONTH, LocalDate.of(2026, 1, 15), calendar)
        repeat(24) {
            val next = period.next(calendar)
            // Each period must begin the day after the previous one ends.
            assertEquals(period.endInclusive.plusDays(1), next.start)
            period = next
        }
    }

    @Test
    fun `isCurrent only matches the period holding today`() {
        val today = LocalDate.of(2026, 9, 13)
        val thisMonth = Period.of(PeriodType.MONTH, today, calendar)
        assertTrue(thisMonth.isCurrent(today))
        assertFalse(thisMonth.previous(calendar).isCurrent(today))
    }

    @Test
    fun `month trend buckets tile the month exactly`() {
        val period = Period.of(PeriodType.MONTH, LocalDate.of(2026, 9, 1), calendar)
        val buckets = period.trendBuckets(calendar)

        assertEquals(period.start, buckets.first().start)
        assertEquals(period.endInclusive, buckets.last().endInclusive)
        buckets.zipWithNext().forEach { (a, b) ->
            assertEquals(a.endInclusive.plusDays(1), b.start)
        }
    }

    @Test
    fun `year trend buckets give one per month`() {
        val period = Period.of(PeriodType.YEAR, LocalDate.of(2026, 5, 5), calendar)
        assertEquals(12, period.trendBuckets(calendar).size)
    }
}
