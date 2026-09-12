package com.zive.moneymanager.data.db.model

import androidx.room.ColumnInfo
import com.zive.moneymanager.data.db.entity.AccountType
import com.zive.moneymanager.data.db.entity.IncomeKind
import com.zive.moneymanager.data.db.entity.TxType

/** A transaction joined with the display fields every list row needs. */
data class TransactionDetail(
    val id: Long,
    val type: TxType,
    val amountMinor: Long,
    val dateEpochDay: Long,
    val minuteOfDay: Int,
    val note: String,
    val counterparty: String,
    val accountId: Long,
    val accountName: String,
    val accountColor: String,
    val toAccountId: Long?,
    val toAccountName: String?,
    val categoryId: Long?,
    val categoryName: String?,
    val categoryColor: String?,
    val categoryIcon: String?,
    val recurringRuleId: Long?,
)

/** Spend or income rolled up to one category, used by the donut and ranking list. */
data class CategoryTotal(
    val categoryId: Long?,
    val categoryName: String?,
    val colorHex: String?,
    val iconKey: String?,
    val totalMinor: Long,
    val txCount: Int,
)

/** Income and expense totals for a single calendar day. */
data class DayTotal(
    val dateEpochDay: Long,
    val incomeMinor: Long,
    val expenseMinor: Long,
)

/** Income split by whether it came from employment. */
data class IncomeKindTotal(
    val incomeKind: IncomeKind?,
    val totalMinor: Long,
)

/** An account with its derived running balance. */
data class AccountBalance(
    val accountId: Long,
    val name: String,
    val type: AccountType,
    val colorHex: String,
    val iconKey: String,
    val archived: Boolean,
    val includeInTotal: Boolean,
    val balanceMinor: Long,
)

/** Totals for a period — the headline numbers on the overview. */
data class PeriodTotals(
    @ColumnInfo(name = "incomeMinor") val incomeMinor: Long = 0,
    @ColumnInfo(name = "expenseMinor") val expenseMinor: Long = 0,
    @ColumnInfo(name = "txCount") val txCount: Int = 0,
) {
    val netMinor: Long get() = incomeMinor - expenseMinor
}
