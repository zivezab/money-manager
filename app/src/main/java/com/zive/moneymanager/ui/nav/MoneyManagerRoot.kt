package com.zive.moneymanager.ui.nav

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.zive.moneymanager.data.prefs.AppSettings
import com.zive.moneymanager.ui.accounts.AccountsScreen
import com.zive.moneymanager.ui.accounts.AccountsViewModel
import com.zive.moneymanager.ui.budgets.BudgetsScreen
import com.zive.moneymanager.ui.budgets.BudgetsViewModel
import com.zive.moneymanager.ui.categories.CategoriesScreen
import com.zive.moneymanager.ui.categories.CategoriesViewModel
import com.zive.moneymanager.ui.editor.TxEditorScreen
import com.zive.moneymanager.ui.editor.TxEditorViewModel
import com.zive.moneymanager.ui.overview.OverviewScreen
import com.zive.moneymanager.ui.overview.OverviewViewModel
import com.zive.moneymanager.ui.recurring.RecurringScreen
import com.zive.moneymanager.ui.recurring.RecurringViewModel
import com.zive.moneymanager.ui.settings.SettingsScreen
import com.zive.moneymanager.ui.settings.SettingsViewModel
import com.zive.moneymanager.ui.stats.StatsScreen
import com.zive.moneymanager.ui.stats.StatsViewModel
import com.zive.moneymanager.ui.transactions.TransactionsScreen
import com.zive.moneymanager.ui.transactions.TransactionsViewModel

