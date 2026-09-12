package com.zive.moneymanager.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.zive.moneymanager.data.db.dao.AccountDao
import com.zive.moneymanager.data.db.dao.BudgetDao
import com.zive.moneymanager.data.db.dao.CategoryDao
import com.zive.moneymanager.data.db.dao.RecurringDao
import com.zive.moneymanager.data.db.dao.TransactionDao
import com.zive.moneymanager.data.db.entity.Account
import com.zive.moneymanager.data.db.entity.Budget
import com.zive.moneymanager.data.db.entity.Category
import com.zive.moneymanager.data.db.entity.RecurringRule
import com.zive.moneymanager.data.db.entity.TransactionEntity

@Database(
    entities = [
        Account::class,
        Category::class,
        TransactionEntity::class,
        Budget::class,
        RecurringRule::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun recurringDao(): RecurringDao

    companion object {
        private const val NAME = "money-manager.db"

        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: build(context.applicationContext).also { instance = it }
            }

        private fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, NAME)
                .addCallback(object : Callback() {
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        // Room only honours foreign keys when they are switched on per connection.
                        db.execSQL("PRAGMA foreign_keys = ON")
                    }
                })
                .build()
    }
}
