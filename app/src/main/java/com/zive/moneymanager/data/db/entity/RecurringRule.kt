package com.zive.moneymanager.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A template that posts transactions on a schedule — a salary on the 25th, a home
 * loan instalment on the 1st. [nextRunEpochDay] is advanced as occurrences post.
 */
@Entity(tableName = "recurring_rules")
data class RecurringRule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: TxType,
    val amountMinor: Long,
    val accountId: Long,
    val toAccountId: Long? = null,
    val categoryId: Long? = null,
    val note: String = "",
    val counterparty: String = "",
    val unit: RecurrenceUnit = RecurrenceUnit.MONTH,
    /** Every N units — 1 = monthly, 2 = every other month. */
    val interval: Int = 1,
    val startEpochDay: Long,
    val endEpochDay: Long? = null,
    val nextRunEpochDay: Long,
    val enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
)
