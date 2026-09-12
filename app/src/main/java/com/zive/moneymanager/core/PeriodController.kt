package com.zive.moneymanager.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate

/**
 * The selected reporting window, shared by every tab. Switching from Overview to
 * Stats keeps you in the same month rather than resetting to today, which is what
 * you want when you are investigating a particular period.
 */
class PeriodController {

    data class Selection(
        val type: PeriodType = PeriodType.MONTH,
        val anchor: LocalDate = LocalDate.now(),
    )

    private val _selection = MutableStateFlow(Selection())
    val selection: StateFlow<Selection> = _selection.asStateFlow()

    /** Applies the user's saved default the first time settings load. */
    fun applyDefaultOnce(type: PeriodType) {
        if (defaultApplied) return
        defaultApplied = true
        _selection.update { it.copy(type = type) }
    }

    private var defaultApplied = false

    fun setType(type: PeriodType) = _selection.update { it.copy(type = type) }

    fun jumpToToday() = _selection.update { it.copy(anchor = LocalDate.now()) }

    fun jumpTo(date: LocalDate) = _selection.update { it.copy(anchor = date) }

    fun previous(config: PeriodConfig) = _selection.update {
        it.copy(anchor = Period.of(it.type, it.anchor, config).previous(config).start)
    }

    fun next(config: PeriodConfig) = _selection.update {
        it.copy(anchor = Period.of(it.type, it.anchor, config).next(config).start)
    }

    fun periodFor(selection: Selection, config: PeriodConfig): Period =
        Period.of(selection.type, selection.anchor, config)
}
