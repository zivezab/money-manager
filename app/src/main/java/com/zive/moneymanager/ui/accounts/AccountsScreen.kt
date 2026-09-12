package com.zive.moneymanager.ui.accounts

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.zive.moneymanager.data.db.entity.Account
import com.zive.moneymanager.ui.components.IconBadge
import com.zive.moneymanager.ui.components.SectionHeader
import com.zive.moneymanager.ui.theme.moneyColors

/** Wallets, cards and savings, with balances derived from the ledger. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    state: AccountsUiState,
    onBack: () -> Unit,
    onSave: (Long, String, com.zive.moneymanager.data.db.entity.AccountType, Long, String, String, Boolean, Boolean) -> Unit,
    onDelete: (Long) -> Unit,
) {
    var editing by remember { mutableStateOf<Account?>(null) }
    var creating by remember { mutableStateOf(false) }
    val symbol = state.settings.symbol
    val colors = moneyColors

    Scaffold(
        // The root scaffold already inset for the status and navigation bars;
        // taking them again here would double the gap under the top bar.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = { Text("Accounts") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { creating = true }) {
                Icon(Icons.Filled.Add, contentDescription = "New account")
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 100.dp),
        ) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text(
                            text = "Net worth",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = Money.format(state.netWorthMinor, symbol),
                            style = MaterialTheme.typography.headlineMedium,
                            color = if (state.netWorthMinor < 0) colors.expense else MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                            Column {
                                Text(
                                    "Assets",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    Money.format(state.assetsMinor, symbol),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = colors.income,
                                )
                            }
                            Column {
                                Text(
                                    "Liabilities",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    Money.format(state.liabilitiesMinor, symbol),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = colors.expense,
                                )
                            }
                        }
                    }
                }
            }

            val active = state.balances.filter { !it.archived }
            val archived = state.balances.filter { it.archived }

            item { SectionHeader("Active") }
            items(active, key = { it.accountId }) { balance ->
                AccountRow(
                    name = balance.name,
                    subtitle = balance.type.label,
                    iconKey = balance.iconKey,
                    colorHex = balance.colorHex,
                    amount = Money.format(balance.balanceMinor, symbol),
                    negative = balance.balanceMinor < 0,
                    onClick = {
                        editing = state.accounts.firstOrNull { it.id == balance.accountId }
                    },
                )
            }

            if (archived.isNotEmpty()) {
                item {
                    HorizontalDivider()
                    SectionHeader("Archived")
                }
                items(archived, key = { it.accountId }) { balance ->
                    AccountRow(
                        name = balance.name,
                        subtitle = "${balance.type.label} · archived",
                        iconKey = balance.iconKey,
                        colorHex = balance.colorHex,
                        amount = Money.format(balance.balanceMinor, symbol),
                        negative = balance.balanceMinor < 0,
                        onClick = {
                            editing = state.accounts.firstOrNull { it.id == balance.accountId }
                        },
                    )
                }
            }
        }
    }

    if (creating || editing != null) {
        AccountEditorSheet(
            account = editing,
            symbol = symbol,
            onDismiss = {
                creating = false
                editing = null
            },
            onSave = { id, name, type, opening, color, icon, include, archivedFlag ->
                onSave(id, name, type, opening, color, icon, include, archivedFlag)
                creating = false
                editing = null
            },
            onDelete = { id ->
                onDelete(id)
                creating = false
                editing = null
            },
        )
    }
}

@Composable
private fun AccountRow(
    name: String,
    subtitle: String,
    iconKey: String,
    colorHex: String,
    amount: String,
    negative: Boolean,
    onClick: () -> Unit,
) {
    val colors = moneyColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(iconKey, colorHex)
        Column(
            Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Text(name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = amount,
            style = MaterialTheme.typography.titleSmall,
            color = if (negative) colors.expense else MaterialTheme.colorScheme.onSurface,
        )
    }
}
