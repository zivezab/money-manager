package com.zive.moneymanager.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zive.moneymanager.core.Money
import com.zive.moneymanager.data.db.entity.Account
import com.zive.moneymanager.data.db.entity.Category
import com.zive.moneymanager.data.db.entity.TransactionEntity
import com.zive.moneymanager.data.db.entity.TxType
import com.zive.moneymanager.data.prefs.AppSettings
import com.zive.moneymanager.data.prefs.SettingsStore
import com.zive.moneymanager.data.repo.FinanceRepository
import com.zive.moneymanager.di.AppContainer
import com.zive.moneymanager.di.containerFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

data class TxEditorState(
    val txId: Long = 0,
    val type: TxType = TxType.EXPENSE,
    /** Raw keypad buffer, e.g. "12.5". Empty means nothing typed yet. */
    val amountInput: String = "",
    val date: LocalDate = LocalDate.now(),
    val minuteOfDay: Int = LocalTime.now().hour * 60 + LocalTime.now().minute,
    val accountId: Long? = null,
    val toAccountId: Long? = null,
    val categoryId: Long? = null,
    val note: String = "",
    val counterparty: String = "",
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val saving: Boolean = false,
    val loaded: Boolean = false,
    val error: String? = null,
) {
    val isNew: Boolean get() = txId == 0L
    val amountMinor: Long get() = Money.parse(amountInput) ?: 0L
    val displayAmount: String get() = if (amountInput.isEmpty()) "0" else amountInput

    val selectedAccount: Account? get() = accounts.firstOrNull { it.id == accountId }
    val selectedToAccount: Account? get() = accounts.firstOrNull { it.id == toAccountId }
    val selectedCategory: Category? get() = categories.firstOrNull { it.id == categoryId }

    /** Categories for the current type, parents first with their children attached. */
    val categoryTree: List<Pair<Category, List<Category>>>
        get() {
            val ofType = categories.filter { it.type == type && !it.archived }
            val parents = ofType.filter { it.parentId == null }.sortedBy { it.sortOrder }
            val childrenByParent = ofType.filter { it.parentId != null }.groupBy { it.parentId }
            return parents.map { parent ->
                parent to (childrenByParent[parent.id].orEmpty().sortedBy { it.sortOrder })
            }
        }

    val canSave: Boolean
        get() = amountMinor > 0 &&
            accountId != null &&
            when (type) {
                TxType.TRANSFER -> toAccountId != null && toAccountId != accountId
                else -> true
            }
}

