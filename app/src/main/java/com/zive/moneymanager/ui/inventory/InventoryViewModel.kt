package com.zive.moneymanager.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zive.moneymanager.core.ExpiryBucket
import com.zive.moneymanager.data.db.entity.Category
import com.zive.moneymanager.data.db.entity.ItemKind
import com.zive.moneymanager.data.db.entity.ItemStatus
import com.zive.moneymanager.data.db.entity.TxType
import com.zive.moneymanager.data.db.model.DuplicateGroup
import com.zive.moneymanager.data.db.model.InventoryItemDetail
import com.zive.moneymanager.data.db.model.InventorySummary
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

enum class InventoryTab(val label: String) {
    STOCK("Stock"),
    EXPIRING("Expiring"),
    DECLUTTER("Declutter"),
}

data class InventoryFilter(
    val kind: ItemKind? = null,
    val categoryId: Long? = null,
    val location: String? = null,
    val query: String = "",
    /** Include things already used up, donated or thrown away. */
    val showResolved: Boolean = false,
) {
    val isActive: Boolean
        get() = kind != null || categoryId != null || location != null ||
            query.isNotBlank() || showResolved
}

/** Items sharing an urgency band, in the order the checklist renders them. */
data class ExpirySection(
    val bucket: ExpiryBucket,
    val items: List<InventoryItemDetail>,
)

