package com.zive.moneymanager.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import com.zive.moneymanager.data.prefs.ThemeMode

private val LightScheme = lightColorScheme(
    primary = Teal40,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = Teal90,
    onPrimaryContainer = Teal10,
    secondary = Sand40,
    secondaryContainer = Sand90,
    onSecondaryContainer = Sand10,
    tertiary = Plum40,
    tertiaryContainer = Plum90,
    onTertiaryContainer = Plum10,
    background = NeutralLightBg,
    surface = NeutralLightSurface,
)

private val DarkScheme = darkColorScheme(
    primary = Teal80,
    onPrimary = Teal20,
    primaryContainer = Teal40,
    onPrimaryContainer = Teal90,
    secondary = Sand80,
    secondaryContainer = Sand20,
    onSecondaryContainer = Sand90,
    tertiary = Plum80,
    tertiaryContainer = Plum40,
    background = NeutralDarkBg,
    surface = NeutralDarkSurface,
)

@Composable
fun MoneyManagerTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val supportsDynamic = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val context = LocalContext.current

    val scheme = when {
        dynamicColor && supportsDynamic && dark -> dynamicDarkColorScheme(context)
        dynamicColor && supportsDynamic -> dynamicLightColorScheme(context)
        dark -> DarkScheme
        else -> LightScheme
    }

    CompositionLocalProvider(
        LocalMoneyColors provides if (dark) DarkMoneyColors else LightMoneyColors,
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = AppTypography,
            content = content,
        )
    }
}

/** Shorthand for `LocalMoneyColors.current`, read on nearly every screen. */
val moneyColors: MoneyColors
    @Composable get() = LocalMoneyColors.current
