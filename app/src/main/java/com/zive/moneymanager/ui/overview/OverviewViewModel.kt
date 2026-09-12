package com.zive.moneymanager.ui.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zive.moneymanager.core.Period
import com.zive.moneymanager.core.PeriodController
import com.zive.moneymanager.core.PeriodType
import com.zive.moneymanager.data.db.entity.IncomeKind
import com.zive.moneymanager.data.db.entity.TxType
import com.zive.moneymanager.data.db.model.AccountBalance
import com.zive.moneymanager.data.db.model.CategoryTotal
import com.zive.moneymanager.data.db.model.PeriodTotals
import com.zive.moneymanager.data.db.model.TransactionDetail
import com.zive.moneymanager.data.prefs.AppSettings
import com.zive.moneymanager.data.prefs.SettingsStore
import com.zive.moneymanager.data.repo.BudgetProgress
import com.zive.moneymanager.data.repo.FinanceRepository
import com.zive.moneymanager.data.repo.periodContaining
import com.zive.moneymanager.di.AppContainer
import com.zive.moneymanager.di.containerFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class IncomeSplit(
    val jobMinor: Long = 0,
    val nonJobMinor: Long = 0,
    val unclassifiedMinor: Long = 0,
) {
    val totalMinor: Long get() = jobMinor + nonJobMinor + unclassifiedMinor
    val jobFraction: Float get() = if (totalMinor <= 0) 0f else jobMinor.toFloat() / totalMinor
}

data class OverviewUiState(
    val period: Period = Period.of(PeriodType.MONTH, LocalDate.now(), com.zive.moneymanager.core.PeriodConfig()),
    val settings: AppSettings = AppSettings(),
    val totals: PeriodTotals = PeriodTotals(),
    val incomeSplit: IncomeSplit = IncomeSplit(),
    val topExpenseCategories: List<CategoryTotal> = emptyList(),
    val accounts: List<AccountBalance> = emptyList(),
    val recent: List<TransactionDetail> = emptyList(),
    val budgets: List<BudgetProgress> = emptyList(),
    val loading: Boolean = true,
) {
    val netWorthMinor: Long get() = accounts.filter { it.includeInTotal && !it.archived }.sumOf { it.balanceMinor }
}

@OptIn(ExperimentalCoroutinesApi::class)
class OverviewViewModel(
    private val repo: FinanceRepository,
    private val settingsStore: SettingsStore,
    val periods: PeriodController,
) : ViewModel() {

    private data class Context(val period: Period, val settings: AppSettings, val anchor: LocalDate)

    private val context = combine(settingsStore.settings, periods.selection) { settings, selection ->
        periods.applyDefaultOnce(settings.defaultPeriod)
        Context(
            period = periods.periodFor(selection, settings.periodConfig),
            settings = settings,
            anchor = selection.anchor,
        )
    }

    private val ledger = context.flatMapLatest { ctx ->
        combine(
            repo.observeTotals(ctx.period),
            repo.observeIncomeKindTotals(ctx.period),
            repo.observeCategoryTotals(TxType.EXPENSE, ctx.period),
            repo.observeAccountBalances(),
            repo.observeTransactions(ctx.period, limit = 8),
        ) { totals, kinds, categories, accounts, recent ->
            val split = IncomeSplit(
                jobMinor = kinds.firstOrNull { it.incomeKind == IncomeKind.JOB }?.totalMinor ?: 0L,
                nonJobMinor = kinds.firstOrNull { it.incomeKind == IncomeKind.NON_JOB }?.totalMinor ?: 0L,
                unclassifiedMinor = kinds.filter { it.incomeKind == null || it.incomeKind == IncomeKind.NONE }
                    .sumOf { it.totalMinor },
            )
            LedgerSnapshot(totals, split, categories.take(5), accounts, recent)
        }
    }

    private data class LedgerSnapshot(
        val totals: PeriodTotals,
        val split: IncomeSplit,
        val topCategories: List<CategoryTotal>,
        val accounts: List<AccountBalance>,
        val recent: List<TransactionDetail>,
    )

    /**
     * Budget spend needs a suspend query per budget because each budget runs on
     * its own cycle, so it is recomputed whenever the ledger changes rather than
     * observed directly.
     */
    private val budgets = context.flatMapLatest { ctx ->
        combine(
            repo.observeActiveBudgets(),
            repo.observeCategories(),
            repo.observeLedgerRevision(),
        ) { budgets, categories, _ -> budgets to categories }
            .mapLatest { (budgetList, categories) ->
                val byId = categories.associateBy { it.id }
                budgetList.map { budget ->
                    val budgetPeriod = budget.periodContaining(ctx.anchor, ctx.settings.periodConfig)
                    val category = budget.categoryId?.let { byId[it] }
                    BudgetProgress(
                        budget = budget,
                        categoryName = category?.name ?: "Total spending",
                        colorHex = category?.colorHex ?: "#64748B",
                        iconKey = category?.iconKey ?: "piggy",
                        period = budgetPeriod,
                        spentMinor = repo.spent(budget.categoryId, budgetPeriod),
                    )
                }.sortedByDescending { it.fraction }
            }
    }

    val state = combine(context, ledger, budgets) { ctx, snapshot, budgetProgress ->
        OverviewUiState(
            period = ctx.period,
            settings = ctx.settings,
            totals = snapshot.totals,
            incomeSplit = snapshot.split,
            topExpenseCategories = snapshot.topCategories,
            accounts = snapshot.accounts,
            recent = snapshot.recent,
            budgets = budgetProgress,
            loading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OverviewUiState())

    /** Widest range with data, so the "All" period does not scan to year 2999. */
    val dataStart = repo.observeEarliestDay()
        .map { it?.let { day -> LocalDate.ofEpochDay(day) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    companion object {
        val Factory = containerFactory { c: AppContainer ->
            OverviewViewModel(c.repository, c.settings, c.periodController)
        }
    }
}
