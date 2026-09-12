package com.zive.moneymanager.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.zive.moneymanager.core.CurrencyOption
import com.zive.moneymanager.core.PeriodConfig
import com.zive.moneymanager.core.PeriodType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.LocalDate

enum class ThemeMode(val label: String) { SYSTEM("Follow system"), LIGHT("Light"), DARK("Dark") }

data class AppSettings(
    val currency: CurrencyOption = CurrencyOption.SGD,
    val weekStart: DayOfWeek = DayOfWeek.MONDAY,
    /** Day of month a financial month begins — 25 for a salary that lands on the 25th. */
    val monthStartDay: Int = 1,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val defaultPeriod: PeriodType = PeriodType.MONTH,
    val carryOverBalance: Boolean = false,
    val seeded: Boolean = false,
    val lastRecurringRunDay: Long = 0,
    /** How many days ahead counts as "expiring soon". */
    val expiryHorizonDays: Int = 7,
    val expiryReminderEnabled: Boolean = true,
    /** Hour of day, 0-23, the daily expiry check posts its notification. */
    val expiryReminderHour: Int = 9,
    /** Held items untouched for this many months are flagged as clutter. */
    val declutterIdleMonths: Int = 6,
) {
    val periodConfig: PeriodConfig get() = PeriodConfig(weekStart, monthStartDay)
    val symbol: String get() = currency.symbol

    /** Last date still counted as "expiring soon", inclusive. */
    fun expiryHorizon(today: LocalDate): LocalDate = today.plusDays(expiryHorizonDays.toLong())

    /** Anything last touched on or before this is considered idle. */
    fun declutterCutoff(today: LocalDate): LocalDate = today.minusMonths(declutterIdleMonths.toLong())
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsStore(private val context: Context) {

    private object Keys {
        val CURRENCY = stringPreferencesKey("currency")
        val WEEK_START = intPreferencesKey("week_start")
        val MONTH_START_DAY = intPreferencesKey("month_start_day")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val DEFAULT_PERIOD = stringPreferencesKey("default_period")
        val CARRY_OVER = booleanPreferencesKey("carry_over")
        val SEEDED = booleanPreferencesKey("seeded")
        val LAST_RECURRING_RUN = longPreferencesKey("last_recurring_run")
        val EXPIRY_HORIZON_DAYS = intPreferencesKey("expiry_horizon_days")
        val EXPIRY_REMINDER_ENABLED = booleanPreferencesKey("expiry_reminder_enabled")
        val EXPIRY_REMINDER_HOUR = intPreferencesKey("expiry_reminder_hour")
        val DECLUTTER_IDLE_MONTHS = intPreferencesKey("declutter_idle_months")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            currency = CurrencyOption.fromCode(p[Keys.CURRENCY] ?: CurrencyOption.SGD.code),
            weekStart = DayOfWeek.of((p[Keys.WEEK_START] ?: DayOfWeek.MONDAY.value).coerceIn(1, 7)),
            monthStartDay = (p[Keys.MONTH_START_DAY] ?: 1).coerceIn(1, 28),
            themeMode = runCatching { ThemeMode.valueOf(p[Keys.THEME_MODE] ?: "SYSTEM") }.getOrDefault(ThemeMode.SYSTEM),
            dynamicColor = p[Keys.DYNAMIC_COLOR] ?: true,
            defaultPeriod = runCatching { PeriodType.valueOf(p[Keys.DEFAULT_PERIOD] ?: "MONTH") }.getOrDefault(PeriodType.MONTH),
            carryOverBalance = p[Keys.CARRY_OVER] ?: false,
            seeded = p[Keys.SEEDED] ?: false,
            lastRecurringRunDay = p[Keys.LAST_RECURRING_RUN] ?: 0L,
            expiryHorizonDays = (p[Keys.EXPIRY_HORIZON_DAYS] ?: 7).coerceIn(1, 90),
            expiryReminderEnabled = p[Keys.EXPIRY_REMINDER_ENABLED] ?: true,
            expiryReminderHour = (p[Keys.EXPIRY_REMINDER_HOUR] ?: 9).coerceIn(0, 23),
            declutterIdleMonths = (p[Keys.DECLUTTER_IDLE_MONTHS] ?: 6).coerceIn(1, 60),
        )
    }

    suspend fun setCurrency(option: CurrencyOption) = edit { it[Keys.CURRENCY] = option.code }
    suspend fun setWeekStart(day: DayOfWeek) = edit { it[Keys.WEEK_START] = day.value }
    suspend fun setMonthStartDay(day: Int) = edit { it[Keys.MONTH_START_DAY] = day.coerceIn(1, 28) }
    suspend fun setThemeMode(mode: ThemeMode) = edit { it[Keys.THEME_MODE] = mode.name }
    suspend fun setDynamicColor(enabled: Boolean) = edit { it[Keys.DYNAMIC_COLOR] = enabled }
    suspend fun setDefaultPeriod(type: PeriodType) = edit { it[Keys.DEFAULT_PERIOD] = type.name }
    suspend fun setCarryOver(enabled: Boolean) = edit { it[Keys.CARRY_OVER] = enabled }
    suspend fun setSeeded(seeded: Boolean) = edit { it[Keys.SEEDED] = seeded }
    suspend fun setLastRecurringRun(epochDay: Long) = edit { it[Keys.LAST_RECURRING_RUN] = epochDay }
    suspend fun setExpiryHorizonDays(days: Int) = edit { it[Keys.EXPIRY_HORIZON_DAYS] = days.coerceIn(1, 90) }
    suspend fun setExpiryReminderEnabled(enabled: Boolean) = edit { it[Keys.EXPIRY_REMINDER_ENABLED] = enabled }
    suspend fun setExpiryReminderHour(hour: Int) = edit { it[Keys.EXPIRY_REMINDER_HOUR] = hour.coerceIn(0, 23) }
    suspend fun setDeclutterIdleMonths(months: Int) = edit { it[Keys.DECLUTTER_IDLE_MONTHS] = months.coerceIn(1, 60) }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit(block)
    }
}
