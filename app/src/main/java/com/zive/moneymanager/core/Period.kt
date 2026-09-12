package com.zive.moneymanager.core

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

enum class PeriodType(val label: String) {
    DAY("Daily"),
    WEEK("Weekly"),
    MONTH("Monthly"),
    QUARTER("Quarterly"),
    YEAR("Yearly"),
    ALL("All");

    /** Types the period selector offers, in order. */
    companion object {
        val selectable = listOf(DAY, WEEK, MONTH, QUARTER, YEAR, ALL)
    }
}

/**
 * Rules that shift period boundaries away from the plain calendar — a salary that
 * lands on the 25th makes "this month" run the 25th to the 24th.
 */
data class PeriodConfig(
    val weekStart: DayOfWeek = DayOfWeek.MONDAY,
    val monthStartDay: Int = 1,
) {
    /** Clamped so the 31st still works in February. */
    fun monthStartIn(year: Int, month: Int): LocalDate {
        val first = LocalDate.of(year, month, 1)
        return first.withDayOfMonth(minOf(monthStartDay, first.lengthOfMonth()))
    }
}

/** A closed date range [start, end] plus the knowledge of how it was derived. */
data class Period(
    val type: PeriodType,
    val start: LocalDate,
    val endInclusive: LocalDate,
) {
    val startEpochDay: Long get() = start.toEpochDay()
    val endEpochDay: Long get() = endInclusive.toEpochDay()

    fun contains(date: LocalDate): Boolean =
        !date.isBefore(start) && !date.isAfter(endInclusive)

    val dayCount: Int get() = (endEpochDay - startEpochDay + 1).toInt()

    fun previous(config: PeriodConfig): Period =
        if (type == PeriodType.ALL) this else of(type, start.minusDays(1), config)

    fun next(config: PeriodConfig): Period =
        if (type == PeriodType.ALL) this else of(type, endInclusive.plusDays(1), config)

    /** True when [today] falls inside this period, used to disable "next". */
    fun isCurrent(today: LocalDate): Boolean = type == PeriodType.ALL || contains(today)

    fun label(today: LocalDate = LocalDate.now()): String = when (type) {
        PeriodType.DAY -> when (start) {
            today -> "Today"
            today.minusDays(1) -> "Yesterday"
            else -> start.format(DAY_FMT)
        }
        PeriodType.WEEK -> {
            val sameMonth = start.month == endInclusive.month && start.year == endInclusive.year
            if (sameMonth) {
                "${start.dayOfMonth} – ${endInclusive.format(D_MON_YEAR)}"
            } else {
                "${start.format(D_MON)} – ${endInclusive.format(D_MON_YEAR)}"
            }
        }
        PeriodType.MONTH ->
            if (start.dayOfMonth == 1) start.format(MON_YEAR)
            else "${start.format(D_MON)} – ${endInclusive.format(D_MON_YEAR)}"
        PeriodType.QUARTER -> "Q${(start.monthValue - 1) / 3 + 1} ${start.year}"
        PeriodType.YEAR -> if (start.dayOfMonth == 1 && start.monthValue == 1) {
            start.year.toString()
        } else {
            "${start.format(D_MON_YEAR)} – ${endInclusive.format(D_MON_YEAR)}"
        }
        PeriodType.ALL -> "All time"
    }

    /** Sub-ranges used to draw the trend chart across this period. */
    fun trendBuckets(config: PeriodConfig): List<Bucket> = when (type) {
        PeriodType.DAY, PeriodType.WEEK ->
            (0 until dayCount).map { i ->
                val d = start.plusDays(i.toLong())
                Bucket(d, d, d.dayOfWeek.getDisplayName(java.time.format.TextStyle.NARROW, Locale.getDefault()))
            }
        PeriodType.MONTH -> {
            // Weekly buckets clipped to the month so labels stay readable.
            val out = mutableListOf<Bucket>()
            var cursor = start
            while (!cursor.isAfter(endInclusive)) {
                val weekEnd = cursor.with(TemporalAdjusters.nextOrSame(config.weekStart.minus(1)))
                val bEnd = minOf(weekEnd, endInclusive)
                out += Bucket(cursor, bEnd, cursor.dayOfMonth.toString())
                cursor = bEnd.plusDays(1)
            }
            out
        }
        PeriodType.QUARTER, PeriodType.YEAR -> {
            val out = mutableListOf<Bucket>()
            var cursor = start
            while (!cursor.isAfter(endInclusive)) {
                val next = cursor.plusMonths(1)
                val bEnd = minOf(next.minusDays(1), endInclusive)
                out += Bucket(cursor, bEnd, cursor.format(MON_SHORT))
                cursor = bEnd.plusDays(1)
            }
            out
        }
        PeriodType.ALL -> {
            val out = mutableListOf<Bucket>()
            var cursor = start.withDayOfMonth(1)
            while (!cursor.isAfter(endInclusive) && out.size < 36) {
                val bEnd = minOf(cursor.plusMonths(1).minusDays(1), endInclusive)
                out += Bucket(cursor, bEnd, cursor.format(MON_SHORT))
                cursor = bEnd.plusDays(1)
            }
            out
        }
    }

    data class Bucket(val start: LocalDate, val endInclusive: LocalDate, val label: String)

    companion object {
        private val DAY_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.getDefault())
        private val D_MON: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
        private val D_MON_YEAR: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())
        private val MON_YEAR: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
        private val MON_SHORT: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM", Locale.getDefault())

        /** The earliest date the app will look back to for "All time" queries. */
        val MIN_DATE: LocalDate = LocalDate.of(1970, 1, 1)
        val MAX_DATE: LocalDate = LocalDate.of(2999, 12, 31)

        /** Builds the period of [type] that contains [anchor]. */
        fun of(type: PeriodType, anchor: LocalDate, config: PeriodConfig): Period = when (type) {
            PeriodType.DAY -> Period(type, anchor, anchor)

            PeriodType.WEEK -> {
                val s = anchor.with(TemporalAdjusters.previousOrSame(config.weekStart))
                Period(type, s, s.plusDays(6))
            }

            PeriodType.MONTH -> {
                val s = monthStartFor(anchor, config)
                Period(type, s, s.plusMonths(1).minusDays(1))
            }

            PeriodType.QUARTER -> {
                val ms = monthStartFor(anchor, config)
                val quarterFirstMonth = ((ms.monthValue - 1) / 3) * 3 + 1
                val s = config.monthStartIn(ms.year, quarterFirstMonth)
                Period(type, s, s.plusMonths(3).minusDays(1))
            }

            PeriodType.YEAR -> {
                val ms = monthStartFor(anchor, config)
                val s = config.monthStartIn(ms.year, 1)
                Period(type, s, s.plusYears(1).minusDays(1))
            }

            PeriodType.ALL -> Period(type, MIN_DATE, MAX_DATE)
        }

        /** Start of the financial month containing [date]. */
        fun monthStartFor(date: LocalDate, config: PeriodConfig): LocalDate {
            val thisMonthStart = config.monthStartIn(date.year, date.monthValue)
            return if (!date.isBefore(thisMonthStart)) {
                thisMonthStart
            } else {
                val prev = date.minusMonths(1)
                config.monthStartIn(prev.year, prev.monthValue)
            }
        }
    }
}
