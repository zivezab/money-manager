package com.zive.moneymanager.ui.overview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zive.moneymanager.core.Money
import com.zive.moneymanager.ui.components.EmptyState
import com.zive.moneymanager.ui.components.PeriodBar
import com.zive.moneymanager.ui.components.SectionHeader
import com.zive.moneymanager.ui.components.TransactionRow
import java.time.LocalDate

/**
 * The landing screen: what came in, what went out, and where it is going —
 * all for whichever period is selected.
 */
@Composable
fun OverviewScreen(
    state: OverviewUiState,
    onPeriodTypeChange: (com.zive.moneymanager.core.PeriodType) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onJumpToToday: () -> Unit,
    onOpenTransaction: (Long) -> Unit,
    onSeeAllRecords: () -> Unit,
    onOpenBudgets: () -> Unit,
    onOpenAccounts: () -> Unit,
    onOpenStats: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = LocalDate.now()
    val symbol = state.settings.symbol

    Column(modifier = modifier.fillMaxSize()) {
        PeriodBar(
            period = state.period,
            today = today,
            onTypeChange = onPeriodTypeChange,
            onPrevious = onPrevious,
            onNext = onNext,
            onJumpToToday = onJumpToToday,
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item {
                SummaryCard(
                    totals = state.totals,
                    symbol = symbol,
                    periodLabel = state.period.label(today),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }

            if (state.incomeSplit.totalMinor > 0) {
                item {
                    IncomeSplitCard(
                        split = state.incomeSplit,
                        symbol = symbol,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
                item { Spacer(Modifier.height(8.dp)) }
            }

            if (state.accounts.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Accounts · ${Money.format(state.netWorthMinor, symbol)}",
                        trailing = {
                            TextButton(onClick = onOpenAccounts) { Text("Manage") }
                        },
                    )
                }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(state.accounts.filter { !it.archived }, key = { it.accountId }) { account ->
                            AccountTile(
                                balance = account,
                                symbol = symbol,
                                onClick = onOpenAccounts,
                            )
                        }
                    }
                }
                item { Spacer(Modifier.height(8.dp)) }
            }

            if (state.budgets.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Budgets",
                        trailing = { TextButton(onClick = onOpenBudgets) { Text("All") } },
                    )
                }
                items(state.budgets.take(3), key = { it.budget.id }) { progress ->
                    BudgetRow(
                        progress = progress,
                        symbol = symbol,
                        today = today,
                        onClick = onOpenBudgets,
                    )
                }
                item { Spacer(Modifier.height(8.dp)) }
            }

            if (state.topExpenseCategories.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Top spending",
                        trailing = { TextButton(onClick = onOpenStats) { Text("Stats") } },
                    )
                }
                items(state.topExpenseCategories, key = { it.categoryId ?: -1L }) { total ->
                    CategoryTotalRow(
                        total = total,
                        grandTotalMinor = state.totals.expenseMinor,
                        symbol = symbol,
                        onClick = onOpenStats,
                    )
                }
                item { Spacer(Modifier.height(8.dp)) }
            }

            item {
                SectionHeader(
                    title = "Recent records",
                    trailing = { TextButton(onClick = onSeeAllRecords) { Text("See all") } },
                )
            }

            if (state.recent.isEmpty() && !state.loading) {
                item {
                    EmptyState(
                        icon = Icons.AutoMirrored.Filled.ReceiptLong,
                        title = "Nothing recorded yet",
                        subtitle = "Tap + to log your first income or expense for this period.",
                    )
                }
            } else {
                items(state.recent, key = { it.id }) { detail ->
                    TransactionRow(
                        detail = detail,
                        symbol = symbol,
                        onClick = { onOpenTransaction(detail.id) },
                    )
                }
            }

            if (state.totals.txCount > state.recent.size) {
                item {
                    HorizontalDivider()
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        TextButton(onClick = onSeeAllRecords) {
                            Text("View all ${state.totals.txCount} records")
                        }
                    }
                }
            }

            if (state.loading) {
                item {
                    Text(
                        text = "Loading…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }
    }
}
