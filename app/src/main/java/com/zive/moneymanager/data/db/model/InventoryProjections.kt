package com.zive.moneymanager.data.db.model

import androidx.room.Embedded
import com.zive.moneymanager.data.db.entity.InventoryItem

/** An item joined with its category's display fields. */
data class InventoryItemDetail(
    @Embedded val item: InventoryItem,
    val categoryName: String?,
    val categoryColor: String?,
    val categoryIcon: String?,
)

/**
 * Several held items sharing a name — the signal behind "you already own three
 * of these".
 */
data class DuplicateGroup(
    val nameKey: String,
    val displayName: String,
    val itemCount: Int,
    val totalQuantity: Double,
    val totalValueMinor: Long,
    val locations: String?,
)

/** Headline inventory numbers for the overview card and the tab badge. */
data class InventorySummary(
    val heldCount: Int = 0,
    val totalValueMinor: Long = 0,
    val expiredCount: Int = 0,
    val expiringSoonCount: Int = 0,
    val toDonateCount: Int = 0,
    val toDiscardCount: Int = 0,
) {
    val pendingDisposalCount: Int get() = toDonateCount + toDiscardCount

    /** Anything the user should act on right now. */
    val needsAttentionCount: Int get() = expiredCount + expiringSoonCount
}
