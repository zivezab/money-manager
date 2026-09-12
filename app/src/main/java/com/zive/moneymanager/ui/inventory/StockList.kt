package com.zive.moneymanager.ui.inventory

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.zive.moneymanager.ui.components.EmptyState
import com.zive.moneymanager.ui.components.SectionHeader
import java.time.LocalDate

/** The plain browse list, grouped by storage location. */
@Composable
fun StockList(
    state: InventoryUiState,
    today: LocalDate,
    symbol: String,
    actions: InventoryActions,
    onOpenItem: (Long) -> Unit,
) {
    if (state.items.isEmpty() && !state.loading) {
        EmptyState(
            icon = Icons.Filled.Inventory2,
            title = if (state.filter.isActive) "Nothing matches" else "Nothing tracked yet",
            subtitle = if (state.filter.isActive) {
                "Try clearing the filters."
            } else {
                "Tap + to add something you own — groceries with a use-by date, or a possession you want to keep track of."
            },
        )
        return
    }

    // Grouped by where the thing lives, because that is how you go and find it.
    val grouped = state.items.groupBy { it.item.location.ifBlank { "Unsorted" } }
        .toSortedMap(compareBy { if (it == "Unsorted") "￿" else it.lowercase() })

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = InventoryListPadding,
    ) {
        grouped.forEach { (location, items) ->
            item(key = "header-$location") {
                SectionHeader(title = "$location · ${items.size}")
            }
            items(items, key = { it.item.id }) { detail ->
                InventoryRow(
                    detail = detail,
                    symbol = symbol,
                    today = today,
                    onClick = { onOpenItem(detail.item.id) },
                )
            }
            item(key = "divider-$location") { HorizontalDivider() }
        }
    }
}
