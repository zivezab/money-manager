package com.zive.moneymanager.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.zive.moneymanager.data.db.entity.InventoryItem
import com.zive.moneymanager.data.db.entity.ItemKind
import com.zive.moneymanager.data.db.entity.ItemStatus
import com.zive.moneymanager.data.db.model.DuplicateGroup
import com.zive.moneymanager.data.db.model.InventoryItemDetail
import com.zive.moneymanager.data.db.model.InventorySummary
import kotlinx.coroutines.flow.Flow

/**
 * Everything the inventory screens read. "Held" throughout means the thing is
 * still in your home: in stock, or queued for donation or the bin.
 */
@Dao
interface InventoryDao {

    @Query("SELECT * FROM inventory_items WHERE id = :id")
    suspend fun byId(id: Long): InventoryItem?

    /**
     * The browse list. Every filter is optional; passing null leaves that
     * dimension unconstrained.
     */
    @Query(
        """
        SELECT i.*, c.name AS categoryName, c.colorHex AS categoryColor, c.iconKey AS categoryIcon
        FROM inventory_items i
        LEFT JOIN categories c ON c.id = i.categoryId
        WHERE (:heldOnly = 0 OR i.status IN ('IN_STOCK', 'TO_DONATE', 'TO_DISCARD'))
          AND (:kind IS NULL OR i.kind = :kind)
          AND (:status IS NULL OR i.status = :status)
          AND (:categoryId IS NULL OR i.categoryId = :categoryId)
          AND (:location IS NULL OR i.location = :location)
          AND (:query IS NULL
               OR i.name LIKE '%' || :query || '%'
               OR i.note LIKE '%' || :query || '%'
               OR i.location LIKE '%' || :query || '%'
               OR c.name LIKE '%' || :query || '%')
        ORDER BY
          CASE WHEN i.expiryEpochDay IS NULL THEN 1 ELSE 0 END,
          i.expiryEpochDay ASC,
          i.name COLLATE NOCASE ASC
        """
    )
    fun observeItems(
        heldOnly: Boolean = true,
        kind: ItemKind? = null,
        status: ItemStatus? = null,
        categoryId: Long? = null,
        location: String? = null,
        query: String? = null,
    ): Flow<List<InventoryItemDetail>>

    /**
     * The expiry checklist: everything still held that has a date on or before
     * [horizonEpochDay], soonest first. Items already past their date come first
     * because their [expiryEpochDay] is smallest.
     */
    @Query(
        """
        SELECT i.*, c.name AS categoryName, c.colorHex AS categoryColor, c.iconKey AS categoryIcon
        FROM inventory_items i
        LEFT JOIN categories c ON c.id = i.categoryId
        WHERE i.status IN ('IN_STOCK', 'TO_DONATE', 'TO_DISCARD')
          AND i.expiryEpochDay IS NOT NULL
          AND i.expiryEpochDay <= :horizonEpochDay
        ORDER BY i.expiryEpochDay ASC, i.name COLLATE NOCASE ASC
        """
    )
    fun observeExpiringBy(horizonEpochDay: Long): Flow<List<InventoryItemDetail>>

    /** Same as [observeExpiringBy] but a one-shot read, for the reminder worker. */
    @Query(
        """
        SELECT i.*, c.name AS categoryName, c.colorHex AS categoryColor, c.iconKey AS categoryIcon
        FROM inventory_items i
        LEFT JOIN categories c ON c.id = i.categoryId
        WHERE i.status IN ('IN_STOCK', 'TO_DONATE', 'TO_DISCARD')
          AND i.expiryEpochDay IS NOT NULL
          AND i.expiryEpochDay <= :horizonEpochDay
        ORDER BY i.expiryEpochDay ASC, i.name COLLATE NOCASE ASC
        """
    )
    suspend fun expiringByOnce(horizonEpochDay: Long): List<InventoryItemDetail>

    /** Items queued for donation or the bin — the disposal checklist. */
    @Query(
        """
        SELECT i.*, c.name AS categoryName, c.colorHex AS categoryColor, c.iconKey AS categoryIcon
        FROM inventory_items i
        LEFT JOIN categories c ON c.id = i.categoryId
        WHERE i.status IN ('TO_DONATE', 'TO_DISCARD')
        ORDER BY i.status ASC, i.name COLLATE NOCASE ASC
        """
    )
    fun observePendingDisposal(): Flow<List<InventoryItemDetail>>

