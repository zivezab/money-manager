package com.zive.moneymanager.data.db.entity

/** Which side of the ledger a category or transaction sits on. */
enum class TxType { INCOME, EXPENSE, TRANSFER }

/** Income is split into job-derived and everything else, so the overview can
 *  answer "how much of this came from employment?" for job / jobless periods. */
enum class IncomeKind { JOB, NON_JOB, NONE }

enum class AccountType(val label: String) {
    CASH("Cash"),
    BANK("Bank account"),
    CARD("Credit card"),
    SAVINGS("Savings"),
    INVESTMENT("Investment"),
    OTHER("Other"),
}

enum class RecurrenceUnit(val label: String) {
    DAY("day"),
    WEEK("week"),
    MONTH("month"),
    QUARTER("quarter"),
    YEAR("year"),
}

enum class BudgetPeriod(val label: String) {
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    QUARTERLY("Quarterly"),
    YEARLY("Yearly"),
}
