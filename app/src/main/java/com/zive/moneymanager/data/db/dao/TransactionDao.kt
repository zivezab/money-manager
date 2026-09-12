package com.zive.moneymanager.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.zive.moneymanager.data.db.entity.TransactionEntity
import com.zive.moneymanager.data.db.entity.TxType
import com.zive.moneymanager.data.db.model.CategoryTotal
import com.zive.moneymanager.data.db.model.DayTotal
import com.zive.moneymanager.data.db.model.IncomeKindTotal
import com.zive.moneymanager.data.db.model.PeriodTotals
import com.zive.moneymanager.data.db.model.TransactionDetail
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun byId(id: Long): TransactionEntity?

    /**
     * The list feed. Every filter is optional — passing null for a parameter
     * leaves that dimension unconstrained.
     */
    @Query(
        """
        SELECT t.id AS id, t.type AS type, t.amountMinor AS amountMinor,
               t.dateEpochDay AS dateEpochDay, t.minuteOfDay AS minuteOfDay,
               t.note AS note, t.counterparty AS counterparty,
               t.accountId AS accountId, a.name AS accountName, a.colorHex AS accountColor,
               t.toAccountId AS toAccountId, ta.name AS toAccountName,
               t.categoryId AS categoryId, c.name AS categoryName,
               c.colorHex AS categoryColor, c.iconKey AS categoryIcon,
               t.recurringRuleId AS recurringRuleId
        FROM transactions t
        JOIN accounts a ON a.id = t.accountId
        LEFT JOIN accounts ta ON ta.id = t.toAccountId
        LEFT JOIN categories c ON c.id = t.categoryId
        WHERE t.dateEpochDay BETWEEN :start AND :end
          AND (:type IS NULL OR t.type = :type)
          AND (:accountId IS NULL OR t.accountId = :accountId OR t.toAccountId = :accountId)
          AND (:categoryId IS NULL OR t.categoryId = :categoryId OR c.parentId = :categoryId)
          AND (:query IS NULL
               OR t.note LIKE '%' || :query || '%'
               OR t.counterparty LIKE '%' || :query || '%'
               OR c.name LIKE '%' || :query || '%'
               OR a.name LIKE '%' || :query || '%')
        ORDER BY t.dateEpochDay DESC, t.minuteOfDay DESC, t.id DESC
        LIMIT :limit
        """
    )
    fun observeDetails(
        start: Long,
        end: Long,
        type: TxType? = null,
        accountId: Long? = null,
        categoryId: Long? = null,
        query: String? = null,
        limit: Int = 2000,
    ): Flow<List<TransactionDetail>>

    /** Headline income / expense / count for a date range. Transfers are excluded:
     *  moving your own money is neither earning nor spending. */
    @Query(
        """
        SELECT IFNULL(SUM(CASE WHEN type = 'INCOME'  THEN amountMinor ELSE 0 END), 0) AS incomeMinor,
               IFNULL(SUM(CASE WHEN type = 'EXPENSE' THEN amountMinor ELSE 0 END), 0) AS expenseMinor,
               COUNT(*) AS txCount
        FROM transactions
        WHERE dateEpochDay BETWEEN :start AND :end AND type != 'TRANSFER'
        """
    )
    fun observeTotals(start: Long, end: Long): Flow<PeriodTotals>

    @Query(
        """
        SELECT IFNULL(SUM(CASE WHEN type = 'INCOME'  THEN amountMinor ELSE 0 END), 0) AS incomeMinor,
               IFNULL(SUM(CASE WHEN type = 'EXPENSE' THEN amountMinor ELSE 0 END), 0) AS expenseMinor,
               COUNT(*) AS txCount
        FROM transactions
        WHERE dateEpochDay BETWEEN :start AND :end AND type != 'TRANSFER'
        """
    )
    suspend fun totalsOnce(start: Long, end: Long): PeriodTotals

    /**
     * Category rollup for the donut and ranking list. Subcategory amounts are
     * folded into their parent so a "Travel" slice includes "Flights".
     */
    @Query(
        """
        SELECT COALESCE(p.id, c.id) AS categoryId,
               COALESCE(p.name, c.name) AS categoryName,
               COALESCE(p.colorHex, c.colorHex) AS colorHex,
               COALESCE(p.iconKey, c.iconKey) AS iconKey,
               SUM(t.amountMinor) AS totalMinor,
               COUNT(*) AS txCount
        FROM transactions t
        LEFT JOIN categories c ON c.id = t.categoryId
        LEFT JOIN categories p ON p.id = c.parentId
        WHERE t.type = :type AND t.dateEpochDay BETWEEN :start AND :end
        GROUP BY COALESCE(p.id, c.id)
        ORDER BY totalMinor DESC
        """
    )
    fun observeCategoryTotals(type: TxType, start: Long, end: Long): Flow<List<CategoryTotal>>

    /** Leaf-level breakdown used when drilling into a single parent category. */
    @Query(
        """
        SELECT c.id AS categoryId, c.name AS categoryName, c.colorHex AS colorHex,
               c.iconKey AS iconKey, SUM(t.amountMinor) AS totalMinor, COUNT(*) AS txCount
        FROM transactions t
        JOIN categories c ON c.id = t.categoryId
        WHERE t.type = :type AND t.dateEpochDay BETWEEN :start AND :end
          AND (c.id = :parentId OR c.parentId = :parentId)
        GROUP BY c.id
        ORDER BY totalMinor DESC
        """
    )
    fun observeSubcategoryTotals(type: TxType, parentId: Long, start: Long, end: Long): Flow<List<CategoryTotal>>

    @Query(
        """
        SELECT dateEpochDay AS dateEpochDay,
               IFNULL(SUM(CASE WHEN type = 'INCOME'  THEN amountMinor ELSE 0 END), 0) AS incomeMinor,
               IFNULL(SUM(CASE WHEN type = 'EXPENSE' THEN amountMinor ELSE 0 END), 0) AS expenseMinor
        FROM transactions
        WHERE dateEpochDay BETWEEN :start AND :end AND type != 'TRANSFER'
        GROUP BY dateEpochDay
        ORDER BY dateEpochDay ASC
        """
    )
    fun observeDayTotals(start: Long, end: Long): Flow<List<DayTotal>>

    /** Employment income vs everything else, for the job / jobless split. */
    @Query(
        """
        SELECT c.incomeKind AS incomeKind, SUM(t.amountMinor) AS totalMinor
        FROM transactions t
        LEFT JOIN categories c ON c.id = t.categoryId
        WHERE t.type = 'INCOME' AND t.dateEpochDay BETWEEN :start AND :end
        GROUP BY c.incomeKind
        """
    )
    fun observeIncomeKindTotals(start: Long, end: Long): Flow<List<IncomeKindTotal>>

    /** Spend counted against one budget. A null [categoryId] totals all expenses. */
    @Query(
        """
        SELECT IFNULL(SUM(t.amountMinor), 0)
        FROM transactions t
        LEFT JOIN categories c ON c.id = t.categoryId
        WHERE t.type = 'EXPENSE' AND t.dateEpochDay BETWEEN :start AND :end
          AND (:categoryId IS NULL OR t.categoryId = :categoryId OR c.parentId = :categoryId)
        """
    )
    suspend fun spentInRange(categoryId: Long?, start: Long, end: Long): Long

    @Query("SELECT MIN(dateEpochDay) FROM transactions")
    fun observeEarliestDay(): Flow<Long?>

    @Query("SELECT COUNT(*) FROM transactions WHERE recurringRuleId = :ruleId")
    suspend fun countForRule(ruleId: Long): Int

    @Query("SELECT * FROM transactions ORDER BY dateEpochDay ASC, minuteOfDay ASC, id ASC")
    suspend fun allForExport(): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tx: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(txs: List<TransactionEntity>): List<Long>

    @Update
    suspend fun update(tx: TransactionEntity)

    @Delete
    suspend fun delete(tx: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()
}
