package com.zive.moneymanager.ui.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zive.moneymanager.core.ExpiryBucket
import com.zive.moneymanager.ui.components.EmptyState
import com.zive.moneymanager.ui.theme.moneyColors
import java.time.LocalDate

/**
 * The expiration checklist: what is going off, grouped by urgency, with the three
 * decisions you actually make — I used it, I threw it out, or it is still fine.
 */
@Composable
fun ExpiryChecklist(
    state: InventoryUiState,
    today: LocalDate,
    symbol: String,
    actions: InventoryActions,
    onOpenItem: (Long) -> Unit,
) {
    if (state.expirySections.isEmpty() && !state.loading) {
        EmptyState(
            icon = Icons.Filled.EventAvailable,
            title = "Nothing expiring",
            subtitle = "Nothing you are tracking goes off within the next month. Add expiry dates to groceries and medicine to see them here.",
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = InventoryListPadding,
    ) {
        state.expirySections.forEach { section ->
            item(key = "header-${section.bucket}") {
                BucketHeader(bucket = section.bucket, count = section.items.size)
            }
            items(section.items, key = { it.item.id }) { detail ->
                InventoryRow(
                    detail = detail,
                    symbol = symbol,
                    today = today,
                    onClick = { onOpenItem(detail.item.id) },
                    trailing = {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            RowAction(
                                label = "Used",
                                icon = Icons.Filled.CheckCircle,
                                color = moneyColors.positive,
                                onClick = { actions.markUsedUp(detail.item.id) },
                            )
                            RowAction(
                                label = "Binned",
                                icon = Icons.Filled.DeleteOutline,
                                color = moneyColors.danger,
                                onClick = { actions.completeDiscard(detail.item.id) },
                            )
                            RowAction(
                                label = "+7d",
                                icon = Icons.Filled.Schedule,
                                onClick = { actions.extendExpiry(detail.item.id, 7) },
                            )
                        }
                    },
                )
            }
        }

        item {
            Text(
                text = "\"Used\" and \"Binned\" take the item out of stock. \"+7d\" pushes the date back a week when something is still good.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@Composable
private fun BucketHeader(bucket: ExpiryBucket, count: Int) {
    val color = bucket.color()
    Row(
        Modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.10f))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = bucket.label,
            style = MaterialTheme.typography.labelLarge,
            color = color,
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelLarge,
            color = color,
        )
    }
}
