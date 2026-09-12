package com.zive.moneymanager.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One ledger line. [amountMinor] is always stored positive; [type] decides the
 * direction so reports never have to guess at a sign convention.
 */
@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = Account::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Account::class,
            parentColumns = ["id"],
            childColumns = ["toAccountId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("accountId"), Index("toAccountId"), Index("categoryId"), Index("dateEpochDay")],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: TxType,
    val amountMinor: Long,
    /** Local calendar day, stored as an epoch day so range queries stay integer comparisons. */
    val dateEpochDay: Long,
    /** Minutes since midnight — keeps same-day ordering stable. */
    val minuteOfDay: Int = 0,
    val accountId: Long,
    /** Destination account for TRANSFER, null otherwise. */
    val toAccountId: Long? = null,
    val categoryId: Long? = null,
    val note: String = "",
    /** Payee for an expense, payer/source for income. */
    val counterparty: String = "",
    /** Set when this row was generated from a [RecurringRule]. */
    val recurringRuleId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
)
