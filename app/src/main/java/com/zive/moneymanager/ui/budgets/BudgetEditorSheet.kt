package com.zive.moneymanager.ui.budgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.zive.moneymanager.core.Money
import com.zive.moneymanager.data.db.entity.Budget
import com.zive.moneymanager.data.db.entity.BudgetPeriod
import com.zive.moneymanager.data.db.entity.Category
import com.zive.moneymanager.ui.components.IconBadge

/** Create or edit one spending cap. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetEditorSheet(
    budget: Budget?,
    categories: List<Category>,
    symbol: String,
    onDismiss: () -> Unit,
    onSave: (Long, Long?, Long, BudgetPeriod, Boolean) -> Unit,
    onDelete: (Budget) -> Unit,
) {
    var amountText by remember(budget) {
        mutableStateOf(budget?.let { Money.toEditable(it.amountMinor) } ?: "")
    }
    var categoryId by remember(budget) { mutableStateOf(budget?.categoryId) }
    var period by remember(budget) { mutableStateOf(budget?.period ?: BudgetPeriod.MONTHLY) }
    var active by remember(budget) { mutableStateOf(budget?.active ?: true) }

    val amountMinor = Money.parse(amountText) ?: 0L

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .imePadding()
        ) {
            Text(
                text = if (budget == null) "New budget" else "Edit budget",
                style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                label = { Text("Limit") },
                prefix = { Text(symbol) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(16.dp))

            Text("Resets", style = androidx.compose.material3.MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BudgetPeriod.entries.forEach { option ->
                    FilterChip(
                        selected = period == option,
                        onClick = { period = option },
                        label = { Text(option.label) },
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Text("Applies to", style = androidx.compose.material3.MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = categoryId == null,
                        onClick = { categoryId = null },
                        label = { Text("All spending") },
                    )
                }
                items(categories, key = { it.id }) { category ->
                    FilterChip(
                        selected = categoryId == category.id,
                        onClick = { categoryId = category.id },
                        label = { Text(category.name) },
                        leadingIcon = {
                            IconBadge(category.iconKey, category.colorHex, size = 20)
                        },
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Active", modifier = Modifier.weight(1f))
                Switch(checked = active, onCheckedChange = { active = it })
            }

            Spacer(Modifier.height(20.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (budget != null) {
                    TextButton(
                        onClick = { onDelete(budget) },
                        modifier = Modifier.weight(1f),
                    ) { Text("Delete") }
                }
                Button(
                    onClick = {
                        onSave(budget?.id ?: 0L, categoryId, amountMinor, period, active)
                    },
                    enabled = amountMinor > 0,
                    modifier = Modifier.weight(2f),
                ) { Text("Save") }
            }
        }
    }
}
