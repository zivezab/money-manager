package com.zive.moneymanager

import com.zive.moneymanager.core.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyTest {

    @Test
    fun `parses plain and grouped input`() {
        assertEquals(1200L, Money.parse("12"))
        assertEquals(1250L, Money.parse("12.5"))
        assertEquals(1234550L, Money.parse("12,345.50"))
        assertEquals(0L, Money.parse("0"))
    }

    @Test
    fun `parses negatives for opening balances`() {
        assertEquals(-45000L, Money.parse("-450"))
    }

    @Test
    fun `rounds half up at the second decimal`() {
        assertEquals(1235L, Money.parse("12.345"))
        assertEquals(1234L, Money.parse("12.344"))
    }

    @Test
    fun `rejects junk`() {
        assertNull(Money.parse(""))
        assertNull(Money.parse("."))
        assertNull(Money.parse("-"))
        assertNull(Money.parse("abc"))
    }

    @Test
    fun `round trips through the editable form`() {
        listOf(0L, 5L, 99L, 100L, 123456L, -7890L).forEach { minor ->
            assertEquals(minor, Money.parse(Money.toEditable(minor)))
        }
    }

    @Test
    fun `formats with symbol and sign`() {
        assertEquals("S$1,234.56", Money.format(123456L, "S$"))
        assertEquals("-S$1,234.56", Money.format(-123456L, "S$"))
        assertEquals("+S$50.00", Money.formatSigned(5000L, "S$"))
        assertEquals("-S$50.00", Money.formatSigned(-5000L, "S$"))
        assertEquals("S$0.00", Money.formatSigned(0L, "S$"))
    }

    @Test
    fun `compact form shortens large amounts`() {
        assertEquals("$1.2k", Money.formatCompact(123456L, "$"))
        assertEquals("$1.2M", Money.formatCompact(123456789L, "$"))
        assertEquals("$45", Money.formatCompact(4500L, "$"))
    }
}
