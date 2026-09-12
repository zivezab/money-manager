package com.zive.moneymanager.ui.editor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SouthWest
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zive.moneymanager.core.Money
import com.zive.moneymanager.data.db.entity.Category
import com.zive.moneymanager.data.db.entity.TxType
import com.zive.moneymanager.ui.components.IconBadge
import com.zive.moneymanager.ui.theme.moneyColors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DATE_FMT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.getDefault())

/** Create or edit a single record. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TxEditorScreen(
    state: TxEditorState,
    onBack: () -> Unit,
    onTypeChange: (TxType) -> Unit,
    onDigit: (Char) -> Unit,
    onDecimal: () -> Unit,
    onBackspace: () -> Unit,
    onClearAmount: () -> Unit,
    onDateChange: (LocalDate) -> Unit,
    onAccountChange: (Long) -> Unit,
    onToAccountChange: (Long) -> Unit,
    onCategoryChange: (Long?) -> Unit,
    onNoteChange: (String) -> Unit,
    onCounterpartyChange: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = moneyColors
    var showDatePicker by remember { mutableStateOf(false) }
    var showCategorySheet by remember { mutableStateOf(false) }
    var showAccountSheet by remember { mutableStateOf<AccountSlot?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    val accent = when (state.type) {
        TxType.INCOME -> colors.income
        TxType.EXPENSE -> colors.expense
        TxType.TRANSFER -> colors.transfer
    }

    Scaffold(
        // The root scaffold already inset for the status and navigation bars;
        // taking them again here would double the gap under the top bar.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = { Text(if (state.isNew) "New record" else "Edit record") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!state.isNew) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete record")
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding(),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    TxType.entries.forEachIndexed { index, type ->
                        SegmentedButton(
                            selected = state.type == type,
                            onClick = { onTypeChange(type) },
                            shape = SegmentedButtonDefaults.itemShape(index, TxType.entries.size),
                            label = {
                                Text(type.name.lowercase().replaceFirstChar { it.uppercase() })
                            },
                        )
                    }
                }

                // Amount display — driven by the keypad below, never focusable.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = state.settings.currency.code,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "${state.settings.symbol}${state.displayAmount}",
                        style = MaterialTheme.typography.displaySmall,
                        color = accent,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                    )
                }

                HorizontalDivider()

                EditorField(
                    icon = Icons.Filled.CalendarToday,
                    label = "Date",
                    value = state.date.format(DATE_FMT),
                    onClick = { showDatePicker = true },
                )

                EditorField(
                    icon = Icons.Filled.Wallet,
                    label = if (state.type == TxType.TRANSFER) "From account" else "Account",
                    value = state.selectedAccount?.name ?: "Select account",
                    placeholder = state.selectedAccount == null,
                    onClick = { showAccountSheet = AccountSlot.FROM },
                )

                if (state.type == TxType.TRANSFER) {
                    EditorField(
                        icon = Icons.Filled.SouthWest,
                        label = "To account",
                        value = state.selectedToAccount?.name ?: "Select account",
                        placeholder = state.selectedToAccount == null,
                        onClick = { showAccountSheet = AccountSlot.TO },
                    )
                } else {
                    EditorField(
                        icon = Icons.Filled.Category,
                        label = "Category",
                        value = state.selectedCategory?.let { category ->
                            val parent = state.categories.firstOrNull { it.id == category.parentId }
                            if (parent != null) "${parent.name} › ${category.name}" else category.name
                        } ?: "Select category",
                        placeholder = state.selectedCategory == null,
                        onClick = { showCategorySheet = true },
                    )
                }

                OutlinedTextField(
                    value = state.counterparty,
                    onValueChange = onCounterpartyChange,
                    label = {
                        Text(if (state.type == TxType.INCOME) "Paid by" else "Paid to")
                    },
                    leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                )

                OutlinedTextField(
                    value = state.note,
                    onValueChange = onNoteChange,
                    label = { Text("Note") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                )

                if (state.error != null) {
                    Text(
                        text = state.error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                    )
                }

                Spacer(Modifier.height(12.dp))
            }

            AmountKeypad(
                onDigit = onDigit,
                onDecimal = onDecimal,
                onBackspace = onBackspace,
                onClear = onClearAmount,
                onSave = onSave,
                saveEnabled = state.canSave && !state.saving,
                saveLabel = if (state.isNew) "Save" else "Update",
            )
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.date
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        // The picker reports UTC midnight; read it back in UTC to
                        // avoid landing on the previous day west of Greenwich.
                        onDateChange(
                            Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()
                        )
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (showCategorySheet) {
        ModalBottomSheet(
            onDismissRequest = { showCategorySheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            CategoryPickerSheet(
                tree = state.categoryTree,
                selectedId = state.categoryId,
                onSelect = {
                    onCategoryChange(it)
                    showCategorySheet = false
                },
            )
        }
    }

    showAccountSheet?.let { slot ->
        ModalBottomSheet(onDismissRequest = { showAccountSheet = null }) {
            Column(Modifier.padding(bottom = 24.dp)) {
                Text(
                    text = if (slot == AccountSlot.FROM) "From account" else "To account",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                )
                state.accounts.forEach { account ->
                    val disabled = when (slot) {
                        AccountSlot.FROM -> state.type == TxType.TRANSFER && account.id == state.toAccountId
                        AccountSlot.TO -> account.id == state.accountId
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !disabled) {
                                if (slot == AccountSlot.FROM) onAccountChange(account.id)
                                else onToAccountChange(account.id)
                                showAccountSheet = null
                            }
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconBadge(account.iconKey, account.colorHex, size = 36)
                        Column(Modifier.padding(start = 12.dp).weight(1f)) {
                            Text(
                                text = account.name,
                                color = if (disabled) {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                            )
                            Text(
                                text = account.type.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this record?") },
            text = { Text("${Money.format(state.amountMinor, state.settings.symbol)} on ${state.date.format(DATE_FMT)} will be removed. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            },
        )
    }
}

private enum class AccountSlot { FROM, TO }

@Composable
private fun EditorField(
    icon: ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit,
    placeholder: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 14.dp),
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = if (placeholder) {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

/** Grid of top-level categories; picking one reveals its subcategories. */
@Composable
private fun CategoryPickerSheet(
    tree: List<Pair<Category, List<Category>>>,
    selectedId: Long?,
    onSelect: (Long?) -> Unit,
) {
    var expanded by remember {
        mutableStateOf(
            tree.firstOrNull { (parent, children) ->
                parent.id == selectedId || children.any { it.id == selectedId }
            }?.first?.id
        )
    }

    Column(Modifier.padding(bottom = 24.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Choose a category",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { onSelect(null) }) { Text("None") }
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 88.dp),
            modifier = Modifier.height(300.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp),
        ) {
            items(tree, key = { it.first.id }) { (parent, children) ->
                val isSelected = parent.id == selectedId || children.any { it.id == selectedId }
                Column(
                    modifier = Modifier
                        .clickable {
                            if (children.isEmpty()) {
                                onSelect(parent.id)
                            } else {
                                expanded = if (expanded == parent.id) null else parent.id
                            }
                        }
                        .padding(vertical = 10.dp, horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box {
                        IconBadge(parent.iconKey, parent.colorHex, size = 46)
                    }
                    Text(
                        text = parent.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 2,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }

        val expandedEntry = tree.firstOrNull { it.first.id == expanded }
        if (expandedEntry != null && expandedEntry.second.isNotEmpty()) {
            HorizontalDivider()
            Text(
                text = expandedEntry.first.name,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = selectedId == expandedEntry.first.id,
                    onClick = { onSelect(expandedEntry.first.id) },
                    label = { Text("All ${expandedEntry.first.name}") },
                )
                expandedEntry.second.forEach { child ->
                    FilterChip(
                        selected = selectedId == child.id,
                        onClick = { onSelect(child.id) },
                        label = { Text(child.name) },
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}
