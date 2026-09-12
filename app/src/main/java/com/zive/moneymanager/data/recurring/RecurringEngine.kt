package com.zive.moneymanager.data.recurring

import com.zive.moneymanager.data.db.entity.RecurrenceUnit
import com.zive.moneymanager.data.db.entity.RecurringRule
import com.zive.moneymanager.data.db.entity.TransactionEntity
import com.zive.moneymanager.data.repo.FinanceRepository
import java.time.LocalDate

/**
 * Posts scheduled transactions that have come due. Runs on app start rather than
 * from a background worker, so an app that has not been opened for three months
 * still catches up all three months of instalments the moment it is launched.
 */
class RecurringEngine(private val repo: FinanceRepository) {

    /** Guards against a corrupt rule spinning forever. */
    private val maxOccurrencesPerRun = 400

    suspend fun postDue(today: LocalDate = LocalDate.now()): Int {
        val todayEpoch = today.toEpochDay()
        val due = repo.recurringDue(todayEpoch)
        var posted = 0

        for (rule in due) {
            val rows = mutableListOf<TransactionEntity>()
            var next = LocalDate.ofEpochDay(rule.nextRunEpochDay)
            var guard = 0

            while (next.toEpochDay() <= todayEpoch && guard < maxOccurrencesPerRun) {
                val endsAt = rule.endEpochDay
                if (endsAt != null && next.toEpochDay() > endsAt) break

                rows += TransactionEntity(
                    type = rule.type,
                    amountMinor = rule.amountMinor,
                    dateEpochDay = next.toEpochDay(),
                    accountId = rule.accountId,
                    toAccountId = rule.toAccountId,
                    categoryId = rule.categoryId,
                    note = rule.note,
                    counterparty = rule.counterparty,
                    recurringRuleId = rule.id,
                )
                next = advance(rule, next)
                guard++
            }

            if (rows.isNotEmpty()) {
                repo.postTransactions(rows)
                posted += rows.size
            }

            val finished = rule.endEpochDay != null && next.toEpochDay() > rule.endEpochDay
            repo.upsertRecurring(
                rule.copy(
                    nextRunEpochDay = next.toEpochDay(),
                    enabled = rule.enabled && !finished,
                )
            )
        }
        return posted
    }

    companion object {

        /** The occurrence that follows [from] for this rule. */
        fun advance(rule: RecurringRule, from: LocalDate): LocalDate {
            val n = rule.interval.coerceAtLeast(1).toLong()
            return when (rule.unit) {
                RecurrenceUnit.DAY -> from.plusDays(n)
                RecurrenceUnit.WEEK -> from.plusWeeks(n)
                RecurrenceUnit.MONTH -> alignDayOfMonth(rule, from.plusMonths(n))
                RecurrenceUnit.QUARTER -> alignDayOfMonth(rule, from.plusMonths(3 * n))
                RecurrenceUnit.YEAR -> from.plusYears(n)
            }
        }

        /**
         * A rule that starts on the 31st should keep landing on month-end rather
         * than drifting to the 28th after passing through February, so the day of
         * month is re-derived from the rule's own start date each time.
         */
        private fun alignDayOfMonth(rule: RecurringRule, candidate: LocalDate): LocalDate {
            val wanted = LocalDate.ofEpochDay(rule.startEpochDay).dayOfMonth
            return candidate.withDayOfMonth(minOf(wanted, candidate.lengthOfMonth()))
        }

        /** First occurrence on or after [start] for a freshly created rule. */
        fun firstRun(startEpochDay: Long): Long = startEpochDay
    }
}
