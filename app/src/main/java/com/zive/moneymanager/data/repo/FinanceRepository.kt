package com.zive.moneymanager.data.repo

import com.zive.moneymanager.core.Period
import com.zive.moneymanager.data.db.AppDatabase
import com.zive.moneymanager.data.db.entity.Account
import com.zive.moneymanager.data.db.entity.Budget
import com.zive.moneymanager.data.db.entity.Category
import com.zive.moneymanager.data.db.entity.RecurringRule
import com.zive.moneymanager.data.db.entity.TransactionEntity
import com.zive.moneymanager.data.db.entity.TxType
import com.zive.moneymanager.data.db.model.AccountBalance
import com.zive.moneymanager.data.db.model.CategoryTotal
import com.zive.moneymanager.data.db.model.DayTotal
import com.zive.moneymanager.data.db.model.IncomeKindTotal
import com.zive.moneymanager.data.db.model.PeriodTotals
import com.zive.moneymanager.data.db.model.TransactionDetail
import com.zive.moneymanager.data.seed.DefaultData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Single entry point onto the database. Screens talk to this rather than to DAOs
 * so query shapes stay in one place.
 */
class FinanceRepository(private val db: AppDatabase) {

    private val accounts = db.accountDao()
    private val categories = db.categoryDao()
    private val transactions = db.transactionDao()
    private val budgets = db.budgetDao()
    private val recurring = db.recurringDao()

    // ---- Accounts ----------------------------------------------------------

    fun observeAccounts(): Flow<List<Account>> = accounts.observeAll()
    fun observeActiveAccounts(): Flow<List<Account>> = accounts.observeActive()
    fun observeAccountBalances(): Flow<List<AccountBalance>> = accounts.observeBalances()
    suspend fun account(id: Long): Account? = accounts.byId(id)
    suspend fun upsertAccount(account: Account): Long =
        if (account.id == 0L) accounts.insert(account) else account.id.also { accounts.update(account) }
    suspend fun deleteAccount(account: Account) = accounts.delete(account)

    // ---- Categories --------------------------------------------------------

    fun observeCategories(): Flow<List<Category>> = categories.observeAll()
    fun observeCategories(type: TxType): Flow<List<Category>> = categories.observeByType(type)
    suspend fun category(id: Long): Category? = categories.byId(id)
    suspend fun upsertCategory(category: Category): Long =
        if (category.id == 0L) categories.insert(category) else category.id.also { categories.update(category) }

    suspend fun deleteCategory(category: Category) {
        categories.detachChildren(category.id)
        categories.delete(category)
    }

    // ---- Transactions ------------------------------------------------------

    fun observeTransactions(
        period: Period,
        type: TxType? = null,
        accountId: Long? = null,
        categoryId: Long? = null,
        query: String? = null,
        limit: Int = 2000,
    ): Flow<List<TransactionDetail>> = transactions.observeDetails(
        start = period.startEpochDay,
        end = period.endEpochDay,
        type = type,
        accountId = accountId,
        categoryId = categoryId,
        query = query?.trim()?.takeIf { it.isNotEmpty() },
        limit = limit,
    )

    /**
     * Emits whenever anything in the transactions table changes. Used to re-run
     * the suspend budget queries, which Room cannot observe directly because each
     * budget needs its own date range.
     */
    fun observeLedgerRevision(): Flow<PeriodTotals> =
        transactions.observeTotals(Long.MIN_VALUE / 4, Long.MAX_VALUE / 4)

    fun observeTotals(period: Period): Flow<PeriodTotals> =
        transactions.observeTotals(period.startEpochDay, period.endEpochDay)

    suspend fun totalsOnce(period: Period): PeriodTotals =
        transactions.totalsOnce(period.startEpochDay, period.endEpochDay)

    fun observeCategoryTotals(type: TxType, period: Period): Flow<List<CategoryTotal>> =
        transactions.observeCategoryTotals(type, period.startEpochDay, period.endEpochDay)

    fun observeSubcategoryTotals(type: TxType, parentId: Long, period: Period): Flow<List<CategoryTotal>> =
        transactions.observeSubcategoryTotals(type, parentId, period.startEpochDay, period.endEpochDay)

    fun observeDayTotals(period: Period): Flow<List<DayTotal>> =
        transactions.observeDayTotals(period.startEpochDay, period.endEpochDay)

    fun observeIncomeKindTotals(period: Period): Flow<List<IncomeKindTotal>> =
        transactions.observeIncomeKindTotals(period.startEpochDay, period.endEpochDay)

    fun observeEarliestDay(): Flow<Long?> = transactions.observeEarliestDay()

    suspend fun transaction(id: Long): TransactionEntity? = transactions.byId(id)

    suspend fun upsertTransaction(tx: TransactionEntity): Long =
        if (tx.id == 0L) transactions.insert(tx) else tx.id.also { transactions.update(tx) }

    suspend fun deleteTransaction(id: Long) = transactions.deleteById(id)

    suspend fun allTransactionsForExport(): List<TransactionEntity> = transactions.allForExport()

    // ---- Budgets -----------------------------------------------------------

    fun observeBudgets(): Flow<List<Budget>> = budgets.observeAll()
    fun observeActiveBudgets(): Flow<List<Budget>> = budgets.observeActive()
    suspend fun budget(id: Long): Budget? = budgets.byId(id)
    suspend fun upsertBudget(budget: Budget): Long =
        if (budget.id == 0L) budgets.insert(budget) else budget.id.also { budgets.update(budget) }
    suspend fun deleteBudget(budget: Budget) = budgets.delete(budget)

    suspend fun spent(categoryId: Long?, period: Period): Long =
        transactions.spentInRange(categoryId, period.startEpochDay, period.endEpochDay)

    // ---- Recurring ---------------------------------------------------------

    fun observeRecurring(): Flow<List<RecurringRule>> = recurring.observeAll()
    suspend fun recurringRule(id: Long): RecurringRule? = recurring.byId(id)
    suspend fun recurringDue(today: Long): List<RecurringRule> = recurring.dueOn(today)
    suspend fun upsertRecurring(rule: RecurringRule): Long =
        if (rule.id == 0L) recurring.insert(rule) else rule.id.also { recurring.update(rule) }
    suspend fun deleteRecurring(rule: RecurringRule) = recurring.delete(rule)
    suspend fun postedCountForRule(id: Long): Int = transactions.countForRule(id)

    suspend fun postTransactions(rows: List<TransactionEntity>) {
        if (rows.isNotEmpty()) transactions.insertAll(rows)
    }

    // ---- First run ---------------------------------------------------------

    /** Inserts the starter accounts and category tree. Safe to call repeatedly —
     *  it bails out as soon as it sees existing rows. */
    suspend fun seedIfEmpty(): Boolean {
        if (accounts.count() > 0 || categories.count() > 0) return false

        accounts.insertAll(DefaultData.accounts)

        DefaultData.parentCategories().forEach { (seed, parent) ->
            val parentId = categories.insert(parent)
            if (seed.children.isNotEmpty()) {
                categories.insertAll(
                    seed.children.mapIndexed { i, childName ->
                        Category(
                            name = childName,
                            type = parent.type,
                            parentId = parentId,
                            colorHex = parent.colorHex,
                            iconKey = parent.iconKey,
                            incomeKind = parent.incomeKind,
                            sortOrder = i,
                        )
                    }
                )
            }
        }
        return true
    }

    /** Convenience for the overview's "top spending category" line. */
    fun observeTopExpenseCategory(period: Period): Flow<CategoryTotal?> =
        observeCategoryTotals(TxType.EXPENSE, period).map { it.firstOrNull() }
}
