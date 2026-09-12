package com.zive.moneymanager.ui.inventory

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterListOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zive.moneymanager.core.Money
import com.zive.moneymanager.data.db.entity.ItemKind
import java.time.LocalDate

/**
 * Things you own. Three views over the same data: everything in stock, what is
 * about to go off, and what you could reasonably let go of.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    state: InventoryUiState,
    actions: InventoryActions,
    onOpenItem: (Long) -> Unit,
    onAddItem: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = LocalDate.now()
    val symbol = state.settings.symbol
    var searchOpen by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        // The root scaffold already inset for the status and navigation bars.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = { Text("Inventory") },
                actions = {
                    IconButton(onClick = { searchOpen = !searchOpen }) {
                        Icon(
                            imageVector = if (searchOpen) Icons.Filled.Close else Icons.Filled.Search,
                            contentDescription = if (searchOpen) "Close search" else "Search inventory",
                        )
                    }
                    if (state.filter.isActive) {
                        IconButton(onClick = {
                            searchOpen = false
                            actions.clearFilters()
                        }) {
                            Icon(Icons.Filled.FilterListOff, contentDescription = "Clear filters")
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddItem) {
                Icon(Icons.Filled.Add, contentDescription = "Add item")
            }
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            InventorySummaryStrip(summary = state.summary, symbol = symbol)

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                InventoryTab.entries.forEachIndexed { index, entry ->
                    val badge = when (entry) {
                        InventoryTab.STOCK -> 0
                        InventoryTab.EXPIRING -> state.summary.needsAttentionCount
                        InventoryTab.DECLUTTER -> state.declutterCount + state.disposalCount
                    }
                    SegmentedButton(
                        selected = state.tab == entry,
                        onClick = { actions.setTab(entry) },
                        shape = SegmentedButtonDefaults.itemShape(index, InventoryTab.entries.size),
                        // No check icon: three labels plus counts need the width.
                        icon = {},
                        label = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(entry.label, maxLines = 1)
                                if (badge > 0) {
                                    Badge { Text(badge.toString()) }
                                }
                            }
                        },
                    )
                }
            }

            AnimatedVisibility(visible = searchOpen) {
                OutlinedTextField(
                    value = state.filter.query,
                    onValueChange = actions.setQuery,
                    placeholder = { Text("Search name, note, location") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }

            if (state.tab == InventoryTab.STOCK) {
                StockFilters(state = state, actions = actions)
            }

            HorizontalDivider()

            when (state.tab) {
                InventoryTab.STOCK -> StockList(
                    state = state,
                    today = today,
                    symbol = symbol,
                    actions = actions,
                    onOpenItem = onOpenItem,
                )
                InventoryTab.EXPIRING -> ExpiryChecklist(
                    state = state,
                    today = today,
                    symbol = symbol,
                    actions = actions,
                    onOpenItem = onOpenItem,
                )
                InventoryTab.DECLUTTER -> DeclutterList(
                    state = state,
                    today = today,
                    symbol = symbol,
                    actions = actions,
                    onOpenItem = onOpenItem,
                )
            }
        }
    }
}

/** Held count, total value, and what needs attention. */
@Composable
private fun InventorySummaryStrip(
    summary: com.zive.moneymanager.data.db.model.InventorySummary,
    symbol: String,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        SummaryCell("Items", summary.heldCount.toString())
        SummaryCell("Value", Money.format(summary.totalValueMinor, symbol))
        SummaryCell(
            "Expired",
            summary.expiredCount.toString(),
            highlight = summary.expiredCount > 0,
        )
        SummaryCell(
            "To sort",
            summary.pendingDisposalCount.toString(),
            highlight = summary.pendingDisposalCount > 0,
        )
    }
}

@Composable
private fun SummaryCell(label: String, value: String, highlight: Boolean = false) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = if (highlight) {
                com.zive.moneymanager.ui.theme.moneyColors.warning
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            maxLines = 1,
        )
    }
}

@Composable
private fun StockFilters(state: InventoryUiState, actions: InventoryActions) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilterChip(
            selected = state.filter.kind == null,
            onClick = { actions.setKind(null) },
            label = { Text("All") },
        )
        ItemKind.entries.forEach { kind ->
            FilterChip(
                selected = state.filter.kind == kind,
                onClick = { actions.setKind(if (state.filter.kind == kind) null else kind) },
                label = { Text(kind.label) },
            )
        }
        state.locations.forEach { location ->
            FilterChip(
                selected = state.filter.location == location,
                onClick = {
                    actions.setLocation(if (state.filter.location == location) null else location)
                },
                label = { Text(location, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            )
        }
        FilterChip(
            selected = state.filter.showResolved,
            onClick = { actions.setShowResolved(!state.filter.showResolved) },
            label = { Text("Include history") },
        )
    }
}

/** Everything the screen can do, bundled so the composables stay readable. */
data class InventoryActions(
    val setTab: (InventoryTab) -> Unit,
    val setKind: (ItemKind?) -> Unit,
    val setLocation: (String?) -> Unit,
    val setQuery: (String) -> Unit,
    val setShowResolved: (Boolean) -> Unit,
    val clearFilters: () -> Unit,
    val markUsedUp: (Long) -> Unit,
    val markUsedToday: (Long) -> Unit,
    val extendExpiry: (Long, Long) -> Unit,
    val flagForDonation: (Long) -> Unit,
    val flagForDiscard: (Long) -> Unit,
    val returnToStock: (Long) -> Unit,
    val completeDonation: (Long) -> Unit,
    val completeDiscard: (Long) -> Unit,
)

/** Shared empty-list padding so the three views line up. */
val InventoryListPadding = PaddingValues(bottom = 110.dp)