    /**
     * Names you hold more than one of. Grouping on the stored [InventoryItem.nameKey]
     * keeps this an index scan rather than a full-table lower() over every row.
     */
    @Query(
        """
        SELECT i.nameKey AS nameKey,
               MIN(i.name) AS displayName,
               COUNT(*) AS itemCount,
               SUM(i.quantity) AS totalQuantity,
               IFNULL(SUM(CAST(ROUND(IFNULL(i.unitCostMinor, 0) * i.quantity) AS INTEGER)), 0) AS totalValueMinor,
               GROUP_CONCAT(DISTINCT NULLIF(i.location, '')) AS locations
        FROM inventory_items i
        WHERE i.status IN ('IN_STOCK', 'TO_DONATE', 'TO_DISCARD')
        GROUP BY i.nameKey
        HAVING COUNT(*) > 1
        ORDER BY COUNT(*) DESC, totalValueMinor DESC
        """
    )
    fun observeDuplicateGroups(): Flow<List<DuplicateGroup>>

    /**
     * Assets nobody has touched since [cutoffEpochDay] — last use if it was ever
     * recorded, otherwise the purchase date.
     *
     * Restricted to assets still in stock on purpose: a consumable going stale is
     * the expiry rule's job, and anything already queued for donation or the bin
     * has been decided, so repeating it here would just be noise.
     */
    @Query(
        """
        SELECT i.*, c.name AS categoryName, c.colorHex AS categoryColor, c.iconKey AS categoryIcon
        FROM inventory_items i
        LEFT JOIN categories c ON c.id = i.categoryId
        WHERE i.status = 'IN_STOCK'
          AND i.kind = 'ASSET'
          AND COALESCE(i.lastUsedEpochDay, i.purchaseEpochDay) IS NOT NULL
          AND COALESCE(i.lastUsedEpochDay, i.purchaseEpochDay) <= :cutoffEpochDay
        ORDER BY COALESCE(i.lastUsedEpochDay, i.purchaseEpochDay) ASC
        """
    )
    fun observeUntouchedSince(cutoffEpochDay: Long): Flow<List<InventoryItemDetail>>

    @Query(
        """
        SELECT
          (SELECT COUNT(*) FROM inventory_items
            WHERE status IN ('IN_STOCK', 'TO_DONATE', 'TO_DISCARD')) AS heldCount,
          (SELECT IFNULL(SUM(CAST(ROUND(IFNULL(unitCostMinor, 0) * quantity) AS INTEGER)), 0)
             FROM inventory_items
            WHERE status IN ('IN_STOCK', 'TO_DONATE', 'TO_DISCARD')) AS totalValueMinor,
          (SELECT COUNT(*) FROM inventory_items
            WHERE status IN ('IN_STOCK', 'TO_DONATE', 'TO_DISCARD')
              AND expiryEpochDay IS NOT NULL AND expiryEpochDay < :todayEpochDay) AS expiredCount,
          (SELECT COUNT(*) FROM inventory_items
            WHERE status IN ('IN_STOCK', 'TO_DONATE', 'TO_DISCARD')
              AND expiryEpochDay IS NOT NULL
              AND expiryEpochDay >= :todayEpochDay
              AND expiryEpochDay <= :horizonEpochDay) AS expiringSoonCount,
          (SELECT COUNT(*) FROM inventory_items WHERE status = 'TO_DONATE') AS toDonateCount,
          (SELECT COUNT(*) FROM inventory_items WHERE status = 'TO_DISCARD') AS toDiscardCount
        """
    )
    fun observeSummary(todayEpochDay: Long, horizonEpochDay: Long): Flow<InventorySummary>

    /** Distinct non-empty locations, for the filter row and editor suggestions. */
    @Query(
        """
        SELECT DISTINCT location FROM inventory_items
        WHERE location != '' ORDER BY location COLLATE NOCASE ASC
        """
    )
    fun observeLocations(): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM inventory_items")
    suspend fun count(): Int

    @Query("SELECT * FROM inventory_items ORDER BY name COLLATE NOCASE ASC")
    suspend fun allForExport(): List<InventoryItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: InventoryItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<InventoryItem>): List<Long>

    @Update
    suspend fun update(item: InventoryItem)

    @Delete
    suspend fun delete(item: InventoryItem)

    @Query("DELETE FROM inventory_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query(
        """
        UPDATE inventory_items
        SET status = :status, statusChangedEpochDay = :todayEpochDay, updatedAt = :now
        WHERE id = :id
        """
    )
    suspend fun setStatus(id: Long, status: ItemStatus, todayEpochDay: Long, now: Long)

    @Query("UPDATE inventory_items SET lastUsedEpochDay = :epochDay, updatedAt = :now WHERE id = :id")
    suspend fun markUsedToday(id: Long, epochDay: Long, now: Long)

    @Query("UPDATE inventory_items SET expiryEpochDay = :epochDay, updatedAt = :now WHERE id = :id")
    suspend fun setExpiry(id: Long, epochDay: Long?, now: Long)
}
