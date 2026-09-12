package com.zive.moneymanager.data.db

import androidx.room.TypeConverter
import com.zive.moneymanager.data.db.entity.AccountType
import com.zive.moneymanager.data.db.entity.BudgetPeriod
import com.zive.moneymanager.data.db.entity.IncomeKind
import com.zive.moneymanager.data.db.entity.RecurrenceUnit
import com.zive.moneymanager.data.db.entity.TxType

/**
 * Enums are persisted by name rather than ordinal so reordering a declaration
 * can never silently reinterpret existing rows, and so raw SQL in the DAOs can
 * compare against readable literals like 'INCOME'.
 */
class Converters {
    @TypeConverter fun txTypeToString(v: TxType?): String? = v?.name
    @TypeConverter fun stringToTxType(v: String?): TxType? = v?.let { runCatching { TxType.valueOf(it) }.getOrNull() }

    @TypeConverter fun accountTypeToString(v: AccountType?): String? = v?.name
    @TypeConverter fun stringToAccountType(v: String?): AccountType? = v?.let { runCatching { AccountType.valueOf(it) }.getOrNull() }

    @TypeConverter fun incomeKindToString(v: IncomeKind?): String? = v?.name
    @TypeConverter fun stringToIncomeKind(v: String?): IncomeKind? = v?.let { runCatching { IncomeKind.valueOf(it) }.getOrNull() }

    @TypeConverter fun recurrenceUnitToString(v: RecurrenceUnit?): String? = v?.name
    @TypeConverter fun stringToRecurrenceUnit(v: String?): RecurrenceUnit? = v?.let { runCatching { RecurrenceUnit.valueOf(it) }.getOrNull() }

    @TypeConverter fun budgetPeriodToString(v: BudgetPeriod?): String? = v?.name
    @TypeConverter fun stringToBudgetPeriod(v: String?): BudgetPeriod? = v?.let { runCatching { BudgetPeriod.valueOf(it) }.getOrNull() }
}
