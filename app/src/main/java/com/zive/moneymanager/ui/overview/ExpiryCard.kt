package com.zive.moneymanager.ui.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zive.moneymanager.core.ExpiryBucket
import com.zive.moneymanager.core.ExpiryText
import com.zive.moneymanager.data.db.model.InventoryItemDetail
import com.zive.moneymanager.data.db.model.InventorySummary
import com.zive.moneymanager.ui.inventory.color
import com.zive.moneymanager.ui.inventory.formatQuantity
import com.zive.moneymanager.ui.theme.moneyColors
import java.time.LocalDate

/**
 * The overview's expiration checklist: what is going off, with one tap to say it
 * was used. Only rendered when there is actually something to act on.
 */
@Composable
fun ExpiryChecklistCard(
    summary: InventorySummary,
    items: List<InventoryItemDetail>,
    today: LocalDate,
    onMarkUsed: (Long) -> Unit,
    onOpenInventory: () -> Unit,
    onOpenItem: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = moneyColors
    val headlineColor = if (summary.expiredCount > 0) colors.danger else colors.warning

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.EventBusy,
                    contentDescription = null,
                    tint = headlineColor,
                    modifier = Modifier.size(20.dp),
                )
                Column(
                    Modifier
                        .weight(1f)
                        .padding(start = 10.dp)
                ) {
                    Text(
                        text = headline(summary),
                        style = MaterialTheme.typography.titleSmall,
                        color = headlineColor,
                    )
                    if (summary.pendingDisposalCount > 0) {
                        Text(
                            text = "${summary.pendingDisposalCount} waiting to be donated or thrown away",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                TextButton(onClick = onOpenInventory) { Text("Open") }
            }

            if (items.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                items.forEach { detail ->
                    ChecklistLine(
                        detail = detail,
                        today = today,
                        onMarkUsed = { onMarkUsed(detail.item.id) },
                        onClick = { onOpenItem(detail.item.id) },
                    )
                }

                val remaining = summary.needsAttentionCount - items.size
                if (remaining > 0) {
                    TextButton(onClick = onOpenInventory) {
                        Text("and $remaining more")
                    }
                }
            }
        }
    }
}

private fun headline(summary: InventorySummary): String = when {
    summary.expiredCount > 0 && summary.expiringSoonCount > 0 ->
        "${summary.expiredCount} expired, ${summary.expiringSoonCount} expiring soon"
    summary.expiredCount > 0 ->
        if (summary.expiredCount == 1) "1 item has expired" else "${summary.expiredCount} items have expired"
    summary.expiringSoonCount > 0 ->
        if (summary.expiringSoonCount == 1) "1 item expiring soon" else "${summary.expiringSoonCount} items expiring soon"
    else -> "Inventory needs a tidy"
}

@Composable
private fun ChecklistLine(
    detail: InventoryItemDetail,
    today: LocalDate,
    onMarkUsed: () -> Unit,
    onClick: () -> Unit,
) {
    val bucket = ExpiryBucket.of(detail.item.expiryEpochDay, today.toEpochDay())
    val color = bucket.color()

    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Column(Modifier.weight(1f)) {
            Text(
                text = detail.item.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = buildList {
                    ExpiryText.describe(detail.item.expiryEpochDay, today)?.let(::add)
                    add("${formatQuantity(detail.item.quantity)} ${detail.item.unit.label}")
                    if (detail.item.location.isNotBlank()) add(detail.item.location)
                }.joinToString(" · "),
                style = MaterialTheme.typography.labelSmall,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onMarkUsed) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = "Mark ${detail.item.name} as used",
                tint = moneyColors.positive,
            )
        }
    }
}
