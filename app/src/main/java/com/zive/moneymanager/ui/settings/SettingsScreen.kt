package com.zive.moneymanager.ui.settings

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
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
import androidx.compose.material.icons.filled.Savings
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.zive.moneymanager.core.CurrencyOption
import com.zive.moneymanager.core.PeriodType
import com.zive.moneymanager.data.prefs.ThemeMode
import com.zive.moneymanager.data.reminder.ExpiryNotifier
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
    onOpenBudgets: () -> Unit,
    onOpenAccounts: () -> Unit,
    onOpenCategories: () -> Unit,
    onOpenRecurring: () -> Unit,
    onCurrencyChange: (CurrencyOption) -> Unit,
    onThemeChange: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onWeekStartChange: (DayOfWeek) -> Unit,
    onMonthStartDayChange: (Int) -> Unit,
    onDefaultPeriodChange: (PeriodType) -> Unit,
    onExpiryHorizonChange: (Int) -> Unit,
    onExpiryReminderEnabledChange: (Boolean) -> Unit,
    onExpiryReminderHourChange: (Int) -> Unit,
    onDeclutterMonthsChange: (Int) -> Unit,
    onExport: (android.net.Uri) -> Unit,
    onImport: (android.net.Uri) -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val settings = state.settings
    val context = LocalContext.current

    // Android 13 introduced a runtime permission for notifications. Ask for it at
    // the moment the user switches the reminder on, which is when the reason for
    // asking is obvious, and only enable the reminder if it is actually granted.
    val notificationsAllowed = remember(settings.expiryReminderEnabled) {
        ExpiryNotifier.canPost(context)
    }
    val requestNotifications = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> onExpiryReminderEnabledChange(granted) }

    fun toggleReminder(enabled: Boolean) {
        when {
            !enabled -> onExpiryReminderEnabledChange(false)
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ->
                onExpiryReminderEnabledChange(true)
            ExpiryNotifier.canPost(context) -> onExpiryReminderEnabledChange(true)
            else -> requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

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
                    icon = Icons.Filled.Savings,
                    title = "Budgets",
                    subtitle = "Spending caps by category or overall",
                    onClick = onOpenBudgets,
                )
            }
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


            item { SectionHeader("Inventory") }
            item {
                SettingLabel(
                    "Expiring soon means ${settings.expiryHorizonDays} days ahead",
                    "How far ahead the overview and the reminder look",
                )
                ChipRow(
                    options = listOf(1, 2, 3, 5, 7, 10, 14, 21, 30),
                    isSelected = { it == settings.expiryHorizonDays },
                    label = { if (it == 1) "1 day" else "$it days" },
                    onSelect = onExpiryHorizonChange,
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
                        Text("Daily expiry reminder", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "A notification when something is about to go off",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = settings.expiryReminderEnabled,
                        onCheckedChange = ::toggleReminder,
                    )
                }
            }
            if (settings.expiryReminderEnabled && !notificationsAllowed) {
                item {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        Text(
                            "Notifications are switched off for this app, so the reminder cannot appear.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                        TextButton(
                            onClick = {
                                context.startActivity(
                                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                    }
                                )
                            },
                            contentPadding = PaddingValues(0.dp),
                        ) { Text("Open notification settings") }
                    }
                }
            }

            if (settings.expiryReminderEnabled) {
                item {
                    SettingLabel("Remind me at ${"%02d".format(settings.expiryReminderHour)}:00", null)
                    ChipRow(
                        options = (6..22).toList(),
                        isSelected = { it == settings.expiryReminderHour },
                        label = { "%02d:00".format(it) },
                        onSelect = onExpiryReminderHourChange,
                    )
                }
            }
            item {
                SettingLabel(
                    "Flag things untouched for ${settings.declutterIdleMonths} months",
                    "Drives the declutter suggestions",
                )
                ChipRow(
                    options = listOf(1, 2, 3, 6, 9, 12, 18, 24),
                    isSelected = { it == settings.declutterIdleMonths },
                    label = { if (it == 1) "1 month" else "$it months" },
                    onSelect = onDeclutterMonthsChange,
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
