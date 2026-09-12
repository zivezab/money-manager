package com.zive.moneymanager.ui.recurring

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
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
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.zive.moneymanager.data.db.entity.Account
import com.zive.moneymanager.data.db.entity.Category
import com.zive.moneymanager.data.db.entity.RecurrenceUnit
import com.zive.moneymanager.data.db.entity.RecurringRule
import com.zive.moneymanager.data.db.entity.TxType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Everything the editor collects for one schedule. */
data class RecurringDraft(
    val id: Long,
    val name: String,
    val type: TxType,
    val amountMinor: Long,
    val accountId: Long,
    val toAccountId: Long?,
    val categoryId: Long?,
    val note: String,
    val counterparty: String,
    val unit: RecurrenceUnit,
    val interval: Int,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val enabled: Boolean,
)

typealias RecurringSaveHandler = (RecurringDraft) -> Unit

private val DATE_FMT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringEditorSheet(
    rule: RecurringRule?,
    accounts: List<Account>,
    categories: List<Category>,
    symbol: String,
    onDismiss: () -> Unit,
    onSave: RecurringSaveHandler,
    onDelete: (RecurringRule) -> Unit,
) {
    var name by remember(rule) { mutableStateOf(rule?.name ?: "") }
    var type by remember(rule) { mutableStateOf(rule?.type ?: TxType.EXPENSE) }
    var amountText by remember(rule) {
        mutableStateOf(rule?.let { Money.toEditable(it.amountMinor) } ?: "")
    }
    var accountId by remember(rule) { mutableStateOf(rule?.accountId ?: accounts.firstOrNull()?.id) }
    var toAccountId by remember(rule) { mutableStateOf(rule?.toAccountId) }
    var categoryId by remember(rule) { mutableStateOf(rule?.categoryId) }
    var note by remember(rule) { mutableStateOf(rule?.note ?: "") }
    var counterparty by remember(rule) { mutableStateOf(rule?.counterparty ?: "") }
    var unit by remember(rule) { mutableStateOf(rule?.unit ?: RecurrenceUnit.MONTH) }
    var interval by remember(rule) { mutableStateOf((rule?.interval ?: 1).toString()) }
    var startDate by remember(rule) {
        mutableStateOf(rule?.let { LocalDate.ofEpochDay(it.startEpochDay) } ?: LocalDate.now())
    }
    var endDate by remember(rule) {
        mutableStateOf(rule?.endEpochDay?.let { LocalDate.ofEpochDay(it) })
    }
    var enabled by remember(rule) { mutableStateOf(rule?.enabled ?: true) }
    var datePickerTarget by remember { mutableStateOf<DateTarget?>(null) }

    val amountMinor = Money.parse(amountText) ?: 0L
    // Parents first, each followed by its own children, so the chip row reads as a
    // tree rather than as one alphabetical jumble of top and sub categories.
    val availableCategories = remember(categories, type) {
        val ofType = categories.filter { it.type == type && !it.archived }
        val childrenByParent = ofType.filter { it.parentId != null }.groupBy { it.parentId }
        ofType.filter { it.parentId == null }
            .sortedBy { it.sortOrder }
            .flatMap { parent ->
                listOf(parent to parent.name) +
                    childrenByParent[parent.id].orEmpty()
                        .sortedBy { it.sortOrder }
                        .map { child -> child to "${parent.name} \u203A ${child.name}" }
            }
    }
    val canSave = amountMinor > 0 && accountId != null &&
        (type != TxType.TRANSFER || (toAccountId != null && toAccountId != accountId))

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .verticalScroll(rememberScrollState())
                .imePadding()
        ) {
            Text(
                text = if (rule == null) "New scheduled payment" else "Edit scheduled payment",
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.height(16.dp))

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                TxType.entries.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = type == option,
                        onClick = {
                            type = option
                            categoryId = null
                        },
                        shape = SegmentedButtonDefaults.itemShape(index, TxType.entries.size),
                        label = { Text(option.name.lowercase().replaceFirstChar { it.uppercase() }) },
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                placeholder = { Text("Monthly salary") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                label = { Text("Amount") },
                prefix = { Text(symbol) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(16.dp))
            Text("Repeats every", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = interval,
                    onValueChange = { interval = it.filter(Char::isDigit).take(2) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(80.dp),
                )
                Row(
                    modifier = Modifier
                        .padding(start = 10.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    RecurrenceUnit.entries.forEach { option ->
                        FilterChip(
                            selected = unit == option,
                            onClick = { unit = option },
                            label = { Text(option.label.replaceFirstChar { it.uppercase() } + "s") },
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("From account", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                accounts.forEach { account ->
                    FilterChip(
                        selected = accountId == account.id,
                        onClick = { accountId = account.id },
                        label = { Text(account.name) },
                    )
                }
            }

            if (type == TxType.TRANSFER) {
                Spacer(Modifier.height(12.dp))
                Text("To account", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    accounts.filter { it.id != accountId }.forEach { account ->
                        FilterChip(
                            selected = toAccountId == account.id,
                            onClick = { toAccountId = account.id },
                            label = { Text(account.name) },
                        )
                    }
                }
            } else {
                Spacer(Modifier.height(12.dp))
                Text("Category", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    availableCategories.forEach { (category, label) ->
                        FilterChip(
                            selected = categoryId == category.id,
                            onClick = { categoryId = category.id },
                            label = { Text(label) },
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(
                    onClick = { datePickerTarget = DateTarget.START },
                    modifier = Modifier.weight(1f),
                ) { Text("Starts ${startDate.format(DATE_FMT)}") }
                TextButton(
                    onClick = { datePickerTarget = DateTarget.END },
                    modifier = Modifier.weight(1f),
                ) { Text(endDate?.let { "Ends ${it.format(DATE_FMT)}" } ?: "No end date") }
            }
            if (endDate != null) {
                TextButton(onClick = { endDate = null }) { Text("Clear end date") }
            }

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = counterparty,
                onValueChange = { counterparty = it },
                label = { Text(if (type == TxType.INCOME) "Paid by" else "Paid to") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Active", modifier = Modifier.weight(1f))
                Switch(checked = enabled, onCheckedChange = { enabled = it })
            }

            Spacer(Modifier.height(20.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (rule != null) {
                    TextButton(
                        onClick = { onDelete(rule) },
                        modifier = Modifier.weight(1f),
                    ) { Text("Delete") }
                }
                Button(
                    onClick = {
                        onSave(
                            RecurringDraft(
                                id = rule?.id ?: 0L,
                                name = name,
                                type = type,
                                amountMinor = amountMinor,
                                accountId = accountId!!,
                                toAccountId = toAccountId,
                                categoryId = categoryId,
                                note = note,
                                counterparty = counterparty,
                                unit = unit,
                                interval = interval.toIntOrNull() ?: 1,
                                startDate = startDate,
                                endDate = endDate,
                                enabled = enabled,
                            )
                        )
                    },
                    enabled = canSave,
                    modifier = Modifier.weight(2f),
                ) { Text("Save") }
            }
        }
    }

    datePickerTarget?.let { target ->
        val initial = when (target) {
            DateTarget.START -> startDate
            DateTarget.END -> endDate ?: startDate.plusYears(1)
        }
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
                        val picked = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.of("UTC"))
                            .toLocalDate()
                        when (target) {
                            DateTarget.START -> startDate = picked
                            DateTarget.END -> endDate = picked
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
}

private enum class DateTarget { START, END }
