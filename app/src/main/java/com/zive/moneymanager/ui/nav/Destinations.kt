package com.zive.moneymanager.ui.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.ui.graphics.vector.ImageVector

object Routes {
    const val OVERVIEW = "overview"
    const val TRANSACTIONS = "transactions"
    const val STATS = "stats"
    const val INVENTORY = "inventory"
    const val MORE = "more"

    const val BUDGETS = "budgets"
    const val ACCOUNTS = "accounts"
    const val CATEGORIES = "categories"
    const val RECURRING = "recurring"

    /** id 0 means "new transaction". */
    const val EDITOR = "editor/{txId}"
    fun editor(txId: Long = 0L) = "editor/$txId"

    /** id 0 means "new inventory item". */
    const val ITEM_EDITOR = "item/{itemId}"
    fun itemEditor(itemId: Long = 0L) = "item/$itemId"
}

/**
 * The five bottom-bar destinations. Budgets is reached from the overview and
 * from More rather than taking a slot here — Material caps the bar at five, and
 * the expiry checklist is the thing that needs checking daily.
 */
enum class TopLevel(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    OVERVIEW(Routes.OVERVIEW, "Overview", Icons.Filled.Dashboard),
    TRANSACTIONS(Routes.TRANSACTIONS, "Records", Icons.AutoMirrored.Filled.ListAlt),
    STATS(Routes.STATS, "Stats", Icons.Filled.DonutLarge),
    INVENTORY(Routes.INVENTORY, "Inventory", Icons.Filled.Inventory2),
    MORE(Routes.MORE, "More", Icons.Filled.MoreHoriz),
}
