package com.zive.moneymanager.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val defaults = Typography()

val AppTypography = Typography(
    displaySmall = defaults.displaySmall.copy(fontWeight = FontWeight.SemiBold),
    headlineMedium = defaults.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
    headlineSmall = defaults.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
    titleLarge = defaults.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = defaults.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = defaults.labelLarge.copy(fontWeight = FontWeight.Medium),
)

/**
 * Tabular figures keep amounts in a list from jittering as digits change.
 * FontFamily.Monospace is guaranteed present on every device.
 */
val AmountStyle = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.SemiBold,
    fontSize = 16.sp,
)

val AmountLargeStyle = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.Bold,
    fontSize = 28.sp,
)
