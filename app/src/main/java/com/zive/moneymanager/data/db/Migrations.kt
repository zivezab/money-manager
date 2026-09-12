package com.zive.moneymanager.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Version 2 adds inventory. The table and its indices are written out by hand to
 * match exactly what Room generates for [com.zive.moneymanager.data.db.entity.InventoryItem];
 * the schema JSON under app/schemas is the reference if this ever needs changing.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `inventory_items` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `nameKey` TEXT NOT NULL,
                `kind` TEXT NOT NULL,
                `categoryId` INTEGER,
                `location` TEXT NOT NULL,
                `quantity` REAL NOT NULL,
                `unit` TEXT NOT NULL,
                `purchaseEpochDay` INTEGER,
                `expiryEpochDay` INTEGER,
                `warrantyEndEpochDay` INTEGER,
                `lastUsedEpochDay` INTEGER,
                `unitCostMinor` INTEGER,
                `transactionId` INTEGER,
                `status` TEXT NOT NULL,
                `statusChangedEpochDay` INTEGER,
                `note` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`)
                    ON UPDATE NO ACTION ON DELETE SET NULL,
                FOREIGN KEY(`transactionId`) REFERENCES `transactions`(`id`)
                    ON UPDATE NO ACTION ON DELETE SET NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_items_categoryId` ON `inventory_items` (`categoryId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_items_transactionId` ON `inventory_items` (`transactionId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_items_status` ON `inventory_items` (`status`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_items_expiryEpochDay` ON `inventory_items` (`expiryEpochDay`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_items_nameKey` ON `inventory_items` (`nameKey`)")
    }
}
