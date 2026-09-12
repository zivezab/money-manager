package com.zive.moneymanager.ui.accounts

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import com.zive.moneymanager.core.Money
import com.zive.moneymanager.data.db.entity.Account
import com.zive.moneymanager.data.db.entity.AccountType
import com.zive.moneymanager.ui.components.IconPicker
import com.zive.moneymanager.ui.components.SwatchPicker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountEditorSheet(
    account: Account?,
    symbol: String,
    onDismiss: () -> Unit,
    onSave: (Long, String, AccountType, Long, String, String, Boolean, Boolean) -> Unit,
    onDelete: (Long) -> Unit,
) {
    var name by remember(account) { mutableStateOf(account?.name ?: "") }
    var type by remember(account) { mutableStateOf(account?.type ?: AccountType.BANK) }
    var opening by remember(account) {
        mutableStateOf(account?.let { Money.toEditable(it.openingBalanceMinor) } ?: "0.00")
    }
    var colorHex by remember(account) { mutableStateOf(account?.colorHex ?: "#3B82F6") }
    var iconKey by remember(account) { mutableStateOf(account?.iconKey ?: "wallet") }
    var includeInTotal by remember(account) { mutableStateOf(account?.includeInTotal ?: true) }
    var archived by remember(account) { mutableStateOf(account?.archived ?: false) }
    var confirmDelete by remember { mutableStateOf(false) }

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
                text = if (account == null) "New account" else "Edit account",
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

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = opening,
                onValueChange = { opening = it.filter { ch -> ch.isDigit() || ch == '.' || ch == '-' } },
                label = { Text("Opening balance") },
                prefix = { Text(symbol) },
                supportingText = { Text("Balance before your first recorded transaction") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(16.dp))
            Text("Type", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState()),
            ) {
                AccountType.entries.forEach { option ->
                    FilterChip(
                        selected = type == option,
                        onClick = { type = option },
                        label = { Text(option.label) },
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
                    Text("Count toward net worth", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Turn off for accounts you only track, like a company card",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = includeInTotal, onCheckedChange = { includeInTotal = it })
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Archived", modifier = Modifier.weight(1f))
                Switch(checked = archived, onCheckedChange = { archived = it })
            }

            Spacer(Modifier.height(20.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (account != null) {
                    TextButton(
                        onClick = { confirmDelete = true },
                        modifier = Modifier.weight(1f),
                    ) { Text("Delete") }
                }
                Button(
                    onClick = {
                        onSave(
                            account?.id ?: 0L,
                            name.trim(),
                            type,
                            Money.parse(opening) ?: 0L,
                            colorHex,
                            iconKey,
                            includeInTotal,
                            archived,
                        )
                    },
                    enabled = name.isNotBlank(),
                    modifier = Modifier.weight(2f),
                ) { Text("Save") }
            }
        }
    }

    if (confirmDelete && account != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete ${account.name}?") },
            text = {
                Text("Every transaction recorded against this account will be deleted too. Archive it instead to keep the history.")
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete(account.id)
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            },
        )
    }
}
