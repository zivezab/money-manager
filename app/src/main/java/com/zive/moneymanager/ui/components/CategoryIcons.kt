package com.zive.moneymanager.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.House
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.LocalGroceryStore
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Stable string keys map to vectors here so the database never stores a resource
 * id that could shift between builds.
 */
object CategoryIcons {

    private val map: Map<String, ImageVector> = mapOf(
        // Income
        "work" to Icons.Filled.Work,
        "bonus" to Icons.Filled.CardGiftcard,
        "laptop" to Icons.Filled.Laptop,
        "support" to Icons.Filled.VolunteerActivism,
        "govt" to Icons.Filled.AccountBalance,
        "invest" to Icons.AutoMirrored.Filled.TrendingUp,
        "percent" to Icons.Filled.Percent,
        "apartment" to Icons.Filled.Apartment,
        "gift" to Icons.Filled.Redeem,
        "refund" to Icons.AutoMirrored.Filled.Undo,
        "payments" to Icons.Filled.Payments,
        // Expense
        "home" to Icons.Filled.Home,
        "house" to Icons.Filled.House,
        "flight" to Icons.Filled.Flight,
        "restaurant" to Icons.Filled.Restaurant,
        "groceries" to Icons.Filled.LocalGroceryStore,
        "car" to Icons.Filled.DirectionsCar,
        "utilities" to Icons.Filled.Bolt,
        "health" to Icons.Filled.LocalHospital,
        "shield" to Icons.Filled.Shield,
        "school" to Icons.Filled.School,
        "shopping" to Icons.Filled.ShoppingBag,
        "movie" to Icons.Filled.Movie,
        "subscription" to Icons.Filled.Subscriptions,
        "spa" to Icons.Filled.Spa,
        "kids" to Icons.Filled.ChildCare,
        "pets" to Icons.Filled.Pets,
        "receipt" to Icons.Filled.Receipt,
        "tax" to Icons.Filled.Paid,
        "piggy" to Icons.Filled.Savings,
        "category" to Icons.Filled.Category,
        // Accounts
        "wallet" to Icons.Filled.AccountBalanceWallet,
        "bank" to Icons.Filled.AccountBalance,
        "card" to Icons.Filled.CreditCard,
        "savings" to Icons.Filled.Savings,
        "transfer" to Icons.Filled.SwapHoriz,
    )

    /** Order shown in the icon picker. */
    val pickerKeys: List<String> = map.keys.toList()

    operator fun get(key: String?): ImageVector = map[key] ?: Icons.Filled.Category
}

/** Colour swatches offered when creating a category or account. */
val CategorySwatches: List<String> = listOf(
    "#DC2626", "#EF4444", "#F43F5E", "#EC4899", "#DB2777", "#A855F7",
    "#8B5CF6", "#7C3AED", "#6366F1", "#3B82F6", "#0EA5E9", "#06B6D4",
    "#14B8A6", "#0D9488", "#10B981", "#059669", "#84CC16", "#EAB308",
    "#F59E0B", "#F97316", "#FB923C", "#A16207", "#78716C", "#64748B",
)
