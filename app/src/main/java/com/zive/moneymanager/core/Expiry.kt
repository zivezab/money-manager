package com.zive.moneymanager.core

import java.time.LocalDate

/**
 * How urgent an expiry date is. The checklist groups by these, so the ordering of
 * the constants is the ordering on screen.
 */
enum class ExpiryBucket(val label: String, val urgent: Boolean) {
    EXPIRED("Already expired", true),
    TODAY("Expires today", true),
    NEXT_THREE_DAYS("Next 3 days", true),
    THIS_WEEK("Within a week", false),
    THIS_MONTH("Within a month", false),
    LATER("Later", false),
    NONE("No expiry date", false);

    companion object {

        /** Buckets that mean "deal with this now", in display order. */
        val urgentBuckets: List<ExpiryBucket> = entries.filter { it.urgent }

        fun of(expiryEpochDay: Long?, todayEpochDay: Long): ExpiryBucket {
            if (expiryEpochDay == null) return NONE
            return when (val days = expiryEpochDay - todayEpochDay) {
                in Long.MIN_VALUE..-1L -> EXPIRED
                0L -> TODAY
                in 1L..3L -> NEXT_THREE_DAYS
                in 4L..7L -> THIS_WEEK
                in 8L..31L -> THIS_MONTH
                else -> {
                    check(days > 31L)
                    LATER
                }
            }
        }
    }
}

/** Human phrasing for a countdown, e.g. "3 days left", "expired 2 days ago". */
object ExpiryText {

    fun describe(expiryEpochDay: Long?, today: LocalDate): String? {
        if (expiryEpochDay == null) return null
        return when (val days = expiryEpochDay - today.toEpochDay()) {
            0L -> "Expires today"
            1L -> "1 day left"
            -1L -> "Expired yesterday"
            in 2L..Long.MAX_VALUE -> "$days days left"
            else -> "Expired ${-days} days ago"
        }
    }

    /** Shortened for chips on a dense list row. */
    fun chip(expiryEpochDay: Long?, today: LocalDate): String? {
        if (expiryEpochDay == null) return null
        return when (val days = expiryEpochDay - today.toEpochDay()) {
            0L -> "Today"
            in 1L..Long.MAX_VALUE -> "${days}d"
            else -> "${-days}d ago"
        }
    }
}
