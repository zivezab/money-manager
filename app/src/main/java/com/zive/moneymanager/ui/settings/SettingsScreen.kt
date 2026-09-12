package com.zive.moneymanager.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.zive.moneymanager.core.CurrencyOption
import com.zive.moneymanager.core.PeriodType
import com.zive.moneymanager.data.prefs.ThemeMode
import com.zive.moneymanager.ui.components.SectionHeader
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

/** The "More" tab: everything that configures the app plus links to management screens. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    message: String?,
    onMessageShown: () -> Unit,
    onOpenAccounts: () -> Unit,
    onOpenCategories: () -> Unit,
    onOpenRecurring: () -> Unit,
    onCurrencyChange: (CurrencyOption) -> Unit,
    onThemeChange: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onWeekStartChange: (DayOfWeek) -> Unit,
    onMonthStartDayChange: (Int) -> Unit,
    onDefaultPeriodChange: (PeriodType) -> Unit,
    onExport: (android.net.Uri) -> Unit,
    onImport: (android.net.Uri) -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val settings = state.settings

    // Storage Access Framework keeps the app free of storage permissions.
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri -> uri?.let(onExport) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(onImport) }

    LaunchedEffect(message) {
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            onMessageShown()
        }
    }

    Scaffold(
        // The root scaffold already inset for the status and navigation bars;
        // taking them again here would double the gap under the top bar.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("More") }, windowInsets = WindowInsets(0, 0, 0, 0)) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 96.dp),
        ) {
            item { SectionHeader("Manage") }
            item {
                NavRow(
                    icon = Icons.Filled.AccountBalanceWallet,
                    title = "Accounts",
                    subtitle = "${state.accountCount} accounts",
                    onClick = onOpenAccounts,
                )
            }
            item {
                NavRow(
                    icon = Icons.Filled.Category,
                    title = "Categories",
                    subtitle = "${state.categoryCount} categories and subcategories",
                    onClick = onOpenCategories,
                )
            }
            item {
                NavRow(
                    icon = Icons.Filled.Autorenew,
                    title = "Scheduled payments",
                    subtitle = if (state.recurringCount == 0) {
                        "Automate salary, rent, loan instalments"
                    } else {
                        "${state.recurringCount} scheduled"
                    },
                    onClick = onOpenRecurring,
                )
            }

            item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }

            item { SectionHeader("Currency") }
            item {
                ChipRow(
                    options = CurrencyOption.entries,
                    isSelected = { it == settings.currency },
                    label = { "${it.symbol} ${it.code}" },
                    onSelect = onCurrencyChange,
                )
            }

            item { SectionHeader("Periods") }
            item {
                SettingLabel("Default view", "Which period the app opens on")
                ChipRow(
                    options = PeriodType.selectable,
                    isSelected = { it == settings.defaultPeriod },
                    label = { it.label },
                    onSelect = onDefaultPeriodChange,
                )
            }
            item {
                SettingLabel("Week starts on", null)
                ChipRow(
                    options = DayOfWeek.entries,
                    isSelected = { it == settings.weekStart },
                    label = { it.getDisplayName(TextStyle.SHORT, Locale.getDefault()) },
                    onSelect = onWeekStartChange,
                )
            }
            item {
                SettingLabel(
                    "Month starts on day ${settings.monthStartDay}",
                    "Set this to your pay day to line periods up with your salary",
                )
                ChipRow(
                    options = (1..28).toList(),
                    isSelected = { it == settings.monthStartDay },
                    label = { it.toString() },
                    onSelect = onMonthStartDayChange,
                )
            }

            item { SectionHeader("Appearance") }
            item {
                ChipRow(
                    options = ThemeMode.entries,
                    isSelected = { it == settings.themeMode },
                    label = { it.label },
                    onSelect = onThemeChange,
                )
            }
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Use wallpaper colours", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Android 12 and newer",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = settings.dynamicColor,
                        onCheckedChange = onDynamicColorChange,
                    )
                }
            }

            item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }

            item { SectionHeader("Backup") }
            item {
                NavRow(
                    icon = Icons.Filled.FileDownload,
                    title = "Export to CSV",
                    subtitle = "${state.transactionCount} records",
                    onClick = { exportLauncher.launch("money-manager-export.csv") },
                )
            }
            item {
                NavRow(
                    icon = Icons.Filled.FileUpload,
                    title = "Import from CSV",
                    subtitle = "Adds records; accounts and categories are created as needed",
                    onClick = {
                        importLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "text/plain", "*/*"))
                    },
                )
            }

            item {
                Text(
                    text = "Money Manager · all data stays on this device",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
}

@Composable
private fun SettingLabel(title: String, subtitle: String?) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        if (subtitle != null) {
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun <T> ChipRow(
    options: List<T>,
    isSelected: (T) -> Boolean,
    label: (T) -> String,
    onSelect: (T) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            FilterChip(
                selected = isSelected(option),
                onClick = { onSelect(option) },
                label = { Text(label(option)) },
            )
        }
    }
}

@Composable
private fun NavRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp),
        )
        Column(
            Modifier
                .weight(1f)
                .padding(horizontal = 14.dp)
        ) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
