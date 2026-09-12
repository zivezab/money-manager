package com.zive.moneymanager.core

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * All monetary values are stored as [Long] minor units (cents) to avoid binary
 * floating point drift. Formatting is centralised here so every screen renders
 * amounts identically.
 */
object Money {

    const val MINOR_PER_MAJOR = 100L

    /** Parses free-form user input ("1,234.5", "12", "-3.99") into minor units. */
    fun parse(input: String): Long? {
        val cleaned = input.trim().replace(",", "").replace(" ", "")
        if (cleaned.isEmpty() || cleaned == "-" || cleaned == ".") return null
        return try {
            BigDecimal(cleaned)
                .setScale(2, RoundingMode.HALF_UP)
                .movePointRight(2)
                .toLong()
        } catch (_: NumberFormatException) {
            null
        }
    }

    /** Minor units to a plain editable string, e.g. 123456 -> "1234.56". */
    fun toEditable(minor: Long): String =
        BigDecimal.valueOf(minor, 2).toPlainString()

    /** "1,234.56" — grouped, no currency symbol. */
    fun formatPlain(minor: Long): String {
        val df = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))
        return df.format(BigDecimal.valueOf(minor, 2))
    }

    /** "S$1,234.56" or "-S$1,234.56". */
    fun format(minor: Long, symbol: String): String {
        val sign = if (minor < 0) "-" else ""
        return "$sign$symbol${formatPlain(kotlin.math.abs(minor))}"
    }

    /** Signed with an explicit leading + for positives, used for net totals. */
    fun formatSigned(minor: Long, symbol: String): String = when {
        minor > 0 -> "+$symbol${formatPlain(minor)}"
        minor < 0 -> "-$symbol${formatPlain(-minor)}"
        else -> "$symbol${formatPlain(0)}"
    }

    /** Compact form for chart labels: 1.2k, 34.5k, 1.2M. */
    fun formatCompact(minor: Long, symbol: String): String {
        val major = minor / MINOR_PER_MAJOR.toDouble()
        val abs = kotlin.math.abs(major)
        val sign = if (major < 0) "-" else ""
        val body = when {
            abs >= 1_000_000 -> String.format(Locale.US, "%.1fM", abs / 1_000_000)
            abs >= 1_000 -> String.format(Locale.US, "%.1fk", abs / 1_000)
            else -> String.format(Locale.US, "%.0f", abs)
        }
        return "$sign$symbol$body"
    }
}

/** Currencies offered in settings. Symbol is what gets rendered next to amounts. */
enum class CurrencyOption(val code: String, val symbol: String, val label: String) {
    SGD("SGD", "S$", "Singapore Dollar"),
    USD("USD", "$", "US Dollar"),
    EUR("EUR", "€", "Euro"),
    GBP("GBP", "£", "British Pound"),
    MYR("MYR", "RM", "Malaysian Ringgit"),
    INR("INR", "₹", "Indian Rupee"),
    JPY("JPY", "¥", "Japanese Yen"),
    CNY("CNY", "CN¥", "Chinese Yuan"),
    AUD("AUD", "A$", "Australian Dollar"),
    HKD("HKD", "HK$", "Hong Kong Dollar"),
    IDR("IDR", "Rp", "Indonesian Rupiah"),
    THB("THB", "฿", "Thai Baht");

    companion object {
        fun fromCode(code: String): CurrencyOption =
            entries.firstOrNull { it.code == code } ?: SGD
    }
}
