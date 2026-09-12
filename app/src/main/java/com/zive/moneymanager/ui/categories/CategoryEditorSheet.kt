package com.zive.moneymanager.ui.categories

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.unit.dp
import com.zive.moneymanager.data.db.entity.Category
import com.zive.moneymanager.data.db.entity.IncomeKind
import com.zive.moneymanager.data.db.entity.TxType
import com.zive.moneymanager.ui.components.IconPicker
import com.zive.moneymanager.ui.components.SwatchPicker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryEditorSheet(
    category: Category?,
    defaultType: TxType,
    defaultParentId: Long?,
    parents: List<Category>,
    onDismiss: () -> Unit,
    onSave: (Long, String, TxType, Long?, String, String, IncomeKind, Boolean) -> Unit,
    onDelete: (Category) -> Unit,
) {
    var name by remember(category) { mutableStateOf(category?.name ?: "") }
    var type by remember(category) { mutableStateOf(category?.type ?: defaultType) }
    var parentId by remember(category) { mutableStateOf(category?.parentId ?: defaultParentId) }
    var colorHex by remember(category) { mutableStateOf(category?.colorHex ?: "#64748B") }
    var iconKey by remember(category) { mutableStateOf(category?.iconKey ?: "category") }
    var incomeKind by remember(category) {
        mutableStateOf(category?.incomeKind?.takeIf { it != IncomeKind.NONE } ?: IncomeKind.JOB)
    }
    var archived by remember(category) { mutableStateOf(category?.archived ?: false) }
    var confirmDelete by remember { mutableStateOf(false) }

    // A parent cannot be its own child, and this app keeps the tree two deep.
    val selectableParents = parents.filter { it.id != category?.id && it.type == type }

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
                text = if (category == null) "New category" else "Edit category",
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(16.dp))

            // Changing side on an existing category would orphan its records.
            if (category == null) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    listOf(TxType.EXPENSE, TxType.INCOME).forEachIndexed { index, option ->
                        SegmentedButton(
                            selected = type == option,
                            onClick = {
                                type = option
                                parentId = null
                            },
                            shape = SegmentedButtonDefaults.itemShape(index, 2),
                            label = { Text(if (option == TxType.EXPENSE) "Expense" else "Income") },
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            if (type == TxType.INCOME) {
                Text("Income source", style = MaterialTheme.typography.labelLarge)
                Text(
                    text = "Drives the employment split on the overview",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = incomeKind == IncomeKind.JOB,
                        onClick = { incomeKind = IncomeKind.JOB },
                        label = { Text("From work") },
                    )
                    FilterChip(
                        selected = incomeKind == IncomeKind.NON_JOB,
                        onClick = { incomeKind = IncomeKind.NON_JOB },
                        label = { Text("Not from work") },
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            Text("Parent category", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState()),
            ) {
                FilterChip(
                    selected = parentId == null,
                    onClick = { parentId = null },
                    label = { Text("Top level") },
                )
                selectableParents.forEach { parent ->
                    FilterChip(
                        selected = parentId == parent.id,
                        onClick = { parentId = parent.id },
                        label = { Text(parent.name) },
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Colour", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(6.dp))
            SwatchPicker(selectedHex = colorHex, onSelect = { colorHex = it })

            Spacer(Modifier.height(16.dp))
            Text("Icon", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(6.dp))
            IconPicker(selectedKey = iconKey, tintHex = colorHex, onSelect = { iconKey = it })

            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Archived", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Hidden from pickers, existing records keep it",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = archived, onCheckedChange = { archived = it })
            }

            Spacer(Modifier.height(20.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (category != null) {
                    TextButton(
                        onClick = { confirmDelete = true },
                        modifier = Modifier.weight(1f),
                    ) { Text("Delete") }
                }
                Button(
                    onClick = {
                        onSave(
                            category?.id ?: 0L,
                            name.trim(),
                            type,
                            parentId,
                            colorHex,
                            iconKey,
                            incomeKind,
                            archived,
                        )
                    },
                    enabled = name.isNotBlank(),
                    modifier = Modifier.weight(2f),
                ) { Text("Save") }
            }
        }
    }

    if (confirmDelete && category != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete ${category.name}?") },
            text = {
                Text("Records in this category stay, but become uncategorised. Subcategories move to the top level.")
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete(category)
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            },
        )
    }
}
