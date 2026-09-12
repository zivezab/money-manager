package com.zive.moneymanager.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Brand — a deep teal, reading as "ledger" rather than "banking app blue".
val Teal10 = Color(0xFF00201C)
val Teal20 = Color(0xFF003731)
val Teal40 = Color(0xFF006A5F)
val Teal80 = Color(0xFF54DBC8)
val Teal90 = Color(0xFF74F8E4)

val Sand10 = Color(0xFF231A00)
val Sand20 = Color(0xFF3B2F00)
val Sand40 = Color(0xFF6B5B00)
val Sand80 = Color(0xFFDFC64E)
val Sand90 = Color(0xFFFDE28C)

val Plum10 = Color(0xFF2B1200)
val Plum40 = Color(0xFF8B5000)
val Plum80 = Color(0xFFFFB876)
val Plum90 = Color(0xFFFFDCC2)

val NeutralLightBg = Color(0xFFFAFAF9)
val NeutralLightSurface = Color(0xFFFFFFFF)
val NeutralDarkBg = Color(0xFF0F1211)
val NeutralDarkSurface = Color(0xFF171B1A)

/**
 * Income / expense colouring lives outside the Material scheme: these keep their
 * meaning even when dynamic colour repaints everything else.
 */
@Immutable
data class MoneyColors(
    val income: Color,
    val incomeContainer: Color,
    val expense: Color,
    val expenseContainer: Color,
    val transfer: Color,
    val transferContainer: Color,
    val positive: Color,
    val warning: Color,
    val danger: Color,
    /** Palette used for chart series that have no colour of their own. */
    val chart: List<Color>,
)

val LightMoneyColors = MoneyColors(
    income = Color(0xFF0F7B54),
    incomeContainer = Color(0xFFD7F2E4),
    expense = Color(0xFFC0362C),
    expenseContainer = Color(0xFFFBE0DD),
    transfer = Color(0xFF4F5B92),
    transferContainer = Color(0xFFE2E5F6),
    positive = Color(0xFF0F7B54),
    warning = Color(0xFFB4690E),
    danger = Color(0xFFC0362C),
    chart = listOf(
        Color(0xFF0EA5E9), Color(0xFFF97316), Color(0xFF8B5CF6), Color(0xFF10B981),
        Color(0xFFF43F5E), Color(0xFFEAB308), Color(0xFF6366F1), Color(0xFF14B8A6),
        Color(0xFFEC4899), Color(0xFF84CC16), Color(0xFF06B6D4), Color(0xFFA855F7),
    ),
)

val DarkMoneyColors = MoneyColors(
    income = Color(0xFF5FD9A4),
    incomeContainer = Color(0xFF10352A),
    expense = Color(0xFFFF8A7D),
    expenseContainer = Color(0xFF41211E),
    transfer = Color(0xFFB6C2FF),
    transferContainer = Color(0xFF232840),
    positive = Color(0xFF5FD9A4),
    warning = Color(0xFFF5C168),
    danger = Color(0xFFFF8A7D),
    chart = listOf(
        Color(0xFF38BDF8), Color(0xFFFB923C), Color(0xFFA78BFA), Color(0xFF34D399),
        Color(0xFFFB7185), Color(0xFFFACC15), Color(0xFF818CF8), Color(0xFF2DD4BF),
        Color(0xFFF472B6), Color(0xFFA3E635), Color(0xFF22D3EE), Color(0xFFC084FC),
    ),
)

val LocalMoneyColors = staticCompositionLocalOf { LightMoneyColors }

/** Parses a "#RRGGBB" seed stored on a category or account. */
fun colorFromHex(hex: String?, fallback: Color): Color {
    if (hex.isNullOrBlank()) return fallback
    return runCatching {
        val clean = hex.removePrefix("#")
        val value = clean.toLong(16)
        when (clean.length) {
            6 -> Color(0xFF000000L or value)
            8 -> Color(value)
            else -> fallback
        }
    }.getOrDefault(fallback)
}
