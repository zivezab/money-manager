package com.zive.moneymanager.ui.recurring

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import com.zive.moneymanager.data.db.entity.RecurringRule
import com.zive.moneymanager.data.db.entity.TxType
import com.zive.moneymanager.ui.components.EmptyState
import com.zive.moneymanager.ui.components.IconBadge
import com.zive.moneymanager.ui.theme.moneyColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val NEXT_FMT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())

/**
 * Salary, rent, loan instalments — anything that repeats. Occurrences post
 * automatically the next time the app opens after they fall due.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringScreen(
    state: RecurringUiState,
    onBack: () -> Unit,
    onSave: RecurringSaveHandler,
    onToggle: (RecurringRule, Boolean) -> Unit,
    onDelete: (RecurringRule) -> Unit,
) {
    var editing by remember { mutableStateOf<RecurringRule?>(null) }
    var creating by remember { mutableStateOf(false) }
    val symbol = state.settings.symbol
    val today = LocalDate.now()

    Scaffold(
        // The root scaffold already inset for the status and navigation bars;
        // taking them again here would double the gap under the top bar.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = { Text("Scheduled payments") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { creating = true }) {
                Icon(Icons.Filled.Add, contentDescription = "New scheduled payment")
            }
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (state.rules.isEmpty() && !state.loading) {
                EmptyState(
                    icon = Icons.Filled.Autorenew,
                    title = "No scheduled payments",
                    subtitle = "Add your salary, rent or home loan instalment and they will be recorded for you each cycle.",
                )
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 100.dp)) {
                    items(state.rules, key = { it.id }) { rule ->
                        RuleCard(
                            rule = rule,
                            state = state,
                            symbol = symbol,
                            today = today,
                            onClick = { editing = rule },
                            onToggle = { onToggle(rule, it) },
                        )
                    }
                }
            }
        }
    }

    if (creating || editing != null) {
        RecurringEditorSheet(
            rule = editing,
            accounts = state.accounts,
            categories = state.categories,
            symbol = symbol,
            onDismiss = {
                creating = false
                editing = null
            },
            onSave = { args ->
                onSave(args)
                creating = false
                editing = null
            },
            onDelete = { rule ->
                onDelete(rule)
                creating = false
                editing = null
            },
        )
    }
}

@Composable
private fun RuleCard(
    rule: RecurringRule,
    state: RecurringUiState,
    symbol: String,
    today: LocalDate,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
) {
    val colors = moneyColors
    val category = state.category(rule.categoryId)
    val nextRun = LocalDate.ofEpochDay(rule.nextRunEpochDay)
    val daysAway = nextRun.toEpochDay() - today.toEpochDay()

    val amountColor = when (rule.type) {
        TxType.INCOME -> colors.income
        TxType.EXPENSE -> colors.expense
        TxType.TRANSFER -> colors.transfer
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
                IconBadge(
                    iconKey = category?.iconKey ?: if (rule.type == TxType.TRANSFER) "transfer" else "category",
                    colorHex = category?.colorHex,
                )
                Column(
                    Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                ) {
                    Text(
                        text = rule.name,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = buildString {
                            append(if (rule.interval == 1) "Every " else "Every ${rule.interval} ")
                            append(rule.unit.label)
                            if (rule.interval > 1) append("s")
                            append(" · ")
                            append(state.accountName(rule.accountId))
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = Money.format(rule.amountMinor, symbol),
                    style = MaterialTheme.typography.titleSmall,
                    color = amountColor,
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = when {
                        !rule.enabled -> "Paused"
                        daysAway <= 0 -> "Due now — posts on next open"
                        daysAway == 1L -> "Next: tomorrow"
                        daysAway <= 30 -> "Next: ${nextRun.format(NEXT_FMT)} (in $daysAway days)"
                        else -> "Next: ${nextRun.format(NEXT_FMT)}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Switch(checked = rule.enabled, onCheckedChange = onToggle)
            }
        }
    }
}
