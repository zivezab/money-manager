package com.zive.moneymanager.ui.transactions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterListOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import com.zive.moneymanager.core.PeriodType
import com.zive.moneymanager.data.db.entity.TxType
import com.zive.moneymanager.ui.components.EmptyState
import com.zive.moneymanager.ui.components.PeriodBar
import com.zive.moneymanager.ui.components.TransactionRow
import com.zive.moneymanager.ui.theme.moneyColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DAY_HEADER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.getDefault())

/** The full ledger for the selected period, grouped by day and filterable. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    state: TransactionsUiState,
    onPeriodTypeChange: (PeriodType) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onJumpToToday: () -> Unit,
    onTypeFilter: (TxType?) -> Unit,
    onAccountFilter: (Long?) -> Unit,
    onQueryChange: (String) -> Unit,
    onClearFilters: () -> Unit,
    onOpenTransaction: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = LocalDate.now()
    val symbol = state.settings.symbol
    val colors = moneyColors
    var searchOpen by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        PeriodBar(
            period = state.period,
            today = today,
            onTypeChange = onPeriodTypeChange,
            onPrevious = onPrevious,
            onNext = onNext,
            onJumpToToday = onJumpToToday,
        )

        // Period totals strip — the same three numbers as the overview card,
        // kept visible while scrolling the list below.
        Row(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TotalCell("Income", Money.format(state.totals.incomeMinor, symbol), colors.income)
            TotalCell("Expenses", Money.format(state.totals.expenseMinor, symbol), colors.expense)
            TotalCell(
                "Net",
                Money.formatSigned(state.totals.netMinor, symbol),
                if (state.totals.netMinor >= 0) colors.income else colors.expense,
            )
        }

        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilterChip(
                selected = state.filter.type == null,
                onClick = { onTypeFilter(null) },
                label = { Text("All") },
            )
            TxType.entries.forEach { type ->
                FilterChip(
                    selected = state.filter.type == type,
                    onClick = { onTypeFilter(type) },
                    label = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }) },
                )
            }

            state.accounts.forEach { account ->
                FilterChip(
                    selected = state.filter.accountId == account.id,
                    onClick = {
                        onAccountFilter(if (state.filter.accountId == account.id) null else account.id)
                    },
                    label = { Text(account.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                )
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { searchOpen = !searchOpen }) {
                Icon(
                    imageVector = if (searchOpen) Icons.Filled.Close else Icons.Filled.Search,
                    contentDescription = if (searchOpen) "Close search" else "Search records",
                )
            }
            AnimatedVisibility(visible = searchOpen, modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = state.filter.query,
                    onValueChange = onQueryChange,
                    placeholder = { Text("Search notes, payees, categories") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                )
            }
            if (!searchOpen) {
                Text(
                    text = "${state.totals.txCount} records",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
            }
            if (state.filter.isActive) {
                IconButton(onClick = {
                    searchOpen = false
                    onClearFilters()
                }) {
                    Icon(Icons.Filled.FilterListOff, contentDescription = "Clear filters")
                }
            }
        }

        HorizontalDivider()

        if (state.groups.isEmpty() && !state.loading) {
            EmptyState(
                icon = Icons.Filled.Search,
                title = if (state.filter.isActive) "No matching records" else "No records this period",
                subtitle = if (state.filter.isActive) {
                    "Try clearing the filters or widening the date range."
                } else {
                    "Tap + to add an income or expense."
                },
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 96.dp),
            ) {
                state.groups.forEach { group ->
                    item(key = "header-${group.date}") {
                        DayHeader(group = group, symbol = symbol, today = today)
                    }
                    items(group.items, key = { it.id }) { detail ->
                        TransactionRow(
                            detail = detail,
                            symbol = symbol,
                            onClick = { onOpenTransaction(detail.id) },
                        )
                    }
                    item(key = "divider-${group.date}") { HorizontalDivider() }
                }
            }
        }
    }
}

@Composable
private fun TotalCell(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MaterialTheme.typography.titleSmall, color = color, maxLines = 1)
    }
}

@Composable
private fun DayHeader(group: DayGroup, symbol: String, today: LocalDate) {
    val colors = moneyColors
    val label = when (group.date) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> group.date.format(DAY_HEADER)
    }
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.4f))
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        if (group.incomeMinor > 0) {
            Text(
                text = "+${symbol}${Money.formatPlain(group.incomeMinor)}",
                style = MaterialTheme.typography.labelMedium,
                color = colors.income,
                modifier = Modifier.padding(end = 10.dp),
            )
        }
        if (group.expenseMinor > 0) {
            Text(
                text = "-${symbol}${Money.formatPlain(group.expenseMinor)}",
                style = MaterialTheme.typography.labelMedium,
                color = colors.expense,
            )
        }
    }
}