/** Hosts the bottom navigation, the add-record FAB, and every destination. */
@Composable
fun MoneyManagerRoot(settings: AppSettings) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val topLevelRoutes = TopLevel.entries.map { it.route }.toSet()
    val showChrome = currentRoute in topLevelRoutes
    val showFab = currentRoute == Routes.OVERVIEW || currentRoute == Routes.TRANSACTIONS

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = showChrome,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                NavigationBar {
                    TopLevel.entries.forEach { destination ->
                        NavigationBarItem(
                            selected = currentRoute == destination.route,
                            onClick = {
                                if (currentRoute != destination.route) {
                                    navController.navigate(destination.route) {
                                        // Single copy of each tab, state preserved between switches.
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(destination.icon, contentDescription = destination.label)
                            },
                            label = { Text(destination.label) },
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = showFab,
                enter = scaleIn(),
                exit = scaleOut(),
            ) {
                FloatingActionButton(onClick = { navController.navigate(Routes.editor()) }) {
                    Icon(Icons.Filled.Add, contentDescription = "Add record")
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.OVERVIEW,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            composable(Routes.OVERVIEW) {
                val vm: OverviewViewModel = viewModel(factory = OverviewViewModel.Factory)
                val state by vm.state.collectAsStateWithLifecycle()
                OverviewScreen(
                    state = state,
                    onPeriodTypeChange = vm.periods::setType,
                    onPrevious = { vm.periods.previous(state.settings.periodConfig) },
                    onNext = { vm.periods.next(state.settings.periodConfig) },
                    onJumpToToday = vm.periods::jumpToToday,
                    onOpenTransaction = { navController.navigate(Routes.editor(it)) },
                    onSeeAllRecords = { navController.navigateTab(Routes.TRANSACTIONS) },
                    onOpenBudgets = { navController.navigateTab(Routes.BUDGETS) },
                    onOpenAccounts = { navController.navigate(Routes.ACCOUNTS) },
                    onOpenStats = { navController.navigateTab(Routes.STATS) },
                )
            }

            composable(Routes.TRANSACTIONS) {
                val vm: TransactionsViewModel = viewModel(factory = TransactionsViewModel.Factory)
                val state by vm.state.collectAsStateWithLifecycle()
                TransactionsScreen(
                    state = state,
                    onPeriodTypeChange = vm.periods::setType,
                    onPrevious = { vm.periods.previous(state.settings.periodConfig) },
                    onNext = { vm.periods.next(state.settings.periodConfig) },
                    onJumpToToday = vm.periods::jumpToToday,
                    onTypeFilter = vm::setType,
                    onAccountFilter = vm::setAccount,
                    onQueryChange = vm::setQuery,
                    onClearFilters = vm::clearFilters,
                    onOpenTransaction = { navController.navigate(Routes.editor(it)) },
                )
            }

            composable(Routes.STATS) {
                val vm: StatsViewModel = viewModel(factory = StatsViewModel.Factory)
                val state by vm.state.collectAsStateWithLifecycle()
                StatsScreen(
                    state = state,
                    onPeriodTypeChange = vm.periods::setType,
                    onPrevious = { vm.periods.previous(state.settings.periodConfig) },
                    onNext = { vm.periods.next(state.settings.periodConfig) },
                    onJumpToToday = vm.periods::jumpToToday,
                    onSideChange = vm::setSide,
                    onDrillInto = vm::drillInto,
                )
            }

            composable(Routes.BUDGETS) {
                val vm: BudgetsViewModel = viewModel(factory = BudgetsViewModel.Factory)
                val state by vm.state.collectAsStateWithLifecycle()
                BudgetsScreen(
                    state = state,
                    onSave = vm::save,
                    onDelete = vm::delete,
                )
            }

            composable(Routes.MORE) {
                val vm: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
                val state by vm.state.collectAsStateWithLifecycle()
                val message by vm.message.collectAsStateWithLifecycle()
                val context = LocalContext.current
                SettingsScreen(
                    state = state,
                    message = message,
                    onMessageShown = vm::clearMessage,
                    onOpenAccounts = { navController.navigate(Routes.ACCOUNTS) },
                    onOpenCategories = { navController.navigate(Routes.CATEGORIES) },
                    onOpenRecurring = { navController.navigate(Routes.RECURRING) },
                    onCurrencyChange = vm::setCurrency,
                    onThemeChange = vm::setThemeMode,
                    onDynamicColorChange = vm::setDynamicColor,
                    onWeekStartChange = vm::setWeekStart,
                    onMonthStartDayChange = vm::setMonthStartDay,
                    onDefaultPeriodChange = vm::setDefaultPeriod,
                    onExport = { vm.exportTo(context, it) },
                    onImport = { vm.importFrom(context, it) },
                )
            }

            composable(Routes.ACCOUNTS) {
                val vm: AccountsViewModel = viewModel(factory = AccountsViewModel.Factory)
                val state by vm.state.collectAsStateWithLifecycle()
                AccountsScreen(
                    state = state,
                    onBack = { navController.popBackStack() },
                    onSave = vm::save,
                    onDelete = vm::delete,
                )
            }

            composable(Routes.CATEGORIES) {
                val vm: CategoriesViewModel = viewModel(factory = CategoriesViewModel.Factory)
                val state by vm.state.collectAsStateWithLifecycle()
                CategoriesScreen(
                    state = state,
                    onBack = { navController.popBackStack() },
                    onSave = vm::save,
                    onDelete = vm::delete,
                )
            }

            composable(Routes.RECURRING) {
                val vm: RecurringViewModel = viewModel(factory = RecurringViewModel.Factory)
                val state by vm.state.collectAsStateWithLifecycle()
                RecurringScreen(
                    state = state,
                    onBack = { navController.popBackStack() },
                    onSave = { draft ->
                        vm.save(
                            id = draft.id,
                            name = draft.name,
                            type = draft.type,
                            amountMinor = draft.amountMinor,
                            accountId = draft.accountId,
                            toAccountId = draft.toAccountId,
                            categoryId = draft.categoryId,
                            note = draft.note,
                            counterparty = draft.counterparty,
                            unit = draft.unit,
                            interval = draft.interval,
                            startDate = draft.startDate,
                            endDate = draft.endDate,
                            enabled = draft.enabled,
                        )
                    },
                    onToggle = vm::setEnabled,
                    onDelete = vm::delete,
                )
            }

            composable(
                route = Routes.EDITOR,
                arguments = listOf(navArgument("txId") { type = NavType.LongType }),
            ) { entry ->
                val txId = entry.arguments?.getLong("txId") ?: 0L
                val vm: TxEditorViewModel = viewModel(factory = TxEditorViewModel.Factory)
                val state by vm.state.collectAsStateWithLifecycle()
                val saved by vm.saved.collectAsStateWithLifecycle()

                LaunchedEffect(txId) { vm.load(txId) }
                LaunchedEffect(saved) { if (saved) navController.popBackStack() }

                TxEditorScreen(
                    state = state,
                    onBack = { navController.popBackStack() },
                    onTypeChange = vm::setType,
                    onDigit = vm::appendDigit,
                    onDecimal = vm::appendDecimalPoint,
                    onBackspace = vm::backspace,
                    onClearAmount = vm::clearAmount,
                    onDateChange = vm::setDate,
                    onAccountChange = vm::setAccount,
                    onToAccountChange = vm::setToAccount,
                    onCategoryChange = vm::setCategory,
                    onNoteChange = vm::setNote,
                    onCounterpartyChange = vm::setCounterparty,
                    onSave = vm::save,
                    onDelete = vm::delete,
                )
            }
        }
    }

    // Settings are read at the root so the theme can react; nothing else needs them here.
    LaunchedEffect(settings.defaultPeriod) { /* theme + period defaults applied by view models */ }
}

/** Switches tabs while keeping the single-copy behaviour of the bottom bar. */
private fun androidx.navigation.NavHostController.navigateTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