data class InventoryUiState(
    val tab: InventoryTab = InventoryTab.STOCK,
    val settings: AppSettings = AppSettings(),
    val summary: InventorySummary = InventorySummary(),
    val filter: InventoryFilter = InventoryFilter(),
    val items: List<InventoryItemDetail> = emptyList(),
    val expirySections: List<ExpirySection> = emptyList(),
    val duplicates: List<DuplicateGroup> = emptyList(),
    val untouched: List<InventoryItemDetail> = emptyList(),
    val toDonate: List<InventoryItemDetail> = emptyList(),
    val toDiscard: List<InventoryItemDetail> = emptyList(),
    val categories: List<Category> = emptyList(),
    val locations: List<String> = emptyList(),
    val loading: Boolean = true,
) {
    /** Items already past their date and still in the house. */
    val expiredHeld: List<InventoryItemDetail>
        get() = expirySections.firstOrNull { it.bucket == ExpiryBucket.EXPIRED }?.items.orEmpty()

    val declutterCount: Int
        get() = duplicates.size + untouched.size + expiredHeld.size

    val disposalCount: Int get() = toDonate.size + toDiscard.size
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class InventoryViewModel(
    private val repo: FinanceRepository,
    settingsStore: SettingsStore,
) : ViewModel() {

    private val tab = MutableStateFlow(InventoryTab.STOCK)
    private val filter = MutableStateFlow(InventoryFilter())

    private data class Context(
        val settings: AppSettings,
        val tab: InventoryTab,
        val filter: InventoryFilter,
        val today: LocalDate,
    )

    private val context = combine(
        settingsStore.settings,
        tab,
        filter.debounce { if (it.query.isBlank()) 0L else 220L },
    ) { settings, activeTab, activeFilter ->
        Context(settings, activeTab, activeFilter, LocalDate.now())
    }

    val state = context.flatMapLatest { ctx ->
        val today = ctx.today

        // The checklist covers everything due within a month; anything further out
        // is browsing, not a to-do, and lives on the Stock tab.
        val checklistHorizon = today.plusDays(31)

        val stock = combine(
            repo.observeInventorySummary(today, ctx.settings.expiryHorizon(today)),
            repo.observeInventory(
                heldOnly = !ctx.filter.showResolved,
                kind = ctx.filter.kind,
                categoryId = ctx.filter.categoryId,
                location = ctx.filter.location,
                query = ctx.filter.query,
            ),
            repo.observeInventoryLocations(),
        ) { summary, items, locations -> Triple(summary, items, locations) }

        val organise = combine(
            repo.observeExpiringBy(checklistHorizon),
            repo.observeDuplicateGroups(),
            repo.observeUntouchedSince(ctx.settings.declutterCutoff(today)),
            repo.observePendingDisposal(),
            repo.observeCategories(TxType.EXPENSE),
        ) { expiring, duplicates, untouched, disposal, categories ->
            Organise(expiring, duplicates, untouched, disposal, categories)
        }

        combine(stock, organise) { (summary, items, locations), org ->
            InventoryUiState(
                tab = ctx.tab,
                settings = ctx.settings,
                summary = summary,
                filter = ctx.filter,
                items = items,
                expirySections = groupByBucket(org.expiring, today),
                duplicates = org.duplicates,
                // A duplicate row already nags about these; don't say it twice.
                untouched = org.untouched.filterNot { detail ->
                    org.duplicates.any { it.nameKey == detail.item.nameKey }
                },
                toDonate = org.disposal.filter { it.item.status == ItemStatus.TO_DONATE },
                toDiscard = org.disposal.filter { it.item.status == ItemStatus.TO_DISCARD },
                categories = org.categories,
                locations = locations,
                loading = false,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InventoryUiState())

    private data class Organise(
        val expiring: List<InventoryItemDetail>,
        val duplicates: List<DuplicateGroup>,
        val untouched: List<InventoryItemDetail>,
        val disposal: List<InventoryItemDetail>,
        val categories: List<Category>,
    )

    private fun groupByBucket(
        items: List<InventoryItemDetail>,
        today: LocalDate,
    ): List<ExpirySection> {
        val todayEpoch = today.toEpochDay()
        return items
            .groupBy { ExpiryBucket.of(it.item.expiryEpochDay, todayEpoch) }
            .toSortedMap(compareBy { it.ordinal })
            .map { (bucket, bucketItems) -> ExpirySection(bucket, bucketItems) }
    }

    // ---- Filters -----------------------------------------------------------

    fun setTab(value: InventoryTab) { tab.value = value }
    fun setKind(value: ItemKind?) = filter.update { it.copy(kind = value) }
    fun setCategory(value: Long?) = filter.update { it.copy(categoryId = value) }
    fun setLocation(value: String?) = filter.update { it.copy(location = value) }
    fun setQuery(value: String) = filter.update { it.copy(query = value) }
    fun setShowResolved(value: Boolean) = filter.update { it.copy(showResolved = value) }
    fun clearFilters() = filter.update { InventoryFilter() }

    // ---- Checklist actions -------------------------------------------------

    /** "I finished it" — leaves the ledger history intact, drops it from stock. */
    fun markUsedUp(id: Long) = updateStatus(id, ItemStatus.USED_UP)

    /** "I used this today" — resets the idle clock without changing stock. */
    fun markUsedToday(id: Long) {
        viewModelScope.launch { repo.markInventoryUsed(id) }
    }

    fun flagForDonation(id: Long) = updateStatus(id, ItemStatus.TO_DONATE)
    fun flagForDiscard(id: Long) = updateStatus(id, ItemStatus.TO_DISCARD)
    fun returnToStock(id: Long) = updateStatus(id, ItemStatus.IN_STOCK)

    /** Ticking an item off the donation checklist. */
    fun completeDonation(id: Long) = updateStatus(id, ItemStatus.DONATED)
    fun completeDiscard(id: Long) = updateStatus(id, ItemStatus.DISCARDED)

    /** Snoozes an expiry date — "this is still fine, check again later". */
    fun extendExpiry(id: Long, days: Long) {
        viewModelScope.launch {
            val item = repo.inventoryItem(id) ?: return@launch
            val base = item.expiryEpochDay?.let(LocalDate::ofEpochDay) ?: LocalDate.now()
            // Never push the new date into the past when snoozing something overdue.
            val from = maxOf(base, LocalDate.now())
            repo.setInventoryExpiry(id, from.plusDays(days))
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch { repo.deleteInventoryItem(id) }
    }

    private fun updateStatus(id: Long, status: ItemStatus) {
        viewModelScope.launch { repo.setInventoryStatus(id, status) }
    }

    companion object {
        val Factory = containerFactory { c: AppContainer ->
            InventoryViewModel(c.repository, c.settings)
        }
    }
}
