package com.zive.moneymanager

import com.zive.moneymanager.core.ExpiryBucket
import com.zive.moneymanager.core.ExpiryText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ExpiryTest {

    private val today = LocalDate.of(2026, 9, 13)
    private val todayEpoch = today.toEpochDay()

    private fun bucketIn(days: Long) = ExpiryBucket.of(todayEpoch + days, todayEpoch)

    @Test
    fun `buckets split the timeline without gaps`() {
        assertEquals(ExpiryBucket.EXPIRED, bucketIn(-30))
        assertEquals(ExpiryBucket.EXPIRED, bucketIn(-1))
        assertEquals(ExpiryBucket.TODAY, bucketIn(0))
        assertEquals(ExpiryBucket.NEXT_THREE_DAYS, bucketIn(1))
        assertEquals(ExpiryBucket.NEXT_THREE_DAYS, bucketIn(3))
        assertEquals(ExpiryBucket.THIS_WEEK, bucketIn(4))
        assertEquals(ExpiryBucket.THIS_WEEK, bucketIn(7))
        assertEquals(ExpiryBucket.THIS_MONTH, bucketIn(8))
        assertEquals(ExpiryBucket.THIS_MONTH, bucketIn(31))
        assertEquals(ExpiryBucket.LATER, bucketIn(32))
        assertEquals(ExpiryBucket.LATER, bucketIn(3650))
    }

    @Test
    fun `every day in a wide range lands in exactly one bucket`() {
        // Guards the when-branches against an unhandled gap creeping in.
        (-400L..400L).forEach { offset ->
            val bucket = bucketIn(offset)
            assertTrue("day $offset produced $bucket", bucket != ExpiryBucket.NONE)
        }
    }

    @Test
    fun `no date means no bucket`() {
        assertEquals(ExpiryBucket.NONE, ExpiryBucket.of(null, todayEpoch))
        assertNull(ExpiryText.describe(null, today))
        assertNull(ExpiryText.chip(null, today))
    }

    @Test
    fun `urgent buckets are the ones needing action today`() {
        assertEquals(
            listOf(ExpiryBucket.EXPIRED, ExpiryBucket.TODAY, ExpiryBucket.NEXT_THREE_DAYS),
            ExpiryBucket.urgentBuckets,
        )
    }

    @Test
    fun `bucket order runs most urgent first`() {
        val ordered = ExpiryBucket.entries.sortedBy { it.ordinal }
        assertEquals(ExpiryBucket.EXPIRED, ordered.first())
        assertEquals(ExpiryBucket.NONE, ordered.last())
    }

    @Test
    fun `countdown text reads naturally on both sides of the date`() {
        assertEquals("Expires today", ExpiryText.describe(todayEpoch, today))
        assertEquals("1 day left", ExpiryText.describe(todayEpoch + 1, today))
        assertEquals("5 days left", ExpiryText.describe(todayEpoch + 5, today))
        assertEquals("Expired yesterday", ExpiryText.describe(todayEpoch - 1, today))
        assertEquals("Expired 4 days ago", ExpiryText.describe(todayEpoch - 4, today))
    }

    @Test
    fun `chip text stays short`() {
        assertEquals("Today", ExpiryText.chip(todayEpoch, today))
        assertEquals("3d", ExpiryText.chip(todayEpoch + 3, today))
        assertEquals("2d ago", ExpiryText.chip(todayEpoch - 2, today))
    }
}
