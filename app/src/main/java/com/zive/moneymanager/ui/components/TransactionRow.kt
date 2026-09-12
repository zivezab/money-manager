package com.zive.moneymanager.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zive.moneymanager.data.db.entity.TxType
import com.zive.moneymanager.data.db.model.TransactionDetail

/** A single ledger line as it appears in every list in the app. */
@Composable
fun TransactionRow(
    detail: TransactionDetail,
    symbol: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = when (detail.type) {
        TxType.TRANSFER -> "${detail.accountName} → ${detail.toAccountName ?: "?"}"
        else -> detail.categoryName ?: "Uncategorised"
    }
    val subtitleParts = buildList {
        if (detail.counterparty.isNotBlank()) add(detail.counterparty)
        if (detail.note.isNotBlank()) add(detail.note)
        if (detail.type != TxType.TRANSFER) add(detail.accountName)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (detail.type == TxType.TRANSFER) {
            IconBadge(
                iconKey = "transfer",
                colorHex = null,
                fallbackIcon = Icons.Filled.SwapHoriz,
            )
        } else {
            IconBadge(iconKey = detail.categoryIcon, colorHex = detail.categoryColor)
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (detail.recurringRuleId != null) {
                    Icon(
                        imageVector = Icons.Filled.Autorenew,
                        contentDescription = "Scheduled",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(start = 6.dp)
                            .width(14.dp),
                    )
                }
            }
            if (subtitleParts.isNotEmpty()) {
                Text(
                    text = subtitleParts.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        AmountText(
            amountMinor = detail.amountMinor,
            type = detail.type,
            symbol = symbol,
        )
    }
}