class TxEditorViewModel(
    private val repo: FinanceRepository,
    private val settingsStore: SettingsStore,
) : ViewModel() {

    private val _state = MutableStateFlow(TxEditorState())
    val state: StateFlow<TxEditorState> = _state.asStateFlow()

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved.asStateFlow()

    private val reference = combine(
        repo.observeActiveAccounts(),
        repo.observeCategories(),
        settingsStore.settings,
    ) { accounts, categories, settings -> Triple(accounts, categories, settings) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, Triple(emptyList(), emptyList(), AppSettings()))

    fun load(txId: Long) {
        if (_state.value.loaded && _state.value.txId == txId) return

        viewModelScope.launch {
            val (accounts, categories, settings) = reference.first { it.first.isNotEmpty() || it.second.isNotEmpty() }
            val existing = if (txId != 0L) repo.transaction(txId) else null

            _state.update { current ->
                if (existing != null) {
                    current.copy(
                        txId = existing.id,
                        type = existing.type,
                        amountInput = Money.toEditable(existing.amountMinor),
                        date = LocalDate.ofEpochDay(existing.dateEpochDay),
                        minuteOfDay = existing.minuteOfDay,
                        accountId = existing.accountId,
                        toAccountId = existing.toAccountId,
                        categoryId = existing.categoryId,
                        note = existing.note,
                        counterparty = existing.counterparty,
                        accounts = accounts,
                        categories = categories,
                        settings = settings,
                        loaded = true,
                    )
                } else {
                    current.copy(
                        txId = 0,
                        accounts = accounts,
                        categories = categories,
                        settings = settings,
                        // Default to the first account so a new record is one tap from saveable.
                        accountId = current.accountId ?: accounts.firstOrNull()?.id,
                        loaded = true,
                    )
                }
            }
        }

        // Keep the pickers fresh if a category is added while the editor is open.
        viewModelScope.launch {
            reference.collect { (accounts, categories, settings) ->
                _state.update {
                    it.copy(
                        accounts = accounts,
                        categories = categories,
                        settings = settings,
                        accountId = it.accountId ?: accounts.firstOrNull()?.id,
                    )
                }
            }
        }
    }

    // ---- Keypad ------------------------------------------------------------

    fun appendDigit(digit: Char) = _state.update { s ->
        val input = s.amountInput
        // Two decimal places is the limit for every currency the app offers.
        val decimals = input.substringAfter('.', "")
        if (input.contains('.') && decimals.length >= 2) return@update s
        if (input == "0") return@update s.copy(amountInput = digit.toString())
        if (input.replace(".", "").length >= 12) return@update s
        s.copy(amountInput = input + digit)
    }

    fun appendDecimalPoint() = _state.update { s ->
        if (s.amountInput.contains('.')) s
        else s.copy(amountInput = if (s.amountInput.isEmpty()) "0." else s.amountInput + ".")
    }

    fun backspace() = _state.update { s ->
        s.copy(amountInput = s.amountInput.dropLast(1))
    }

    fun clearAmount() = _state.update { it.copy(amountInput = "") }

    // ---- Field edits -------------------------------------------------------

    fun setType(type: TxType) = _state.update { s ->
        // A category from the old side of the ledger would be meaningless here.
        s.copy(
            type = type,
            categoryId = if (type == TxType.TRANSFER) null
            else s.categories.firstOrNull { it.id == s.categoryId }?.takeIf { it.type == type }?.id,
            toAccountId = if (type == TxType.TRANSFER) s.toAccountId else null,
        )
    }

    fun setDate(date: LocalDate) = _state.update { it.copy(date = date) }
    fun setAccount(id: Long) = _state.update { s ->
        s.copy(accountId = id, toAccountId = if (s.toAccountId == id) null else s.toAccountId)
    }
    fun setToAccount(id: Long) = _state.update { s ->
        s.copy(toAccountId = id, accountId = if (s.accountId == id) null else s.accountId)
    }
    fun setCategory(id: Long?) = _state.update { it.copy(categoryId = id) }
    fun setNote(note: String) = _state.update { it.copy(note = note) }
    fun setCounterparty(value: String) = _state.update { it.copy(counterparty = value) }

    // ---- Persistence -------------------------------------------------------

    fun save() {
        val s = _state.value
        if (!s.canSave || s.saving) return

        _state.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            runCatching {
                repo.upsertTransaction(
                    TransactionEntity(
                        id = s.txId,
                        type = s.type,
                        amountMinor = s.amountMinor,
                        dateEpochDay = s.date.toEpochDay(),
                        minuteOfDay = s.minuteOfDay,
                        accountId = requireNotNull(s.accountId),
                        toAccountId = if (s.type == TxType.TRANSFER) s.toAccountId else null,
                        categoryId = if (s.type == TxType.TRANSFER) null else s.categoryId,
                        note = s.note.trim(),
                        counterparty = s.counterparty.trim(),
                    )
                )
            }.onSuccess {
                _saved.value = true
            }.onFailure { error ->
                _state.update { it.copy(saving = false, error = error.message ?: "Could not save") }
            }
        }
    }

    fun delete() {
        val id = _state.value.txId
        if (id == 0L) return
        viewModelScope.launch {
            repo.deleteTransaction(id)
            _saved.value = true
        }
    }

    companion object {
        val Factory = containerFactory { c: AppContainer ->
            TxEditorViewModel(c.repository, c.settings)
        }
    }
}
