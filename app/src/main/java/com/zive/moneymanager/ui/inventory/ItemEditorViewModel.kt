package com.zive.moneymanager.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zive.moneymanager.core.Money
import com.zive.moneymanager.data.db.entity.Category
import com.zive.moneymanager.data.db.entity.InventoryItem
import com.zive.moneymanager.data.db.entity.ItemKind
import com.zive.moneymanager.data.db.entity.ItemStatus
import com.zive.moneymanager.data.db.entity.QuantityUnit
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

data class ItemEditorState(
    val itemId: Long = 0,
    val name: String = "",
    val kind: ItemKind = ItemKind.CONSUMABLE,
    val categoryId: Long? = null,
    val location: String = "",
    val quantityInput: String = "1",
    val unit: QuantityUnit = QuantityUnit.PIECE,
    val purchaseDate: LocalDate? = LocalDate.now(),
    val expiryDate: LocalDate? = null,
    val warrantyEnd: LocalDate? = null,
    val lastUsed: LocalDate? = null,
    val unitCostInput: String = "",
    val status: ItemStatus = ItemStatus.IN_STOCK,
    val note: String = "",
    val categories: List<Category> = emptyList(),
    val knownLocations: List<String> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val loaded: Boolean = false,
    val saving: Boolean = false,
) {
    val isNew: Boolean get() = itemId == 0L
    val quantity: Double get() = quantityInput.replace(',', '.').toDoubleOrNull() ?: 0.0
    val unitCostMinor: Long? get() = Money.parse(unitCostInput)
    val canSave: Boolean get() = name.isNotBlank() && quantity > 0

    val totalValueMinor: Long
        get() = unitCostMinor?.let { Math.round(it * quantity) } ?: 0L

    /** Parents first, each followed by its children, for the category chip row. */
    val categoryChoices: List<Pair<Category, String>>
        get() {
            val active = categories.filter { !it.archived }
            val childrenByParent = active.filter { it.parentId != null }.groupBy { it.parentId }
            return active.filter { it.parentId == null }
                .sortedBy { it.sortOrder }
                .flatMap { parent ->
                    listOf(parent to parent.name) +
                        childrenByParent[parent.id].orEmpty()
                            .sortedBy { it.sortOrder }
                            .map { child -> child to "${parent.name} › ${child.name}" }
                }
        }
}

