package com.zive.moneymanager.ui.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zive.moneymanager.data.db.entity.Account
import com.zive.moneymanager.data.db.entity.Category
import com.zive.moneymanager.data.db.entity.RecurrenceUnit
import com.zive.moneymanager.data.db.entity.RecurringRule
import com.zive.moneymanager.data.db.entity.TxType
import com.zive.moneymanager.data.prefs.AppSettings
import com.zive.moneymanager.data.prefs.SettingsStore
import com.zive.moneymanager.data.recurring.RecurringEngine
import com.zive.moneymanager.data.repo.FinanceRepository
import com.zive.moneymanager.di.AppContainer
import com.zive.moneymanager.di.containerFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class RecurringUiState(
    val rules: List<RecurringRule> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val loading: Boolean = true,
) {
    fun accountName(id: Long?): String =
        accounts.firstOrNull { it.id == id }?.name ?: "—"

    fun category(id: Long?): Category? = categories.firstOrNull { it.id == id }
}

class RecurringViewModel(
    private val repo: FinanceRepository,
    private val engine: RecurringEngine,
    settingsStore: SettingsStore,
) : ViewModel() {

    val state = combine(
        repo.observeRecurring(),
        repo.observeActiveAccounts(),
        repo.observeCategories(),
        settingsStore.settings,
    ) { rules, accounts, categories, settings ->
        RecurringUiState(rules, accounts, categories, settings, loading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecurringUiState())

    @Suppress("LongParameterList")
    fun save(
        id: Long,
        name: String,
        type: TxType,
        amountMinor: Long,
        accountId: Long,
        toAccountId: Long?,
        categoryId: Long?,
        note: String,
        counterparty: String,
        unit: RecurrenceUnit,
        interval: Int,
        startDate: LocalDate,
        endDate: LocalDate?,
        enabled: Boolean,
    ) {
        viewModelScope.launch {
            val existing = if (id != 0L) repo.recurringRule(id) else null
            // Editing the schedule of a live rule restarts it from the new start
            // date; otherwise the existing cursor is preserved.
            val nextRun = when {
                existing == null -> startDate.toEpochDay()
                existing.startEpochDay != startDate.toEpochDay() ||
                    existing.unit != unit ||
                    existing.interval != interval -> startDate.toEpochDay()
                else -> existing.nextRunEpochDay
            }

            repo.upsertRecurring(
                RecurringRule(
                    id = id,
                    name = name.trim().ifEmpty { "Scheduled payment" },
                    type = type,
                    amountMinor = amountMinor,
                    accountId = accountId,
                    toAccountId = if (type == TxType.TRANSFER) toAccountId else null,
                    categoryId = if (type == TxType.TRANSFER) null else categoryId,
                    note = note.trim(),
                    counterparty = counterparty.trim(),
                    unit = unit,
                    interval = interval.coerceAtLeast(1),
                    startEpochDay = startDate.toEpochDay(),
                    endEpochDay = endDate?.toEpochDay(),
                    nextRunEpochDay = nextRun,
                    enabled = enabled,
                )
            )
            // Post immediately if the new schedule is already overdue.
            engine.postDue()
        }
    }

    fun setEnabled(rule: RecurringRule, enabled: Boolean) {
        viewModelScope.launch {
            repo.upsertRecurring(rule.copy(enabled = enabled))
            if (enabled) engine.postDue()
        }
    }

    fun delete(rule: RecurringRule) {
        viewModelScope.launch { repo.deleteRecurring(rule) }
    }

    companion object {
        val Factory = containerFactory { c: AppContainer ->
            RecurringViewModel(c.repository, c.recurringEngine, c.settings)
        }
    }
}
