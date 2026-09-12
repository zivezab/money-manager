package com.zive.moneymanager.data.repo

import com.zive.moneymanager.core.Period
import com.zive.moneymanager.core.PeriodConfig
import com.zive.moneymanager.core.PeriodType
import com.zive.moneymanager.data.db.entity.Budget
import com.zive.moneymanager.data.db.entity.BudgetPeriod
import java.time.LocalDate

/** A budget resolved against a concrete date range, with spend filled in. */
data class BudgetProgress(
    val budget: Budget,
    val categoryName: String,
    val colorHex: String,
    val iconKey: String,
    val period: Period,
    val spentMinor: Long,
) {
    val remainingMinor: Long get() = budget.amountMinor - spentMinor

    val fraction: Float
        get() = if (budget.amountMinor <= 0L) 0f else spentMinor.toFloat() / budget.amountMinor

    val overspent: Boolean get() = spentMinor > budget.amountMinor

    /**
     * How far through the period we are. Comparing this against [fraction] is
     * what turns "you've spent 60%" into "you're spending faster than planned".
     */
    fun elapsedFraction(today: LocalDate): Float {
        if (today.isBefore(period.start)) return 0f
        if (today.isAfter(period.endInclusive)) return 1f
        val elapsed = today.toEpochDay() - period.startEpochDay + 1
        return (elapsed.toFloat() / period.dayCount).coerceIn(0f, 1f)
    }

    /** Spend per remaining day that would still land on budget. */
    fun safeDailySpend(today: LocalDate): Long? {
        if (today.isAfter(period.endInclusive)) return null
        val daysLeft = (period.endEpochDay - maxOf(today.toEpochDay(), period.startEpochDay) + 1)
            .coerceAtLeast(1)
        return (remainingMinor / daysLeft).coerceAtLeast(0)
    }

    /** Ahead of plan, on track, or over — drives the colour of the meter. */
    fun status(today: LocalDate): Status = when {
        overspent -> Status.OVER
        fraction > elapsedFraction(today) + 0.1f -> Status.AT_RISK
        else -> Status.ON_TRACK
    }

    enum class Status { ON_TRACK, AT_RISK, OVER }
}

fun BudgetPeriod.toPeriodType(): PeriodType = when (this) {
    BudgetPeriod.WEEKLY -> PeriodType.WEEK
    BudgetPeriod.MONTHLY -> PeriodType.MONTH
    BudgetPeriod.QUARTERLY -> PeriodType.QUARTER
    BudgetPeriod.YEARLY -> PeriodType.YEAR
}

/** The occurrence of this budget's cycle that contains [anchor]. */
fun Budget.periodContaining(anchor: LocalDate, config: PeriodConfig): Period =
    Period.of(period.toPeriodType(), anchor, config)
