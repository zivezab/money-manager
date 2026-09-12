package com.zive.moneymanager.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.zive.moneymanager.core.Money
import com.zive.moneymanager.core.PeriodType
import com.zive.moneymanager.data.db.entity.TxType
import com.zive.moneymanager.ui.components.BarEntry
import com.zive.moneymanager.ui.components.DonutChart
import com.zive.moneymanager.ui.components.DonutLegend
import com.zive.moneymanager.ui.components.DonutSlice
import com.zive.moneymanager.ui.components.EmptyState
import com.zive.moneymanager.ui.components.GroupedBarChart
import com.zive.moneymanager.ui.components.PeriodBar
import com.zive.moneymanager.ui.components.SectionHeader
import com.zive.moneymanager.ui.overview.CategoryTotalRow
import com.zive.moneymanager.ui.overview.IncomeSplitCard
import com.zive.moneymanager.ui.theme.colorFromHex
import com.zive.moneymanager.ui.theme.moneyColors
import java.time.LocalDate

/** Charts and rankings for the selected period. */
@Composable
fun StatsScreen(
    state: StatsUiState,
    onPeriodTypeChange: (PeriodType) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onJumpToToday: () -> Unit,
    onSideChange: (TxType) -> Unit,
    onDrillInto: (Long?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = LocalDate.now()
    val symbol = state.settings.symbol
    val colors = moneyColors

    val palette = colors.chart
    val slices = state.categories.mapIndexed { index, total ->
        DonutSlice(
            label = total.categoryName ?: "Uncategorised",
            value = total.totalMinor,
            color = colorFromHex(total.colorHex, palette[index % palette.size]),
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        PeriodBar(
            period = state.period,
            today = today,
            onTypeChange = onPeriodTypeChange,
            onPrevious = onPrevious,
            onNext = onNext,
            onJumpToToday = onJumpToToday,
        )

        LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
            item {
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    listOf(TxType.EXPENSE, TxType.INCOME).forEachIndexed { index, type ->
                        SegmentedButton(
                            selected = state.side == type,
                            onClick = { onSideChange(type) },
                            shape = SegmentedButtonDefaults.itemShape(index, 2),
                            label = { Text(if (type == TxType.EXPENSE) "Expenses" else "Income") },
                        )
                    }
                }
            }

            item {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        DonutChart(
                            slices = slices.take(8),
                            centerPrimary = Money.format(state.sideTotalMinor, symbol),
                            centerSecondary = if (state.side == TxType.EXPENSE) "Spent" else "Earned",
                            modifier = Modifier
                                .fillMaxWidth(0.62f)
                                .align(Alignment.CenterHorizontally),
                        )

                        Spacer(Modifier.height(16.dp))

                        if (slices.isEmpty()) {
                            Text(
                                text = "No ${if (state.side == TxType.EXPENSE) "spending" else "income"} recorded in this period.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp),
                            )
                        } else {
                            DonutLegend(
                                slices = slices.take(8),
                                formatValue = { Money.format(it, symbol) },
                                onSliceClick = { slice ->
                                    state.categories
                                        .firstOrNull { it.categoryName == slice.label }
                                        ?.categoryId
                                        ?.let(onDrillInto)
                                },
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(12.dp)) }

            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    StatTile(
                        label = "Daily average",
                        value = Money.format(state.dailyAverageMinor, symbol),
                        modifier = Modifier.weight(1f),
                    )
                    StatTile(
                        label = "Records",
                        value = state.totals.txCount.toString(),
                        modifier = Modifier.weight(1f),
                    )
                    StatTile(
                        label = "Categories",
                        value = state.categories.size.toString(),
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            item { Spacer(Modifier.height(16.dp)) }

            if (state.trend.isNotEmpty()) {
                item { SectionHeader("Income vs expenses") }
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        ),
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.padding(bottom = 12.dp),
                            ) {
                                LegendDot("Income", colors.income)
                                LegendDot("Expenses", colors.expense)
                            }
                            GroupedBarChart(
                                entries = state.trend.map {
                                    BarEntry(it.label, it.incomeMinor, it.expenseMinor)
                                },
                                incomeColor = colors.income,
                                expenseColor = colors.expense,
                            )
                        }
                    }
                }
                item { Spacer(Modifier.height(16.dp)) }
            }

            if (state.side == TxType.INCOME && state.incomeSplit.totalMinor > 0) {
                item {
                    IncomeSplitCard(
                        split = state.incomeSplit,
                        symbol = symbol,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
                item { Spacer(Modifier.height(16.dp)) }
            }

            if (state.categories.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Filled.DonutLarge,
                        title = "Nothing to chart yet",
                        subtitle = "Add a few records and this period's breakdown appears here.",
                    )
                }
            } else {
                item {
                    SectionHeader(
                        title = if (state.side == TxType.EXPENSE) "Spending by category" else "Income by category",
                        trailing = {
                            if (state.drilldownId != null) {
                                TextButton(onClick = { onDrillInto(null) }) { Text("Collapse") }
                            }
                        },
                    )
                }

                items(state.categories, key = { it.categoryId ?: -1L }) { total ->
                    Column {
                        CategoryTotalRow(
                            total = total,
                            grandTotalMinor = state.sideTotalMinor,
                            symbol = symbol,
                            onClick = { onDrillInto(total.categoryId) },
                        )
                        // Subcategories of the expanded parent, indented beneath it.
                        if (state.drilldownId != null && state.drilldownId == total.categoryId) {
                            state.subcategories
                                .filter { it.categoryId != total.categoryId }
                                .forEach { sub ->
                                    Row(
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(start = 40.dp, end = 16.dp, bottom = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = sub.categoryName ?: "—",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.weight(1f),
                                        )
                                        Text(
                                            text = Money.format(sub.totalMinor, symbol),
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(text = value, style = MaterialTheme.typography.titleMedium, maxLines = 1)
        }
    }
}

@Composable
private fun LegendDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}
