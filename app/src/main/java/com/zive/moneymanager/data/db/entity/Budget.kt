package com.zive.moneymanager.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A spending cap. A null [categoryId] means the budget covers total expenses for
 * the period rather than a single category.
 */
@Entity(tableName = "budgets", indices = [Index("categoryId")])
data class Budget(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long? = null,
    val period: BudgetPeriod = BudgetPeriod.MONTHLY,
    val amountMinor: Long,
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
)
