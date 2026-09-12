package com.zive.moneymanager.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zive.moneymanager.core.Period
import com.zive.moneymanager.core.PeriodConfig
import com.zive.moneymanager.core.PeriodController
import com.zive.moneymanager.core.PeriodType
import com.zive.moneymanager.data.db.entity.IncomeKind
import com.zive.moneymanager.data.db.entity.TxType
import com.zive.moneymanager.data.db.model.CategoryTotal
import com.zive.moneymanager.data.db.model.DayTotal
import com.zive.moneymanager.data.db.model.PeriodTotals
import com.zive.moneymanager.data.prefs.AppSettings
import com.zive.moneymanager.data.prefs.SettingsStore
import com.zive.moneymanager.data.repo.FinanceRepository
import com.zive.moneymanager.di.AppContainer
import com.zive.moneymanager.di.containerFactory
import com.zive.moneymanager.ui.overview.IncomeSplit
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate

/** A sub-range of the selected period with its own income and expense totals. */
data class TrendBucket(
    val label: String,
    val start: LocalDate,
    val incomeMinor: Long,
    val expenseMinor: Long,
)

data class StatsUiState(
    val period: Period = Period.of(PeriodType.MONTH, LocalDate.now(), PeriodConfig()),
    val settings: AppSettings = AppSettings(),
    /** Which side of the ledger the donut is showing. */
    val side: TxType = TxType.EXPENSE,
    val totals: PeriodTotals = PeriodTotals(),
    val categories: List<CategoryTotal> = emptyList(),
    val subcategories: List<CategoryTotal> = emptyList(),
    val drilldownId: Long? = null,
    val drilldownName: String? = null,
    val trend: List<TrendBucket> = emptyList(),
    val incomeSplit: IncomeSplit = IncomeSplit(),
    val loading: Boolean = true,
) {
    val sideTotalMinor: Long
        get() = if (side == TxType.INCOME) totals.incomeMinor else totals.expenseMinor

    /** Average per day across the period, the figure people use for pacing. */
    val dailyAverageMinor: Long
        get() = if (period.dayCount <= 0) 0 else sideTotalMinor / period.dayCount
}

@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModel(
    private val repo: FinanceRepository,
    settingsStore: SettingsStore,
    val periods: PeriodController,
) : ViewModel() {

    private val side = MutableStateFlow(TxType.EXPENSE)
    private val drilldown = MutableStateFlow<Long?>(null)

    private data class Context(
        val period: Period,
        val settings: AppSettings,
        val side: TxType,
        val drilldownId: Long?,
    )

    private val context = combine(
        settingsStore.settings,
        periods.selection,
        side,
        drilldown,
    ) { settings, selection, activeSide, drill ->
        Context(periods.periodFor(selection, settings.periodConfig), settings, activeSide, drill)
    }

    val state = context.flatMapLatest { ctx ->
        combine(
            repo.observeTotals(ctx.period),
            repo.observeCategoryTotals(ctx.side, ctx.period),
            repo.observeDayTotals(ctx.period),
            repo.observeIncomeKindTotals(ctx.period),
            ctx.drilldownId?.let { repo.observeSubcategoryTotals(ctx.side, it, ctx.period) }
                ?: kotlinx.coroutines.flow.flowOf(emptyList()),
        ) { totals, categories, dayTotals, kinds, subcategories ->
            StatsUiState(
                period = ctx.period,
                settings = ctx.settings,
                side = ctx.side,
                totals = totals,
                categories = categories,
                subcategories = subcategories,
                drilldownId = ctx.drilldownId,
                drilldownName = categories.firstOrNull { it.categoryId == ctx.drilldownId }?.categoryName,
                trend = buildTrend(ctx.period, ctx.settings.periodConfig, dayTotals),
                incomeSplit = IncomeSplit(
                    jobMinor = kinds.firstOrNull { it.incomeKind == IncomeKind.JOB }?.totalMinor ?: 0L,
                    nonJobMinor = kinds.firstOrNull { it.incomeKind == IncomeKind.NON_JOB }?.totalMinor ?: 0L,
                    unclassifiedMinor = kinds
                        .filter { it.incomeKind == null || it.incomeKind == IncomeKind.NONE }
                        .sumOf { it.totalMinor },
                ),
                loading = false,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsUiState())

    fun setSide(type: TxType) {
        side.value = type
        drilldown.value = null
    }

    fun drillInto(categoryId: Long?) = drilldown.update { current ->
        if (current == categoryId) null else categoryId
    }

    /**
     * Folds day totals into the period's own sub-ranges — days for a week,
     * weeks for a month, months for a quarter or year.
     */
    private fun buildTrend(
        period: Period,
        config: PeriodConfig,
        dayTotals: List<DayTotal>,
    ): List<TrendBucket> {
        if (period.type == PeriodType.ALL && dayTotals.isEmpty()) return emptyList()
        val byDay = dayTotals.associateBy { it.dateEpochDay }

        val effective = if (period.type == PeriodType.ALL) {
            val first = dayTotals.minOfOrNull { it.dateEpochDay } ?: return emptyList()
            val last = dayTotals.maxOfOrNull { it.dateEpochDay } ?: return emptyList()
            Period(PeriodType.ALL, LocalDate.ofEpochDay(first), LocalDate.ofEpochDay(last))
        } else {
            period
        }

        return effective.trendBuckets(config).map { bucket ->
            var income = 0L
            var expense = 0L
            var day = bucket.start.toEpochDay()
            val end = bucket.endInclusive.toEpochDay()
            while (day <= end) {
                byDay[day]?.let {
                    income += it.incomeMinor
                    expense += it.expenseMinor
                }
                day++
            }
            TrendBucket(bucket.label, bucket.start, income, expense)
        }
    }

    companion object {
        val Factory = containerFactory { c: AppContainer ->
            StatsViewModel(c.repository, c.settings, c.periodController)
        }
    }
}
