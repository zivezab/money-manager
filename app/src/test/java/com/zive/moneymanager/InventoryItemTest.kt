package com.zive.moneymanager

import com.zive.moneymanager.data.db.entity.InventoryItem
import com.zive.moneymanager.data.db.entity.ItemStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InventoryItemTest {

    private fun item(quantity: Double, unitCostMinor: Long?) = InventoryItem(
        name = "Thing",
        nameKey = "thing",
        quantity = quantity,
        unitCostMinor = unitCostMinor,
    )

    @Test
    fun `name key normalises case and whitespace so duplicates group`() {
        assertEquals("olive oil", InventoryItem.keyOf("  Olive Oil "))
        assertEquals(InventoryItem.keyOf("AA Batteries"), InventoryItem.keyOf("aa batteries"))
    }

    @Test
    fun `total value multiplies quantity by unit cost`() {
        assertEquals(960L, item(3.0, 320).totalValueMinor)
        assertEquals(480L, item(1.5, 320).totalValueMinor)
    }

    @Test
    fun `fractional totals round rather than truncate`() {
        // 0.333 x 100 cents would truncate to 33; it should land on 33 by rounding.
        assertEquals(33L, item(0.333, 100).totalValueMinor)
        assertEquals(34L, item(0.336, 100).totalValueMinor)
    }

    @Test
    fun `items without a recorded cost are worth nothing rather than crashing`() {
        assertEquals(0L, item(5.0, null).totalValueMinor)
    }

    @Test
    fun `held statuses are the ones still in the house`() {
        listOf(ItemStatus.IN_STOCK, ItemStatus.TO_DONATE, ItemStatus.TO_DISCARD).forEach {
            assertTrue("$it should count as held", it.isHeld)
            assertFalse(it.isResolved)
        }
        listOf(ItemStatus.USED_UP, ItemStatus.DONATED, ItemStatus.DISCARDED, ItemStatus.SOLD).forEach {
            assertFalse("$it should not count as held", it.isHeld)
            assertTrue(it.isResolved)
        }
    }

    @Test
    fun `only the queued statuses appear on the disposal checklist`() {
        assertEquals(
            listOf(ItemStatus.TO_DONATE, ItemStatus.TO_DISCARD),
            ItemStatus.entries.filter { it.isPendingDisposal },
        )
    }
}
