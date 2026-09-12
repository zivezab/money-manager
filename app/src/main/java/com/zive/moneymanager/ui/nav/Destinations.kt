package com.zive.moneymanager.ui.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Savings
import androidx.compose.ui.graphics.vector.ImageVector

object Routes {
    const val OVERVIEW = "overview"
    const val TRANSACTIONS = "transactions"
    const val STATS = "stats"
    const val BUDGETS = "budgets"
    const val MORE = "more"

    const val ACCOUNTS = "accounts"
    const val CATEGORIES = "categories"
    const val RECURRING = "recurring"
    const val SETTINGS = "settings"

    /** id 0 means "new transaction". */
    const val EDITOR = "editor/{txId}"
    fun editor(txId: Long = 0L) = "editor/$txId"
}

enum class TopLevel(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    OVERVIEW(Routes.OVERVIEW, "Overview", Icons.Filled.Dashboard),
    TRANSACTIONS(Routes.TRANSACTIONS, "Records", Icons.AutoMirrored.Filled.ListAlt),
    STATS(Routes.STATS, "Stats", Icons.Filled.DonutLarge),
    BUDGETS(Routes.BUDGETS, "Budgets", Icons.Filled.Savings),
    MORE(Routes.MORE, "More", Icons.Filled.MoreHoriz),
}
