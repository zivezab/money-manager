package com.zive.moneymanager

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.zive.moneymanager.data.db.AppDatabase
import com.zive.moneymanager.data.db.MIGRATION_1_2
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Upgrading an install that already has a ledger must not touch that ledger, and
 * must leave a schema Room agrees with. [MigrationTestHelper] rebuilds version 1
 * from the exported schema, applies the migration, then validates the result
 * against version 2.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val dbName = "migration-test.db"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migrate1To2_keepsExistingDataAndAddsInventory() {
        helper.createDatabase(dbName, 1).use { db ->
            db.execSQL(
                """
                INSERT INTO accounts (id, name, type, openingBalanceMinor, colorHex, iconKey,
                                      includeInTotal, archived, sortOrder, note)
                VALUES (1, 'Bank', 'BANK', 100000, '#3B82F6', 'bank', 1, 0, 0, '')
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO categories (id, name, type, parentId, colorHex, iconKey,
                                        incomeKind, archived, sortOrder)
                VALUES (1, 'Groceries', 'EXPENSE', NULL, '#84CC16', 'groceries', 'NONE', 0, 0)
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO transactions (id, type, amountMinor, dateEpochDay, minuteOfDay,
                                          accountId, toAccountId, categoryId, note, counterparty,
                                          recurringRuleId, createdAt)
                VALUES (1, 'EXPENSE', 4550, 20700, 600, 1, NULL, 1, 'weekly shop', 'Market', NULL, 0)
                """.trimIndent()
            )
        }

        // Throws if the resulting schema differs from what version 2 declares.
        helper.runMigrationsAndValidate(dbName, 2, true, MIGRATION_1_2)

        val db = helper.runMigrationsAndValidate(dbName, 2, true, MIGRATION_1_2)

        db.query("SELECT amountMinor, note FROM transactions WHERE id = 1").use { cursor ->
            assertTrue("the pre-existing transaction survived", cursor.moveToFirst())
            assertEquals(4550L, cursor.getLong(0))
            assertEquals("weekly shop", cursor.getString(1))
        }

        db.query("SELECT COUNT(*) FROM inventory_items").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("the new table starts empty", 0, cursor.getInt(0))
        }

        // The new table must actually accept a row with the shape the entity declares.
        db.execSQL(
            """
            INSERT INTO inventory_items (name, nameKey, kind, categoryId, location, quantity, unit,
                                         purchaseEpochDay, expiryEpochDay, warrantyEndEpochDay,
                                         lastUsedEpochDay, unitCostMinor, transactionId, status,
                                         statusChangedEpochDay, note, createdAt, updatedAt)
            VALUES ('Milk', 'milk', 'CONSUMABLE', 1, 'Fridge', 2.0, 'BOTTLE',
                    20700, 20710, NULL, NULL, 320, 1, 'IN_STOCK', 20700, '', 0, 0)
            """.trimIndent()
        )

        db.query("SELECT name, quantity, expiryEpochDay FROM inventory_items").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Milk", cursor.getString(0))
            assertEquals(2.0, cursor.getDouble(1), 0.0001)
            assertEquals(20710L, cursor.getLong(2))
        }
    }

    @Test
    fun migrate1To2_clearsInventoryLinksWhenTheirParentsGo() {
        helper.createDatabase(dbName, 1).use { db ->
            db.execSQL(
                """
                INSERT INTO accounts (id, name, type, openingBalanceMinor, colorHex, iconKey,
                                      includeInTotal, archived, sortOrder, note)
                VALUES (1, 'Bank', 'BANK', 0, '#3B82F6', 'bank', 1, 0, 0, '')
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO categories (id, name, type, parentId, colorHex, iconKey,
                                        incomeKind, archived, sortOrder)
                VALUES (1, 'Groceries', 'EXPENSE', NULL, '#84CC16', 'groceries', 'NONE', 0, 0)
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO transactions (id, type, amountMinor, dateEpochDay, minuteOfDay,
                                          accountId, toAccountId, categoryId, note, counterparty,
                                          recurringRuleId, createdAt)
                VALUES (1, 'EXPENSE', 500, 20700, 0, 1, NULL, 1, '', '', NULL, 0)
                """.trimIndent()
            )
        }

        val db = helper.runMigrationsAndValidate(dbName, 2, true, MIGRATION_1_2)
        db.execSQL("PRAGMA foreign_keys = ON")
        db.execSQL(
            """
            INSERT INTO inventory_items (id, name, nameKey, kind, categoryId, location, quantity, unit,
                                         purchaseEpochDay, expiryEpochDay, warrantyEndEpochDay,
                                         lastUsedEpochDay, unitCostMinor, transactionId, status,
                                         statusChangedEpochDay, note, createdAt, updatedAt)
            VALUES (1, 'Milk', 'milk', 'CONSUMABLE', 1, 'Fridge', 1.0, 'BOTTLE',
                    20700, 20710, NULL, NULL, 320, 1, 'IN_STOCK', 20700, '', 0, 0)
            """.trimIndent()
        )

        // Deleting the receipt must not take the item with it.
        db.execSQL("DELETE FROM transactions WHERE id = 1")

        db.query("SELECT COUNT(*), transactionId FROM inventory_items").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("the item outlives its transaction", 1, cursor.getInt(0))
            assertTrue("its link was cleared", cursor.isNull(1))
        }
    }
}
