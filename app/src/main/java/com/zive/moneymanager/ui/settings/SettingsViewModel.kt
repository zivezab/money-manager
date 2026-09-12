package com.zive.moneymanager.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zive.moneymanager.core.CurrencyOption
import com.zive.moneymanager.core.Period
import com.zive.moneymanager.core.PeriodType
import com.zive.moneymanager.data.csv.CsvSupport
import com.zive.moneymanager.data.prefs.AppSettings
import com.zive.moneymanager.data.prefs.SettingsStore
import com.zive.moneymanager.data.prefs.ThemeMode
import com.zive.moneymanager.data.reminder.ExpiryReminderWorker
import com.zive.moneymanager.data.repo.FinanceRepository
import com.zive.moneymanager.di.AppContainer
import com.zive.moneymanager.di.containerFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val accountCount: Int = 0,
    val categoryCount: Int = 0,
    val recurringCount: Int = 0,
    val transactionCount: Int = 0,
)

class SettingsViewModel(
    private val repo: FinanceRepository,
    private val store: SettingsStore,
) : ViewModel() {

    private val allTime = Period.of(PeriodType.ALL, LocalDate.now(), com.zive.moneymanager.core.PeriodConfig())

    val state: StateFlow<SettingsUiState> = combine(
        store.settings,
        repo.observeAccounts(),
        repo.observeCategories(),
        repo.observeRecurring(),
        repo.observeTotals(allTime),
    ) { settings, accounts, categories, recurring, totals ->
        SettingsUiState(
            settings = settings,
            accountCount = accounts.size,
            categoryCount = categories.size,
            recurringCount = recurring.size,
            transactionCount = totals.txCount,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun clearMessage() { _message.value = null }

    fun setCurrency(option: CurrencyOption) = viewModelScope.launch { store.setCurrency(option) }
    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { store.setThemeMode(mode) }
    fun setDynamicColor(enabled: Boolean) = viewModelScope.launch { store.setDynamicColor(enabled) }
    fun setWeekStart(day: DayOfWeek) = viewModelScope.launch { store.setWeekStart(day) }
    fun setMonthStartDay(day: Int) = viewModelScope.launch { store.setMonthStartDay(day) }
    fun setDefaultPeriod(type: PeriodType) = viewModelScope.launch { store.setDefaultPeriod(type) }
    fun setExpiryHorizonDays(days: Int) = viewModelScope.launch { store.setExpiryHorizonDays(days) }
    fun setDeclutterIdleMonths(months: Int) = viewModelScope.launch { store.setDeclutterIdleMonths(months) }

    /** Toggling the reminder has to (re)schedule the worker, not just save a flag. */
    fun setExpiryReminderEnabled(context: Context, enabled: Boolean) {
        viewModelScope.launch {
            store.setExpiryReminderEnabled(enabled)
            if (enabled) {
                ExpiryReminderWorker.schedule(context, store.settings.first().expiryReminderHour)
            } else {
                ExpiryReminderWorker.cancel(context)
            }
        }
    }

    fun setExpiryReminderHour(context: Context, hour: Int) {
        viewModelScope.launch {
            store.setExpiryReminderHour(hour)
            if (store.settings.first().expiryReminderEnabled) {
                ExpiryReminderWorker.schedule(context, hour)
            }
        }
    }

    /** Writes the whole ledger as CSV to a location the user picked. */
    fun exportTo(context: Context, uri: Uri) {
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val currency = state.value.settings.currency.code
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        CsvSupport.export(repo, out, currency)
                    } ?: error("Could not open the selected file")
                }
            }
            _message.value = result.fold(
                onSuccess = { "Exported ${state.value.transactionCount} records" },
                onFailure = { "Export failed: ${it.message}" },
            )
        }
    }

    fun importFrom(context: Context, uri: Uri) {
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        CsvSupport.import(repo, input)
                    } ?: error("Could not open the selected file")
                }
            }
            _message.value = result.fold(
                onSuccess = { imported ->
                    buildString {
                        append("Imported ${imported.imported} records")
                        if (imported.skipped > 0) append(", skipped ${imported.skipped}")
                        if (imported.createdAccounts > 0) append(", +${imported.createdAccounts} accounts")
                        if (imported.createdCategories > 0) append(", +${imported.createdCategories} categories")
                    }
                },
                onFailure = { "Import failed: ${it.message}" },
            )
        }
    }

    companion object {
        val Factory = containerFactory { c: AppContainer ->
            SettingsViewModel(c.repository, c.settings)
        }
    }
}
