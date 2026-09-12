package com.zive.moneymanager.ui.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zive.moneymanager.data.db.entity.Account
import com.zive.moneymanager.data.db.entity.AccountType
import com.zive.moneymanager.data.db.model.AccountBalance
import com.zive.moneymanager.data.prefs.AppSettings
import com.zive.moneymanager.data.prefs.SettingsStore
import com.zive.moneymanager.data.repo.FinanceRepository
import com.zive.moneymanager.di.AppContainer
import com.zive.moneymanager.di.containerFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AccountsUiState(
    val settings: AppSettings = AppSettings(),
    val balances: List<AccountBalance> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val loading: Boolean = true,
) {
    val netWorthMinor: Long
        get() = balances.filter { it.includeInTotal && !it.archived }.sumOf { it.balanceMinor }

    val assetsMinor: Long
        get() = balances.filter { it.includeInTotal && !it.archived && it.balanceMinor > 0 }
            .sumOf { it.balanceMinor }

    val liabilitiesMinor: Long
        get() = balances.filter { it.includeInTotal && !it.archived && it.balanceMinor < 0 }
            .sumOf { -it.balanceMinor }
}

class AccountsViewModel(
    private val repo: FinanceRepository,
    settingsStore: SettingsStore,
) : ViewModel() {

    val state = combine(
        repo.observeAccountBalances(),
        repo.observeAccounts(),
        settingsStore.settings,
    ) { balances, accounts, settings ->
        AccountsUiState(settings, balances, accounts, loading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AccountsUiState())

    fun save(
        id: Long,
        name: String,
        type: AccountType,
        openingBalanceMinor: Long,
        colorHex: String,
        iconKey: String,
        includeInTotal: Boolean,
        archived: Boolean,
    ) {
        viewModelScope.launch {
            val existing = if (id != 0L) repo.account(id) else null
            repo.upsertAccount(
                (existing ?: Account(name = name)).copy(
                    id = id,
                    name = name.trim(),
                    type = type,
                    openingBalanceMinor = openingBalanceMinor,
                    colorHex = colorHex,
                    iconKey = iconKey,
                    includeInTotal = includeInTotal,
                    archived = archived,
                )
            )
        }
    }

    /** Deleting an account also removes its transactions, by foreign key cascade. */
    fun delete(id: Long) {
        viewModelScope.launch {
            repo.account(id)?.let { repo.deleteAccount(it) }
        }
    }

    companion object {
        val Factory = containerFactory { c: AppContainer ->
            AccountsViewModel(c.repository, c.settings)
        }
    }
}
