package com.zive.moneymanager.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zive.moneymanager.core.Period
import com.zive.moneymanager.core.PeriodConfig
import com.zive.moneymanager.core.PeriodController
import com.zive.moneymanager.core.PeriodType
import com.zive.moneymanager.data.db.entity.Account
import com.zive.moneymanager.data.db.entity.TxType
import com.zive.moneymanager.data.db.model.PeriodTotals
import com.zive.moneymanager.data.db.model.TransactionDetail
import com.zive.moneymanager.data.prefs.AppSettings
import com.zive.moneymanager.data.prefs.SettingsStore
import com.zive.moneymanager.data.repo.FinanceRepository
import com.zive.moneymanager.di.AppContainer
import com.zive.moneymanager.di.containerFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class TxFilter(
    val type: TxType? = null,
    val accountId: Long? = null,
    val categoryId: Long? = null,
    val query: String = "",
) {
    val isActive: Boolean get() = type != null || accountId != null || categoryId != null || query.isNotBlank()
}

/** Transactions for one calendar day, with that day's net. */
data class DayGroup(
    val date: LocalDate,
    val items: List<TransactionDetail>,
) {
    val incomeMinor: Long = items.filter { it.type == TxType.INCOME }.sumOf { it.amountMinor }
    val expenseMinor: Long = items.filter { it.type == TxType.EXPENSE }.sumOf { it.amountMinor }
}

data class TransactionsUiState(
    val period: Period = Period.of(PeriodType.MONTH, LocalDate.now(), PeriodConfig()),
    val settings: AppSettings = AppSettings(),
    val filter: TxFilter = TxFilter(),
    val groups: List<DayGroup> = emptyList(),
    val totals: PeriodTotals = PeriodTotals(),
    val accounts: List<Account> = emptyList(),
    val loading: Boolean = true,
)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class TransactionsViewModel(
    private val repo: FinanceRepository,
    settingsStore: SettingsStore,
    val periods: PeriodController,
) : ViewModel() {

    private val filter = MutableStateFlow(TxFilter())

    private data class Context(val period: Period, val settings: AppSettings, val filter: TxFilter)

    private val context = combine(
        settingsStore.settings,
        periods.selection,
        filter.debounce { if (it.query.isBlank()) 0L else 220L },
    ) { settings, selection, activeFilter ->
        Context(periods.periodFor(selection, settings.periodConfig), settings, activeFilter)
    }

    val state = context.flatMapLatest { ctx ->
        combine(
            repo.observeTransactions(
                period = ctx.period,
                type = ctx.filter.type,
                accountId = ctx.filter.accountId,
                categoryId = ctx.filter.categoryId,
                query = ctx.filter.query,
            ),
            repo.observeTotals(ctx.period),
            repo.observeActiveAccounts(),
        ) { details, totals, accounts ->
            TransactionsUiState(
                period = ctx.period,
                settings = ctx.settings,
                filter = ctx.filter,
                groups = details.groupBy { it.dateEpochDay }
                    .toSortedMap(compareByDescending { it })
                    .map { (day, items) -> DayGroup(LocalDate.ofEpochDay(day), items) },
                totals = totals,
                accounts = accounts,
                loading = false,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TransactionsUiState())

    fun setType(type: TxType?) = filter.update { it.copy(type = type) }
    fun setAccount(accountId: Long?) = filter.update { it.copy(accountId = accountId) }
    fun setQuery(query: String) = filter.update { it.copy(query = query) }
    fun clearFilters() = filter.update { TxFilter() }

    fun delete(id: Long) {
        viewModelScope.launch { repo.deleteTransaction(id) }
    }

    companion object {
        val Factory = containerFactory { c: AppContainer ->
            TransactionsViewModel(c.repository, c.settings, c.periodController)
        }
    }
}
