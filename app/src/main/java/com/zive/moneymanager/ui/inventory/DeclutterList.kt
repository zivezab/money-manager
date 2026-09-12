package com.zive.moneymanager.ui.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zive.moneymanager.core.Money
import com.zive.moneymanager.data.db.model.InventoryItemDetail
import com.zive.moneymanager.ui.components.EmptyState
import com.zive.moneymanager.ui.components.SectionHeader
import com.zive.moneymanager.ui.theme.moneyColors
import java.time.LocalDate

/**
 * The minimalist pass. Three things the data can tell you without judgement —
 * you own several of these, you have not touched this in months, this went off
 * and is still in the cupboard — plus the donate / throw-away checklist those
 * decisions feed into.
 */
@Composable
fun DeclutterList(
    state: InventoryUiState,
    today: LocalDate,
    symbol: String,
    actions: InventoryActions,
    onOpenItem: (Long) -> Unit,
) {
    val nothingToReview = state.declutterCount == 0 && state.disposalCount == 0
    if (nothingToReview && !state.loading) {
        EmptyState(
            icon = Icons.Filled.Spa,
            title = "Nothing to review",
            subtitle = "No duplicates, nothing sitting unused, and nothing expired still on the shelf.",
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = InventoryListPadding,
    ) {
        // ---- The checklist you act on ----
        if (state.disposalCount > 0) {
            item {
                SectionHeader("Ready to go · ${state.disposalCount}")
            }
            if (state.toDonate.isNotEmpty()) {
                item { SubHeader("Donate", moneyColors.positive) }
                items(state.toDonate, key = { "donate-${it.item.id}" }) { detail ->
                    DisposalRow(
                        detail = detail,
                        symbol = symbol,
                        onComplete = { actions.completeDonation(detail.item.id) },
                        onUndo = { actions.returnToStock(detail.item.id) },
                        onClick = { onOpenItem(detail.item.id) },
                    )
                }
            }
            if (state.toDiscard.isNotEmpty()) {
                item { SubHeader("Throw away", moneyColors.warning) }
                items(state.toDiscard, key = { "discard-${it.item.id}" }) { detail ->
                    DisposalRow(
                        detail = detail,
                        symbol = symbol,
                        onComplete = { actions.completeDiscard(detail.item.id) },
                        onUndo = { actions.returnToStock(detail.item.id) },
                        onClick = { onOpenItem(detail.item.id) },
                    )
                }
            }
            item {
                Text(
                    text = "Tick an item once it has actually left the house.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
            }
        }

        // ---- Things worth a second look ----
        if (state.duplicates.isNotEmpty()) {
            item {
                SectionHeader("You own more than one · ${state.duplicates.size}")
            }
            items(state.duplicates, key = { "dup-${it.nameKey}" }) { group ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = group.displayName,
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "${group.itemCount} entries",
                                style = MaterialTheme.typography.labelMedium,
                                color = moneyColors.warning,
                            )
                        }
                        Text(
                            text = buildList {
                                add("${formatQuantity(group.totalQuantity)} in total")
                                group.locations
                                    ?.takeIf { it.isNotBlank() }
                                    ?.let { add(it.replace(",", ", ")) }
                                if (group.totalValueMinor > 0) {
                                    add(Money.format(group.totalValueMinor, symbol))
                                }
                            }.joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        if (state.untouched.isNotEmpty()) {
            item {
                SectionHeader(
                    "Untouched for ${state.settings.declutterIdleMonths}+ months · ${state.untouched.size}"
                )
            }
            items(state.untouched, key = { "idle-${it.item.id}" }) { detail ->
                SuggestionRow(
                    detail = detail,
                    symbol = symbol,
                    today = today,
                    detailLine = idleDescription(detail, today),
                    actions = actions,
                    onClick = { onOpenItem(detail.item.id) },
                    showKeepAction = true,
                )
            }
        }

        if (state.expiredHeld.isNotEmpty()) {
            item {
                SectionHeader("Expired but still here · ${state.expiredHeld.size}")
            }
            items(state.expiredHeld, key = { "expired-${it.item.id}" }) { detail ->
                SuggestionRow(
                    detail = detail,
                    symbol = symbol,
                    today = today,
                    detailLine = null,
                    actions = actions,
                    onClick = { onOpenItem(detail.item.id) },
                    showKeepAction = false,
                )
            }
        }
    }
}

@Composable
private fun SubHeader(text: String, color: androidx.compose.ui.graphics.Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = color,
        modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 2.dp),
    )
}

/** A checklist line: tick it when the thing has actually gone. */
@Composable
private fun DisposalRow(
    detail: InventoryItemDetail,
    symbol: String,
    onComplete: () -> Unit,
    onUndo: () -> Unit,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 16.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = false, onCheckedChange = { onComplete() })
        Column(
            Modifier
                .weight(1f)
                .padding(end = 8.dp)
        ) {
            Text(
                text = detail.item.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = buildList {
                    add("${formatQuantity(detail.item.quantity)} ${detail.item.unit.label}")
                    if (detail.item.location.isNotBlank()) add(detail.item.location)
                    if (detail.item.totalValueMinor > 0) {
                        add(Money.format(detail.item.totalValueMinor, symbol))
                    }
                }.joinToString(" · "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        RowAction(
            label = "Keep",
            icon = Icons.AutoMirrored.Filled.Undo,
            onClick = onUndo,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** A suggestion line with the three ways to resolve it. */
@Composable
private fun SuggestionRow(
    detail: InventoryItemDetail,
    symbol: String,
    today: LocalDate,
    detailLine: String?,
    actions: InventoryActions,
    onClick: () -> Unit,
    showKeepAction: Boolean,
) {
    Column {
        InventoryRow(
            detail = detail,
            symbol = symbol,
            today = today,
            onClick = onClick,
            trailing = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    RowAction(
                        label = "Donate",
                        icon = Icons.Filled.VolunteerActivism,
                        color = moneyColors.positive,
                        onClick = { actions.flagForDonation(detail.item.id) },
                    )
                    RowAction(
                        label = "Bin",
                        icon = Icons.Filled.DeleteOutline,
                        color = moneyColors.danger,
                        onClick = { actions.flagForDiscard(detail.item.id) },
                    )
                    if (showKeepAction) {
                        RowAction(
                            label = "Keep",
                            icon = Icons.Filled.CheckCircle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            onClick = { actions.markUsedToday(detail.item.id) },
                        )
                    }
                }
            },
        )
        if (detailLine != null) {
            Text(
                text = detailLine,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 68.dp, end = 16.dp, bottom = 6.dp),
            )
        }
    }
}

private fun idleDescription(detail: InventoryItemDetail, today: LocalDate): String {
    val since = detail.item.lastUsedEpochDay ?: detail.item.purchaseEpochDay ?: return ""
    val months = (today.toEpochDay() - since) / 30
    val verb = if (detail.item.lastUsedEpochDay != null) "Last used" else "Bought"
    return if (months >= 12) {
        "$verb about ${months / 12} year${if (months / 12 == 1L) "" else "s"} ago"
    } else {
        "$verb about $months month${if (months == 1L) "" else "s"} ago"
    }
}
