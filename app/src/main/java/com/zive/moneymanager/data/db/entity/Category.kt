package com.zive.moneymanager.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "categories",
    indices = [Index("type"), Index("parentId")],
)
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** INCOME or EXPENSE. TRANSFER transactions carry no category. */
    val type: TxType,
    /** Non-null for a subcategory, e.g. "Flights" under "Travel". */
    val parentId: Long? = null,
    val colorHex: String = "#64748B",
    val iconKey: String = "category",
    /** Only meaningful when [type] is INCOME. */
    val incomeKind: IncomeKind = IncomeKind.NONE,
    val archived: Boolean = false,
    val sortOrder: Int = 0,
)
