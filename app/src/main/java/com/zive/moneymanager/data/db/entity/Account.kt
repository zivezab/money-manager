package com.zive.moneymanager.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: AccountType = AccountType.BANK,
    /** Balance the account already held before the first recorded transaction. */
    val openingBalanceMinor: Long = 0,
    val colorHex: String = "#3B82F6",
    val iconKey: String = "wallet",
    val includeInTotal: Boolean = true,
    val archived: Boolean = false,
    val sortOrder: Int = 0,
    val note: String = "",
)
