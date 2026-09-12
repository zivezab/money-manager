package com.zive.moneymanager.ui.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zive.moneymanager.core.Money
import com.zive.moneymanager.data.db.model.AccountBalance
import com.zive.moneymanager.data.db.model.CategoryTotal
import com.zive.moneymanager.data.db.model.PeriodTotals
import com.zive.moneymanager.data.repo.BudgetProgress
import com.zive.moneymanager.ui.components.IconBadge
import com.zive.moneymanager.ui.components.ProgressBar
import com.zive.moneymanager.ui.theme.AmountLargeStyle
import com.zive.moneymanager.ui.theme.colorFromHex
import com.zive.moneymanager.ui.theme.moneyColors
import java.time.LocalDate

/** Income, expenses and the resulting net for the selected period. */
@Composable
fun SummaryCard(
    totals: PeriodTotals,
    symbol: String,
    periodLabel: String,
    modifier: Modifier = Modifier,
) {
    val colors = moneyColors
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                text = periodLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = Money.formatSigned(totals.netMinor, symbol),
                style = AmountLargeStyle,
                color = if (totals.netMinor >= 0) colors.income else colors.expense,
            )
            Text(
                text = if (totals.netMinor >= 0) "left over this period" else "over budget this period",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FlowStat(
                    label = "Income",
                    amount = Money.format(totals.incomeMinor, symbol),
                    color = colors.income,
                    container = colors.incomeContainer,
                    up = true,
                    modifier = Modifier.weight(1f),
                )
                FlowStat(
                    label = "Expenses",
                    amount = Money.format(totals.expenseMinor, symbol),
                    color = colors.expense,
                    container = colors.expenseContainer,
                    up = false,
                    modifier = Modifier.weight(1f),
                )
            }

            if (totals.incomeMinor > 0) {
                Spacer(Modifier.height(14.dp))
                val savedFraction = (totals.netMinor.toFloat() / totals.incomeMinor).coerceIn(0f, 1f)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Savings rate",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "${(savedFraction * 100).toInt()}%",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (totals.netMinor >= 0) colors.income else colors.expense,
                    )
                }
                Spacer(Modifier.height(6.dp))
                ProgressBar(
                    fraction = savedFraction,
                    color = if (totals.netMinor >= 0) colors.income else colors.expense,
                )
            }
        }
    }
}

@Composable
private fun FlowStat(
    label: String,
    amount: String,
    color: Color,
    container: Color,
    up: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(container)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (up) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(18.dp),
        )
        Column(Modifier.padding(start = 8.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color.copy(alpha = 0.85f),
            )
            Text(
                text = amount,
                style = MaterialTheme.typography.titleSmall,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * The job / jobless answer: how much of this period's income came from
 * employment, and how much from benefits, investments and everything else.
 */
@Composable
fun IncomeSplitCard(
    split: IncomeSplit,
    symbol: String,
    modifier: Modifier = Modifier,
) {
    val colors = moneyColors
    val jobColor = colors.income
    val nonJobColor = colors.chart[6]
    val otherColor = MaterialTheme.colorScheme.outline

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Where income came from",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "${(split.jobFraction * 100).toInt()}% from work",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(12.dp))

            // A single stacked bar reads faster than three separate meters.
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                val total = split.totalMinor.coerceAtLeast(1L).toFloat()
                if (split.jobMinor > 0) {
                    Box(Modifier.weight(split.jobMinor / total).fillMaxHeight().background(jobColor))
                }
                if (split.nonJobMinor > 0) {
                    Box(Modifier.weight(split.nonJobMinor / total).fillMaxHeight().background(nonJobColor))
                }
                if (split.unclassifiedMinor > 0) {
                    Box(Modifier.weight(split.unclassifiedMinor / total).fillMaxHeight().background(otherColor))
                }
            }

            Spacer(Modifier.height(12.dp))

            SplitLegendRow("Employment", split.jobMinor, jobColor, symbol)
            if (split.nonJobMinor > 0) {
                SplitLegendRow("Benefits, investments & other", split.nonJobMinor, nonJobColor, symbol)
            }
            if (split.unclassifiedMinor > 0) {
                SplitLegendRow("Uncategorised", split.unclassifiedMinor, otherColor, symbol)
            }
        }
    }
}

@Composable
private fun SplitLegendRow(label: String, amountMinor: Long, color: Color, symbol: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = Money.format(amountMinor, symbol),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

/** Compact budget meter shown on the overview; the Budgets tab has the full view. */
@Composable
fun BudgetRow(
    progress: BudgetProgress,
    symbol: String,
    today: LocalDate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = moneyColors
    val statusColor = when (progress.status(today)) {
        BudgetProgress.Status.OVER -> colors.danger
        BudgetProgress.Status.AT_RISK -> colors.warning
        BudgetProgress.Status.ON_TRACK -> colors.positive
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(progress.iconKey, progress.colorHex, size = 32)
            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp)
            ) {
                Text(
                    text = progress.categoryName,
                    style = MaterialTheme.typography.bodyMedium,
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
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = Money.format(progress.spentMinor, symbol),
                    style = MaterialTheme.typography.bodyMedium,
                    color = statusColor,
                )
                Text(
                    text = "of ${Money.format(progress.budget.amountMinor, symbol)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        ProgressBar(fraction = progress.fraction, color = statusColor, height = 6)
    }
}

/** Horizontally scrolling account tile with its derived balance. */
@Composable
fun AccountTile(
    balance: AccountBalance,
    symbol: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = colorFromHex(balance.colorHex, MaterialTheme.colorScheme.primary)
    Card(
        modifier = modifier
            .width(160.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(14.dp)) {
            IconBadge(balance.iconKey, balance.colorHex, size = 32)
            Spacer(Modifier.height(10.dp))
            Text(
                text = balance.name,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = Money.format(balance.balanceMinor, symbol),
                style = MaterialTheme.typography.titleMedium,
                color = if (balance.balanceMinor < 0) moneyColors.expense else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
            Spacer(Modifier.height(6.dp))
            Box(
                Modifier
                    .width(28.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accent)
            )
        }
    }
}

/** Ranked spending category with a share-of-total meter. */
@Composable
fun CategoryTotalRow(
    total: CategoryTotal,
    grandTotalMinor: Long,
    symbol: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val color = colorFromHex(total.colorHex, MaterialTheme.colorScheme.primary)
    val fraction = if (grandTotalMinor <= 0) 0f else total.totalMinor.toFloat() / grandTotalMinor

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(total.iconKey, total.colorHex, size = 32)
            Text(
                text = total.categoryName ?: "Uncategorised",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp),
            )
            Text(
                text = "${(fraction * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 10.dp),
            )
            Text(
                text = Money.format(total.totalMinor, symbol),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Spacer(Modifier.height(6.dp))
        ProgressBar(fraction = fraction, color = color, height = 5)
    }
}
