package com.zive.moneymanager.data.csv

import com.zive.moneymanager.core.Money
import com.zive.moneymanager.data.db.entity.Account
import com.zive.moneymanager.data.db.entity.AccountType
import com.zive.moneymanager.data.db.entity.Category
import com.zive.moneymanager.data.db.entity.TransactionEntity
import com.zive.moneymanager.data.db.entity.TxType
import com.zive.moneymanager.data.repo.FinanceRepository
import kotlinx.coroutines.flow.first
import java.io.BufferedReader
import java.io.InputStream
import java.io.OutputStream
import java.time.LocalDate

/**
 * CSV round-trip for backups and for moving data in from another app. The
 * exported header is also the import contract.
 */
object CsvSupport {

    private val HEADER = listOf(
        "date", "type", "amount", "currency", "account", "to_account",
        "category", "subcategory", "counterparty", "note",
    )

    // ---- Export ------------------------------------------------------------

    suspend fun export(repo: FinanceRepository, out: OutputStream, currencyCode: String) {
        val accounts = repo.observeAccounts().first().associateBy { it.id }
        val categories = repo.observeCategories().first().associateBy { it.id }
        val rows = repo.allTransactionsForExport()

        out.bufferedWriter().use { w ->
            w.appendLine(HEADER.joinToString(","))
            for (t in rows) {
                val category = t.categoryId?.let { categories[it] }
                val parent = category?.parentId?.let { categories[it] }
                val topName = parent?.name ?: category?.name.orEmpty()
                val subName = if (parent != null) category?.name.orEmpty() else ""

                w.appendLine(
                    listOf(
                        LocalDate.ofEpochDay(t.dateEpochDay).toString(),
                        t.type.name,
                        Money.toEditable(t.amountMinor),
                        currencyCode,
                        accounts[t.accountId]?.name.orEmpty(),
                        t.toAccountId?.let { accounts[it]?.name }.orEmpty(),
                        topName,
                        subName,
                        t.counterparty,
                        t.note,
                    ).joinToString(",") { escape(it) }
                )
            }
        }
    }

    private fun escape(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }

    // ---- Import ------------------------------------------------------------

    data class ImportResult(val imported: Int, val skipped: Int, val createdAccounts: Int, val createdCategories: Int)

    /**
     * Reads rows in the exported shape. Unknown accounts and categories are
     * created on the fly so a file from a previous install lands intact.
     */
    suspend fun import(repo: FinanceRepository, input: InputStream): ImportResult {
        val accountsByName = repo.observeAccounts().first()
            .associateBy { it.name.lowercase() }.toMutableMap()
        val categoriesByKey = repo.observeCategories().first()
            .associateBy { categoryKey(it.type, it.name) }.toMutableMap()

        var imported = 0
        var skipped = 0
        var createdAccounts = 0
        var createdCategories = 0
        val pending = mutableListOf<TransactionEntity>()

        input.bufferedReader().use { reader ->
            val header = parseLine(reader) ?: return ImportResult(0, 0, 0, 0)
            val index = header.mapIndexed { i, h -> h.trim().lowercase() to i }.toMap()

            fun field(cols: List<String>, name: String): String =
                index[name]?.let { cols.getOrNull(it) }?.trim().orEmpty()

            while (true) {
                val cols = parseLine(reader) ?: break
                if (cols.all { it.isBlank() }) continue

                val date = runCatching { LocalDate.parse(field(cols, "date")) }.getOrNull()
                val type = runCatching { TxType.valueOf(field(cols, "type").uppercase()) }.getOrNull()
                val amount = Money.parse(field(cols, "amount"))
                if (date == null || type == null || amount == null || amount == 0L) {
                    skipped++
                    continue
                }

                val accountName = field(cols, "account").ifBlank { "Imported" }
                val account = accountsByName.getOrPut(accountName.lowercase()) {
                    createdAccounts++
                    val id = repo.upsertAccount(Account(name = accountName, type = AccountType.OTHER))
                    Account(id = id, name = accountName, type = AccountType.OTHER)
                }

                val toAccountName = field(cols, "to_account")
                val toAccount = if (type == TxType.TRANSFER && toAccountName.isNotBlank()) {
                    accountsByName.getOrPut(toAccountName.lowercase()) {
                        createdAccounts++
                        val id = repo.upsertAccount(Account(name = toAccountName, type = AccountType.OTHER))
                        Account(id = id, name = toAccountName, type = AccountType.OTHER)
                    }
                } else {
                    null
                }

                var categoryId: Long? = null
                if (type != TxType.TRANSFER) {
                    val topName = field(cols, "category")
                    val subName = field(cols, "subcategory")
                    if (topName.isNotBlank()) {
                        val parent = categoriesByKey.getOrPut(categoryKey(type, topName)) {
                            createdCategories++
                            val id = repo.upsertCategory(Category(name = topName, type = type))
                            Category(id = id, name = topName, type = type)
                        }
                        categoryId = parent.id
                        if (subName.isNotBlank()) {
                            val child = categoriesByKey.getOrPut(categoryKey(type, subName)) {
                                createdCategories++
                                val id = repo.upsertCategory(
                                    Category(name = subName, type = type, parentId = parent.id, colorHex = parent.colorHex, iconKey = parent.iconKey)
                                )
                                Category(id = id, name = subName, type = type, parentId = parent.id)
                            }
                            categoryId = child.id
                        }
                    }
                }

                pending += TransactionEntity(
                    type = type,
                    amountMinor = kotlin.math.abs(amount),
                    dateEpochDay = date.toEpochDay(),
                    accountId = account.id,
                    toAccountId = toAccount?.id,
                    categoryId = categoryId,
                    counterparty = field(cols, "counterparty"),
                    note = field(cols, "note"),
                )
                imported++
            }
        }

        repo.postTransactions(pending)
        return ImportResult(imported, skipped, createdAccounts, createdCategories)
    }

    private fun categoryKey(type: TxType, name: String) = "${type.name}:${name.lowercase()}"

    /**
     * Reads one logical CSV record, which may span several physical lines when a
     * quoted field contains a newline. Returns null at end of input.
     */
    private fun parseLine(reader: BufferedReader): List<String>? {
        val fields = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var sawAnything = false

        while (true) {
            val ch = reader.read()
            if (ch == -1) {
                if (!sawAnything) return null
                fields += current.toString()
                return fields
            }
            sawAnything = true
            val c = ch.toChar()

            when {
                inQuotes && c == '"' -> {
                    reader.mark(1)
                    val nxt = reader.read()
                    if (nxt != -1 && nxt.toChar() == '"') {
                        current.append('"')
                    } else {
                        inQuotes = false
                        if (nxt != -1) reader.reset()
                    }
                }
                c == '"' -> inQuotes = true
                !inQuotes && c == ',' -> {
                    fields += current.toString()
                    current.setLength(0)
                }
                !inQuotes && (c == '\n') -> {
                    fields += current.toString()
                    return fields
                }
                !inQuotes && c == '\r' -> Unit
                else -> current.append(c)
            }
        }
    }
}
