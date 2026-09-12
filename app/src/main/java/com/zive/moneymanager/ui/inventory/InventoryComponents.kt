package com.zive.moneymanager.ui.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zive.moneymanager.core.ExpiryBucket
import com.zive.moneymanager.core.ExpiryText
import com.zive.moneymanager.core.Money
import com.zive.moneymanager.data.db.entity.ItemKind
import com.zive.moneymanager.data.db.entity.ItemStatus
import com.zive.moneymanager.data.db.model.InventoryItemDetail
import com.zive.moneymanager.ui.components.IconBadge
import com.zive.moneymanager.ui.theme.MoneyColors
import com.zive.moneymanager.ui.theme.moneyColors
import java.time.LocalDate

/** Colour an expiry bucket should carry wherever it appears. */
@Composable
fun ExpiryBucket.color(colors: MoneyColors = moneyColors): Color = when (this) {
    ExpiryBucket.EXPIRED -> colors.danger
    ExpiryBucket.TODAY -> colors.danger
    ExpiryBucket.NEXT_THREE_DAYS -> colors.warning
    ExpiryBucket.THIS_WEEK -> colors.warning
    ExpiryBucket.THIS_MONTH -> MaterialTheme.colorScheme.onSurfaceVariant
    ExpiryBucket.LATER -> MaterialTheme.colorScheme.onSurfaceVariant
    ExpiryBucket.NONE -> MaterialTheme.colorScheme.onSurfaceVariant
}

/** Quantity rendered without a trailing ".0" on whole numbers. */
fun formatQuantity(value: Double): String =
    if (value == Math.floor(value) && !value.isInfinite()) value.toLong().toString()
    else String.format(java.util.Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')

/**
 * One inventory row. [trailing] carries whatever actions the calling screen
 * offers — check-off buttons on the checklists, nothing on the plain stock list.
 */
@Composable
fun InventoryRow(
    detail: InventoryItemDetail,
    symbol: String,
    today: LocalDate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
) {
    val item = detail.item
    val bucket = ExpiryBucket.of(item.expiryEpochDay, today.toEpochDay())
    val bucketColor = bucket.color()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(
                iconKey = detail.categoryIcon,
                colorHex = detail.categoryColor,
                fallbackIcon = if (detail.categoryIcon == null) Icons.Filled.Inventory2 else null,
            )

            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val subtitle = buildList {
                    add("${formatQuantity(item.quantity)} ${item.unit.label}")
                    if (item.location.isNotBlank()) add(item.location)
                    detail.categoryName?.let(::add)
                }.joinToString(" · ")
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                if (item.totalValueMinor > 0) {
                    Text(
                        text = Money.format(item.totalValueMinor, symbol),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                ExpiryText.chip(item.expiryEpochDay, today)?.let { chip ->
                    Text(
                        text = chip,
                        style = MaterialTheme.typography.labelMedium,
                        color = bucketColor,
                    )
                }
            }
        }

        val badges = buildList {
            if (item.kind == ItemKind.ASSET) add("Asset" to MaterialTheme.colorScheme.onSurfaceVariant)
            when (item.status) {
                ItemStatus.TO_DONATE -> add("To donate" to moneyColors.positive)
                ItemStatus.TO_DISCARD -> add("To throw away" to moneyColors.warning)
                ItemStatus.USED_UP -> add("Used up" to MaterialTheme.colorScheme.onSurfaceVariant)
                ItemStatus.DONATED -> add("Donated" to MaterialTheme.colorScheme.onSurfaceVariant)
                ItemStatus.DISCARDED -> add("Thrown away" to MaterialTheme.colorScheme.onSurfaceVariant)
                ItemStatus.SOLD -> add("Sold" to MaterialTheme.colorScheme.onSurfaceVariant)
                ItemStatus.IN_STOCK -> Unit
            }
        }

        if (badges.isNotEmpty() || trailing != null) {
            Spacer(Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                badges.forEach { (label, color) -> StatusBadge(label, color) }
                Spacer(Modifier.weight(1f))
                trailing?.invoke()
            }
        }
    }
}

@Composable
fun StatusBadge(text: String, color: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

/** Compact action button used on the checklists. */
@Composable
fun RowAction(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    AssistChip(
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
        leadingIcon = {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
        },
        colors = AssistChipDefaults.assistChipColors(labelColor = color, leadingIconContentColor = color),
    )
}
