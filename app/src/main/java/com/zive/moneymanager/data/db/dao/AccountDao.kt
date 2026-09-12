package com.zive.moneymanager.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.zive.moneymanager.data.db.entity.Account
import com.zive.moneymanager.data.db.model.AccountBalance
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {

    @Query("SELECT * FROM accounts ORDER BY archived ASC, sortOrder ASC, name ASC")
    fun observeAll(): Flow<List<Account>>

    @Query("SELECT * FROM accounts WHERE archived = 0 ORDER BY sortOrder ASC, name ASC")
    fun observeActive(): Flow<List<Account>>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun byId(id: Long): Account?

    @Query("SELECT COUNT(*) FROM accounts")
    suspend fun count(): Int

    /**
     * Opening balance, plus everything paid into the account, minus everything
     * paid out of it. Transfers count twice: out of the source, into the target.
     */
    @Query(
        """
        SELECT a.id AS accountId,
               a.name AS name,
               a.type AS type,
               a.colorHex AS colorHex,
               a.iconKey AS iconKey,
               a.archived AS archived,
               a.includeInTotal AS includeInTotal,
               a.openingBalanceMinor
                 + IFNULL((
                     SELECT SUM(CASE t.type
                                  WHEN 'INCOME' THEN t.amountMinor
                                  ELSE -t.amountMinor
                                END)
                     FROM transactions t WHERE t.accountId = a.id
                   ), 0)
                 + IFNULL((
                     SELECT SUM(t2.amountMinor)
                     FROM transactions t2
                     WHERE t2.toAccountId = a.id AND t2.type = 'TRANSFER'
                   ), 0)
               AS balanceMinor
        FROM accounts a
        ORDER BY a.archived ASC, a.sortOrder ASC, a.name ASC
        """
    )
    fun observeBalances(): Flow<List<AccountBalance>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: Account): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(accounts: List<Account>): List<Long>

    @Update
    suspend fun update(account: Account)

    @Delete
    suspend fun delete(account: Account)
}
