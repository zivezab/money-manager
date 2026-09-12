package com.zive.moneymanager

import com.zive.moneymanager.data.db.entity.RecurrenceUnit
import com.zive.moneymanager.data.db.entity.RecurringRule
import com.zive.moneymanager.data.db.entity.TxType
import com.zive.moneymanager.data.recurring.RecurringEngine
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class RecurringEngineTest {

    private fun rule(
        start: LocalDate,
        unit: RecurrenceUnit,
        interval: Int = 1,
    ) = RecurringRule(
        id = 1,
        name = "test",
        type = TxType.EXPENSE,
        amountMinor = 1000,
        accountId = 1,
        unit = unit,
        interval = interval,
        startEpochDay = start.toEpochDay(),
        nextRunEpochDay = start.toEpochDay(),
    )

    @Test
    fun `monthly advances one month`() {
        val start = LocalDate.of(2026, 1, 15)
        val r = rule(start, RecurrenceUnit.MONTH)
        assertEquals(LocalDate.of(2026, 2, 15), RecurringEngine.advance(r, start))
    }

    @Test
    fun `month end instalments do not drift after February`() {
        val start = LocalDate.of(2026, 1, 31)
        val r = rule(start, RecurrenceUnit.MONTH)

        val feb = RecurringEngine.advance(r, start)
        assertEquals(LocalDate.of(2026, 2, 28), feb)

        // Without re-deriving from the rule's start date this would stay on the 28th.
        val mar = RecurringEngine.advance(r, feb)
        assertEquals(LocalDate.of(2026, 3, 31), mar)

        val apr = RecurringEngine.advance(r, mar)
        assertEquals(LocalDate.of(2026, 4, 30), apr)
    }

    @Test
    fun `quarterly advances three months`() {
        val start = LocalDate.of(2026, 1, 10)
        val r = rule(start, RecurrenceUnit.QUARTER)
        assertEquals(LocalDate.of(2026, 4, 10), RecurringEngine.advance(r, start))
    }

    @Test
    fun `fortnightly advances two weeks`() {
        val start = LocalDate.of(2026, 1, 1)
        val r = rule(start, RecurrenceUnit.WEEK, interval = 2)
        assertEquals(LocalDate.of(2026, 1, 15), RecurringEngine.advance(r, start))
    }

    @Test
    fun `yearly advances one year`() {
        val start = LocalDate.of(2026, 6, 30)
        val r = rule(start, RecurrenceUnit.YEAR)
        assertEquals(LocalDate.of(2027, 6, 30), RecurringEngine.advance(r, start))
    }
}