class ItemEditorViewModel(
    private val repo: FinanceRepository,
    settingsStore: SettingsStore,
) : ViewModel() {

    private val _state = MutableStateFlow(ItemEditorState())
    val state: StateFlow<ItemEditorState> = _state.asStateFlow()

    private val _finished = MutableStateFlow(false)
    val finished: StateFlow<Boolean> = _finished.asStateFlow()

    private val reference = combine(
        repo.observeCategories(TxType.EXPENSE),
        repo.observeInventoryLocations(),
        settingsStore.settings,
    ) { categories, locations, settings -> Triple(categories, locations, settings) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, Triple(emptyList(), emptyList(), AppSettings()))

    fun load(itemId: Long) {
        if (_state.value.loaded && _state.value.itemId == itemId) return

        viewModelScope.launch {
            val (categories, locations, settings) = reference.first()
            val existing = if (itemId != 0L) repo.inventoryItem(itemId) else null

            _state.update { current ->
                if (existing == null) {
                    current.copy(
                        itemId = 0,
                        categories = categories,
                        knownLocations = locations,
                        settings = settings,
                        loaded = true,
                    )
                } else {
                    current.copy(
                        itemId = existing.id,
                        name = existing.name,
                        kind = existing.kind,
                        categoryId = existing.categoryId,
                        location = existing.location,
                        quantityInput = formatQuantity(existing.quantity),
                        unit = existing.unit,
                        purchaseDate = existing.purchaseEpochDay?.let(LocalDate::ofEpochDay),
                        expiryDate = existing.expiryEpochDay?.let(LocalDate::ofEpochDay),
                        warrantyEnd = existing.warrantyEndEpochDay?.let(LocalDate::ofEpochDay),
                        lastUsed = existing.lastUsedEpochDay?.let(LocalDate::ofEpochDay),
                        unitCostInput = existing.unitCostMinor?.let { Money.toEditable(it) } ?: "",
                        status = existing.status,
                        note = existing.note,
                        categories = categories,
                        knownLocations = locations,
                        settings = settings,
                        loaded = true,
                    )
                }
            }
        }

        viewModelScope.launch {
            reference.collect { (categories, locations, settings) ->
                _state.update {
                    it.copy(categories = categories, knownLocations = locations, settings = settings)
                }
            }
        }
    }

    fun setName(value: String) = _state.update { it.copy(name = value) }
    fun setKind(value: ItemKind) = _state.update { s ->
        // Expiry belongs to consumables and warranty to assets; drop what no longer applies.
        s.copy(
            kind = value,
            expiryDate = if (value == ItemKind.CONSUMABLE) s.expiryDate else null,
            warrantyEnd = if (value == ItemKind.ASSET) s.warrantyEnd else null,
        )
    }
    fun setCategory(value: Long?) = _state.update { it.copy(categoryId = value) }
    fun setLocation(value: String) = _state.update { it.copy(location = value) }
    fun setQuantity(value: String) = _state.update {
        it.copy(quantityInput = value.filter { ch -> ch.isDigit() || ch == '.' }.take(9))
    }
    fun setUnit(value: QuantityUnit) = _state.update { it.copy(unit = value) }
    fun setPurchaseDate(value: LocalDate?) = _state.update { it.copy(purchaseDate = value) }
    fun setExpiryDate(value: LocalDate?) = _state.update { it.copy(expiryDate = value) }
    fun setWarrantyEnd(value: LocalDate?) = _state.update { it.copy(warrantyEnd = value) }
    fun setLastUsed(value: LocalDate?) = _state.update { it.copy(lastUsed = value) }
    fun setUnitCost(value: String) = _state.update {
        it.copy(unitCostInput = value.filter { ch -> ch.isDigit() || ch == '.' })
    }
    fun setStatus(value: ItemStatus) = _state.update { it.copy(status = value) }
    fun setNote(value: String) = _state.update { it.copy(note = value) }

    fun save() {
        val s = _state.value
        if (!s.canSave || s.saving) return
        _state.update { it.copy(saving = true) }

        viewModelScope.launch {
            val existing = if (s.itemId != 0L) repo.inventoryItem(s.itemId) else null
            repo.upsertInventoryItem(
                (existing ?: InventoryItem(name = s.name, nameKey = InventoryItem.keyOf(s.name))).copy(
                    id = s.itemId,
                    name = s.name.trim(),
                    nameKey = InventoryItem.keyOf(s.name),
                    kind = s.kind,
                    categoryId = s.categoryId,
                    location = s.location.trim(),
                    quantity = s.quantity,
                    unit = s.unit,
                    purchaseEpochDay = s.purchaseDate?.toEpochDay(),
                    expiryEpochDay = s.expiryDate?.toEpochDay(),
                    warrantyEndEpochDay = s.warrantyEnd?.toEpochDay(),
                    lastUsedEpochDay = s.lastUsed?.toEpochDay(),
                    unitCostMinor = s.unitCostMinor,
                    status = s.status,
                    statusChangedEpochDay = existing
                        ?.takeIf { it.status == s.status }
                        ?.statusChangedEpochDay
                        ?: LocalDate.now().toEpochDay(),
                    note = s.note.trim(),
                )
            )
            _finished.value = true
        }
    }

    fun delete() {
        val id = _state.value.itemId
        if (id == 0L) return
        viewModelScope.launch {
            repo.deleteInventoryItem(id)
            _finished.value = true
        }
    }

    private fun formatQuantity(value: Double): String =
        if (value == Math.floor(value) && !value.isInfinite()) {
            value.toLong().toString()
        } else {
            value.toString()
        }

    companion object {
        val Factory = containerFactory { c: AppContainer ->
            ItemEditorViewModel(c.repository, c.settings)
        }
    }
}
