package com.zive.moneymanager.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One thing you own — a tub of yoghurt or a bicycle. Kept in the same database as
 * the ledger so a purchase can point at the expense that paid for it and a
 * disposal can point at the income from selling it.
 */
@Entity(
    tableName = "inventory_items",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transactionId"],
            // Deleting the receipt should not delete the thing it bought.
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index("categoryId"),
        Index("transactionId"),
        Index("status"),
        Index("expiryEpochDay"),
        Index("nameKey"),
    ],
)
data class InventoryItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /**
     * Lower-cased, trimmed [name]. Stored rather than computed so the duplicate
     * query can group on an indexed column instead of scanning every row.
     */
    val nameKey: String,
    val kind: ItemKind = ItemKind.CONSUMABLE,
    val categoryId: Long? = null,
    /** Free text with presets offered in the editor — "Fridge", "Pantry", "Garage". */
    val location: String = "",
    val quantity: Double = 1.0,
    val unit: QuantityUnit = QuantityUnit.PIECE,
    val purchaseEpochDay: Long? = null,
    /** Consumables only. Null means it does not go off. */
    val expiryEpochDay: Long? = null,
    /** Assets only. */
    val warrantyEndEpochDay: Long? = null,
    /** Last time you actually used it — drives the "untouched for months" check. */
    val lastUsedEpochDay: Long? = null,
    /** What one unit cost, in minor units. */
    val unitCostMinor: Long? = null,
    /** The expense this was bought with, when it was logged from a record. */
    val transactionId: Long? = null,
    val status: ItemStatus = ItemStatus.IN_STOCK,
    /** When [status] last changed, so the history list can be ordered. */
    val statusChangedEpochDay: Long? = null,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
) {
    /** Quantity times unit cost, rounded to the nearest minor unit. */
    val totalValueMinor: Long
        get() = unitCostMinor?.let { Math.round(it * quantity) } ?: 0L

    companion object {
        /** The normalisation used for [nameKey]; duplicate detection depends on it. */
        fun keyOf(name: String): String = name.trim().lowercase()
    }
}
