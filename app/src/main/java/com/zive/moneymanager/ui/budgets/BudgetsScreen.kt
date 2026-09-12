package com.zive.moneymanager.ui.budgets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import com.zive.moneymanager.data.db.entity.Budget
import com.zive.moneymanager.data.repo.BudgetProgress
import com.zive.moneymanager.ui.components.EmptyState
import com.zive.moneymanager.ui.components.IconBadge
import com.zive.moneymanager.ui.components.ProgressBar
import com.zive.moneymanager.ui.theme.moneyColors
import java.time.LocalDate

/** Spending caps and how far through each one you are. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetsScreen(
    state: BudgetsUiState,
    onSave: (Long, Long?, Long, com.zive.moneymanager.data.db.entity.BudgetPeriod, Boolean) -> Unit,
    onDelete: (Budget) -> Unit,
    modifier: Modifier = Modifier,
) {
    var editing by remember { mutableStateOf<Budget?>(null) }
    var creating by remember { mutableStateOf(false) }
    val today = LocalDate.now()
    val symbol = state.settings.symbol
    val colors = moneyColors

    Scaffold(
        // The root scaffold already inset for the status and navigation bars;
        // taking them again here would double the gap under the top bar.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("Budgets") }, windowInsets = WindowInsets(0, 0, 0, 0)) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { creating = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("New budget") },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (state.progress.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = "Budgeted across all caps",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = "${Money.format(state.totalSpentMinor, symbol)} of ${Money.format(state.totalBudgetedMinor, symbol)}",
                                    style = MaterialTheme.typography.titleMedium,
                                )
                            }
                            if (state.overspentCount > 0) {
                                Text(
                                    text = "${state.overspentCount} over",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = colors.danger,
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        ProgressBar(
                            fraction = if (state.totalBudgetedMinor <= 0) 0f
                            else state.totalSpentMinor.toFloat() / state.totalBudgetedMinor,
                            color = if (state.overspentCount > 0) colors.danger else colors.positive,
                        )
                    }
                }
            }

            if (state.progress.isEmpty() && !state.loading) {
                EmptyState(
                    icon = Icons.Filled.Savings,
                    title = "No budgets yet",
                    subtitle = "Set a monthly cap on groceries, travel or total spending and track it here.",
                )
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 100.dp)) {
                    items(state.progress, key = { it.budget.id }) { progress ->
                        BudgetCard(
                            progress = progress,
                            symbol = symbol,
                            today = today,
                            onClick = { editing = progress.budget },
                        )
                    }
                }
            }
        }
    }

    if (creating || editing != null) {
        BudgetEditorSheet(
            budget = editing,
            categories = state.expenseCategories,
            symbol = symbol,
            onDismiss = {
                creating = false
                editing = null
            },
            onSave = { id, categoryId, amount, period, active ->
                onSave(id, categoryId, amount, period, active)
                creating = false
                editing = null
            },
            onDelete = { budget ->
                onDelete(budget)
                creating = false
                editing = null
            },
        )
    }
}

@Composable
private fun BudgetCard(
    progress: BudgetProgress,
    symbol: String,
    today: LocalDate,
    onClick: () -> Unit,
) {
    val colors = moneyColors
    val status = progress.status(today)
    val statusColor = when (status) {
        BudgetProgress.Status.OVER -> colors.danger
        BudgetProgress.Status.AT_RISK -> colors.warning
        BudgetProgress.Status.ON_TRACK -> colors.positive
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(progress.iconKey, progress.colorHex, size = 38)
                Column(
                    Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                ) {
                    Text(
                        text = progress.categoryName,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${progress.budget.period.label} · ${progress.period.label(today)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
                if (!progress.budget.active) {
                    Text(
                        text = "Paused",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = Money.format(progress.spentMinor, symbol),
                    style = MaterialTheme.typography.titleMedium,
                    color = statusColor,
                )
                Text(
                    text = " of ${Money.format(progress.budget.amountMinor, symbol)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "${(progress.fraction * 100).toInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    color = statusColor,
                )
            }

            Spacer(Modifier.height(8.dp))
            ProgressBar(fraction = progress.fraction, color = statusColor)
            Spacer(Modifier.height(8.dp))

            val remaining = progress.remainingMinor
            val safeDaily = progress.safeDailySpend(today)
            Text(
                text = when {
                    remaining < 0 -> "Over by ${Money.format(-remaining, symbol)}"
                    safeDaily != null ->
                        "${Money.format(remaining, symbol)} left · ${Money.format(safeDaily, symbol)}/day to stay on track"
                    else -> "${Money.format(remaining, symbol)} left"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (status == BudgetProgress.Status.AT_RISK) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Spending faster than the period is elapsing.",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.warning,
                )
            }
        }
    }
}
