package com.zive.moneymanager.data.seed

import com.zive.moneymanager.data.db.entity.Account
import com.zive.moneymanager.data.db.entity.AccountType
import com.zive.moneymanager.data.db.entity.Category
import com.zive.moneymanager.data.db.entity.IncomeKind
import com.zive.moneymanager.data.db.entity.TxType

/**
 * What a brand new install starts with. Enough structure to record a first
 * transaction immediately, all of it editable or deletable afterwards.
 */
object DefaultData {

    val accounts: List<Account> = listOf(
        Account(name = "Cash", type = AccountType.CASH, colorHex = "#10B981", iconKey = "wallet", sortOrder = 0),
        Account(name = "Bank Account", type = AccountType.BANK, colorHex = "#3B82F6", iconKey = "bank", sortOrder = 1),
        Account(name = "Credit Card", type = AccountType.CARD, colorHex = "#F43F5E", iconKey = "card", sortOrder = 2),
        Account(name = "Savings", type = AccountType.SAVINGS, colorHex = "#8B5CF6", iconKey = "savings", sortOrder = 3),
    )

    /** Top-level categories. [Seed.children] become subcategories of their parent. */
    data class Seed(
        val name: String,
        val color: String,
        val icon: String,
        val incomeKind: IncomeKind = IncomeKind.NONE,
        val children: List<String> = emptyList(),
    )

    val incomeSeeds: List<Seed> = listOf(
        Seed("Salary", "#059669", "work", IncomeKind.JOB, listOf("Base pay", "Overtime", "Allowance")),
        Seed("Bonus", "#10B981", "bonus", IncomeKind.JOB, listOf("Annual bonus", "Commission")),
        Seed("Freelance", "#14B8A6", "laptop", IncomeKind.JOB, listOf("Contract work", "Consulting")),
        Seed("Unemployment Benefit", "#F59E0B", "support", IncomeKind.NON_JOB),
        Seed("Government Support", "#D97706", "govt", IncomeKind.NON_JOB, listOf("Grant", "Tax rebate")),
        Seed("Investment", "#6366F1", "invest", IncomeKind.NON_JOB, listOf("Dividends", "Capital gains")),
        Seed("Interest", "#818CF8", "percent", IncomeKind.NON_JOB),
        Seed("Rental Income", "#0EA5E9", "apartment", IncomeKind.NON_JOB),
        Seed("Gift", "#EC4899", "gift", IncomeKind.NON_JOB),
        Seed("Refund", "#64748B", "refund", IncomeKind.NON_JOB),
        Seed("Other Income", "#94A3B8", "payments", IncomeKind.NON_JOB),
    )

    val expenseSeeds: List<Seed> = listOf(
        Seed("Home Loan", "#DC2626", "home", children = listOf("Principal", "Interest", "Property tax")),
        Seed("Rent", "#EF4444", "house"),
        Seed("Travel", "#0EA5E9", "flight", children = listOf("Flights", "Hotels", "Tours", "Travel insurance")),
        Seed("Food & Dining", "#F97316", "restaurant", children = listOf("Restaurants", "Coffee", "Takeaway")),
        Seed("Groceries", "#84CC16", "groceries"),
        Seed("Transport", "#0891B2", "car", children = listOf("Fuel", "Public transport", "Taxi & ride-hailing", "Parking", "Car loan")),
        Seed("Utilities", "#EAB308", "utilities", children = listOf("Electricity", "Water", "Gas", "Internet", "Mobile")),
        Seed("Healthcare", "#E11D48", "health", children = listOf("Doctor", "Pharmacy", "Dental")),
        Seed("Insurance", "#7C3AED", "shield", children = listOf("Health", "Life", "Home", "Motor")),
        Seed("Education", "#2563EB", "school", children = listOf("Tuition", "Books", "Courses")),
        Seed("Shopping", "#DB2777", "shopping", children = listOf("Clothing", "Electronics", "Household")),
        Seed("Entertainment", "#A855F7", "movie", children = listOf("Streaming", "Events", "Games")),
        Seed("Subscriptions", "#8B5CF6", "subscription"),
        Seed("Personal Care", "#F472B6", "spa", children = listOf("Haircut", "Gym", "Cosmetics")),
        Seed("Kids", "#FB923C", "kids", children = listOf("Childcare", "School fees", "Toys")),
        Seed("Pets", "#A16207", "pets"),
        Seed("Fees & Charges", "#78716C", "receipt", children = listOf("Bank fees", "Late fees")),
        Seed("Taxes", "#475569", "tax"),
        Seed("Savings & Investments", "#0D9488", "piggy"),
        Seed("Gifts & Donations", "#F43F5E", "gift"),
        Seed("Other", "#94A3B8", "category"),
    )

    /** Flattens the seeds into parent rows; children are inserted afterwards
     *  once the parent ids are known. */
    fun parentCategories(): List<Pair<Seed, Category>> {
        val income = incomeSeeds.mapIndexed { i, s ->
            s to Category(
                name = s.name,
                type = TxType.INCOME,
                colorHex = s.color,
                iconKey = s.icon,
                incomeKind = s.incomeKind,
                sortOrder = i,
            )
        }
        val expense = expenseSeeds.mapIndexed { i, s ->
            s to Category(
                name = s.name,
                type = TxType.EXPENSE,
                colorHex = s.color,
                iconKey = s.icon,
                incomeKind = IncomeKind.NONE,
                sortOrder = i,
            )
        }
        return income + expense
    }
}
