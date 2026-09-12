package com.zive.moneymanager.ui.inventory

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.zive.moneymanager.core.Money
import com.zive.moneymanager.data.db.entity.ItemKind
import com.zive.moneymanager.data.db.entity.ItemStatus
import com.zive.moneymanager.data.db.entity.QuantityUnit
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DATE_FMT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())

/** Common places things live, offered before the user has any of their own. */
private val LOCATION_PRESETS = listOf(
    "Fridge", "Freezer", "Pantry", "Kitchen", "Bathroom",
    "Wardrobe", "Bedroom", "Living room", "Store room", "Garage", "Office",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemEditorScreen(
    state: ItemEditorState,
    onBack: () -> Unit,
    callbacks: ItemEditorCallbacks,
) {
    var datePickerTarget by remember { mutableStateOf<DateField?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    Scaffold(
        // The root scaffold already inset for the status and navigation bars.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = { Text(if (state.isNew) "New item" else "Edit item") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!state.isNew) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete item")
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 16.dp),
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = callbacks.onName,
                label = { Text("Name") },
                placeholder = { Text("Milk, winter coat, drill…") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )

            Spacer(Modifier.height(12.dp))

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                ItemKind.entries.forEachIndexed { index, kind ->
                    SegmentedButton(
                        selected = state.kind == kind,
                        onClick = { callbacks.onKind(kind) },
                        shape = SegmentedButtonDefaults.itemShape(index, ItemKind.entries.size),
                        label = { Text(kind.label) },
                    )
                }
            }
            Text(
                text = if (state.kind == ItemKind.CONSUMABLE) {
                    "Gets used up and can expire — food, toiletries, medicine."
                } else {
                    "Kept and reused — electronics, furniture, tools, clothes."
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )

            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = state.quantityInput,
                    onValueChange = callbacks.onQuantity,
                    label = { Text("Quantity") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.width(130.dp),
                )
                OutlinedTextField(
                    value = state.unitCostInput,
                    onValueChange = callbacks.onUnitCost,
                    label = { Text("Cost per unit") },
                    prefix = { Text(state.settings.symbol) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
            }

            if (state.totalValueMinor > 0) {
                Text(
                    text = "Total value ${Money.format(state.totalValueMinor, state.settings.symbol)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            Spacer(Modifier.height(12.dp))
            LabeledChipRow(label = "Unit") {
                QuantityUnit.entries.forEach { unit ->
                    FilterChip(
                        selected = state.unit == unit,
                        onClick = { callbacks.onUnit(unit) },
                        label = { Text(unit.label) },
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.location,
                onValueChange = callbacks.onLocation,
                label = { Text("Location") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            LabeledChipRow(label = null) {
                val suggestions = (state.knownLocations + LOCATION_PRESETS).distinct()
                suggestions.forEach { preset ->
                    FilterChip(
                        selected = state.location.equals(preset, ignoreCase = true),
                        onClick = { callbacks.onLocation(preset) },
                        label = { Text(preset) },
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            LabeledChipRow(label = "Category") {
                FilterChip(
                    selected = state.categoryId == null,
                    onClick = { callbacks.onCategory(null) },
                    label = { Text("None") },
                )
                state.categoryChoices.forEach { (category, label) ->
                    FilterChip(
                        selected = state.categoryId == category.id,
                        onClick = { callbacks.onCategory(category.id) },
                        label = { Text(label) },
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()

            DateRow(
                label = "Bought on",
                value = state.purchaseDate,
                onPick = { datePickerTarget = DateField.PURCHASE },
                onClear = { callbacks.onPurchaseDate(null) },
            )

            if (state.kind == ItemKind.CONSUMABLE) {
                DateRow(
                    label = "Use by",
                    value = state.expiryDate,
                    emptyLabel = "No expiry date",
                    onPick = { datePickerTarget = DateField.EXPIRY },
                    onClear = { callbacks.onExpiryDate(null) },
                )
                // One tap for the dates people actually reach for.
                LabeledChipRow(label = null) {
                    listOf("3 days" to 3L, "1 week" to 7L, "2 weeks" to 14L, "1 month" to 30L, "6 months" to 182L)
                        .forEach { (label, days) ->
                            FilterChip(
                                selected = false,
                                onClick = { callbacks.onExpiryDate(LocalDate.now().plusDays(days)) },
                                label = { Text(label) },
                            )
                        }
                }
            } else {
                DateRow(
                    label = "Warranty until",
                    value = state.warrantyEnd,
                    emptyLabel = "No warranty",
                    onPick = { datePickerTarget = DateField.WARRANTY },
                    onClear = { callbacks.onWarrantyEnd(null) },
                )
                DateRow(
                    label = "Last used",
                    value = state.lastUsed,
                    emptyLabel = "Never recorded",
                    onPick = { datePickerTarget = DateField.LAST_USED },
                    onClear = { callbacks.onLastUsed(null) },
                )
                TextButton(onClick = { callbacks.onLastUsed(LocalDate.now()) }) {
                    Text("I used this today")
                }
            }

            HorizontalDivider()
            Spacer(Modifier.height(12.dp))

            LabeledChipRow(label = "Status") {
                ItemStatus.entries.forEach { status ->
                    FilterChip(
                        selected = state.status == status,
                        onClick = { callbacks.onStatus(status) },
                        label = { Text(status.label) },
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.note,
                onValueChange = callbacks.onNote,
                label = { Text("Note") },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = callbacks.onSave,
                enabled = state.canSave && !state.saving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (state.isNew) "Save item" else "Update item")
            }
            Spacer(Modifier.height(32.dp))
        }
    }

    datePickerTarget?.let { target ->
        val initial = when (target) {
            DateField.PURCHASE -> state.purchaseDate
            DateField.EXPIRY -> state.expiryDate
            DateField.WARRANTY -> state.warrantyEnd
            DateField.LAST_USED -> state.lastUsed
        } ?: LocalDate.now()

        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = initial
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { datePickerTarget = null },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        // The picker reports UTC midnight; read it back in UTC so the
                        // date does not slip a day west of Greenwich.
                        val picked = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.of("UTC"))
                            .toLocalDate()
                        when (target) {
                            DateField.PURCHASE -> callbacks.onPurchaseDate(picked)
                            DateField.EXPIRY -> callbacks.onExpiryDate(picked)
                            DateField.WARRANTY -> callbacks.onWarrantyEnd(picked)
                            DateField.LAST_USED -> callbacks.onLastUsed(picked)
                        }
                    }
                    datePickerTarget = null
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { datePickerTarget = null }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete ${state.name.ifBlank { "this item" }}?") },
            text = { Text("It will be removed from your inventory. Any expense you recorded for it stays in the ledger.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    callbacks.onDelete()
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            },
        )
    }
}

private enum class DateField { PURCHASE, EXPIRY, WARRANTY, LAST_USED }

@Composable
private fun DateRow(
    label: String,
    value: LocalDate?,
    onPick: () -> Unit,
    onClear: () -> Unit,
    emptyLabel: String = "Not set",
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onPick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value?.format(DATE_FMT) ?: emptyLabel,
            style = MaterialTheme.typography.bodyLarge,
            color = if (value == null) {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
        if (value != null) {
            TextButton(onClick = onClear) { Text("Clear") }
        }
    }
}

@Composable
private fun LabeledChipRow(label: String?, content: @Composable () -> Unit) {
    Column {
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            content()
        }
    }
}

/** Editor callbacks bundled so the screen signature stays manageable. */
data class ItemEditorCallbacks(
    val onName: (String) -> Unit,
    val onKind: (ItemKind) -> Unit,
    val onCategory: (Long?) -> Unit,
    val onLocation: (String) -> Unit,
    val onQuantity: (String) -> Unit,
    val onUnit: (QuantityUnit) -> Unit,
    val onPurchaseDate: (LocalDate?) -> Unit,
    val onExpiryDate: (LocalDate?) -> Unit,
    val onWarrantyEnd: (LocalDate?) -> Unit,
    val onLastUsed: (LocalDate?) -> Unit,
    val onUnitCost: (String) -> Unit,
    val onStatus: (ItemStatus) -> Unit,
    val onNote: (String) -> Unit,
    val onSave: () -> Unit,
    val onDelete: () -> Unit,
)
