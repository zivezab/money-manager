package com.zive.moneymanager.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zive.moneymanager.core.Period
import com.zive.moneymanager.core.PeriodType
import java.time.LocalDate

/**
 * The control every reporting screen shares: pick a granularity, then step
 * backwards and forwards through periods of that granularity.
 */
@Composable
fun PeriodBar(
    period: Period,
    today: LocalDate,
    onTypeChange: (PeriodType) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onJumpToToday: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PeriodType.selectable.forEach { type ->
                    FilterChip(
                        selected = type == period.type,
                        onClick = { onTypeChange(type) },
                        label = { Text(type.label) },
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                val navigable = period.type != PeriodType.ALL

                IconButton(onClick = onPrevious, enabled = navigable) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Previous period",
                        modifier = Modifier.size(28.dp),
                    )
                }

                TextButton(onClick = onJumpToToday) {
                    Text(
                        text = period.label(today),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }

                // Stepping past the current period would only ever show zeroes.
                IconButton(onClick = onNext, enabled = navigable && !period.isCurrent(today)) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Next period",
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
        }
    }
}
