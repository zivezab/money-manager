package com.zive.moneymanager.ui.budgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zive.moneymanager.data.db.entity.Budget
import com.zive.moneymanager.data.db.entity.BudgetPeriod
import com.zive.moneymanager.data.db.entity.Category
import com.zive.moneymanager.data.db.entity.TxType
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
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class BudgetsUiState(
    val settings: AppSettings = AppSettings(),
    val progress: List<BudgetProgress> = emptyList(),
    val expenseCategories: List<Category> = emptyList(),
    val loading: Boolean = true,
) {
    val totalBudgetedMinor: Long get() = progress.sumOf { it.budget.amountMinor }
    val totalSpentMinor: Long get() = progress.sumOf { it.spentMinor }
    val overspentCount: Int get() = progress.count { it.overspent }
}

@OptIn(ExperimentalCoroutinesApi::class)
class BudgetsViewModel(
    private val repo: FinanceRepository,
    private val settingsStore: SettingsStore,
) : ViewModel() {

    val state = combine(
        repo.observeBudgets(),
        repo.observeCategories(),
        settingsStore.settings,
        repo.observeLedgerRevision(),
    ) { budgets, categories, settings, _ ->
        Triple(budgets, categories, settings)
    }.mapLatest { (budgets, categories, settings) ->
        val byId = categories.associateBy { it.id }
        val today = LocalDate.now()

        BudgetsUiState(
            settings = settings,
            progress = budgets.map { budget ->
                val period = budget.periodContaining(today, settings.periodConfig)
                val category = budget.categoryId?.let { byId[it] }
                BudgetProgress(
                    budget = budget,
                    categoryName = category?.name ?: "Total spending",
                    colorHex = category?.colorHex ?: "#64748B",
                    iconKey = category?.iconKey ?: "piggy",
                    period = period,
                    spentMinor = repo.spent(budget.categoryId, period),
                )
            }.sortedWith(compareByDescending<BudgetProgress> { it.budget.active }.thenByDescending { it.fraction }),
            expenseCategories = categories.filter { it.type == TxType.EXPENSE && !it.archived && it.parentId == null },
            loading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BudgetsUiState())

    fun save(id: Long, categoryId: Long?, amountMinor: Long, period: BudgetPeriod, active: Boolean) {
        viewModelScope.launch {
            repo.upsertBudget(
                Budget(
                    id = id,
                    categoryId = categoryId,
                    amountMinor = amountMinor,
                    period = period,
                    active = active,
                )
            )
        }
    }

    fun delete(budget: Budget) {
        viewModelScope.launch { repo.deleteBudget(budget) }
    }

    companion object {
        val Factory = containerFactory { c: AppContainer ->
            BudgetsViewModel(c.repository, c.settings)
        }
    }
}
