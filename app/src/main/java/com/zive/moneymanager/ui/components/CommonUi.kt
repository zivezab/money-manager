package com.zive.moneymanager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zive.moneymanager.core.Money
import com.zive.moneymanager.data.db.entity.TxType
import com.zive.moneymanager.ui.theme.AmountStyle
import com.zive.moneymanager.ui.theme.colorFromHex
import com.zive.moneymanager.ui.theme.moneyColors

/** Circular tinted badge holding a category or account icon. */
@Composable
fun IconBadge(
    iconKey: String?,
    colorHex: String?,
    modifier: Modifier = Modifier,
    size: Int = 40,
    fallbackIcon: ImageVector? = null,
) {
    val base = colorFromHex(colorHex, MaterialTheme.colorScheme.primary)
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(base.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = fallbackIcon ?: CategoryIcons[iconKey],
            contentDescription = null,
            tint = base,
            modifier = Modifier.size((size * 0.52f).dp),
        )
    }
}

/** Amount rendered in the colour its transaction type implies. */
@Composable
fun AmountText(
    amountMinor: Long,
    type: TxType,
    symbol: String,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = AmountStyle,
) {
    val colors = moneyColors
    val (prefix, color) = when (type) {
        TxType.INCOME -> "+" to colors.income
        TxType.EXPENSE -> "-" to colors.expense
        TxType.TRANSFER -> "" to colors.transfer
    }
    Text(
        text = "$prefix$symbol${Money.formatPlain(amountMinor)}",
        color = color,
        style = style,
        maxLines = 1,
        modifier = modifier,
    )
}

/** Small pill label, used for account names and tags on list rows. */
@Composable
fun TagChip(
    text: String,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.10f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

/** Slim horizontal meter used by budgets and the category ranking list. */
@Composable
fun ProgressBar(
    fraction: Float,
    color: Color,
    modifier: Modifier = Modifier,
    height: Int = 8,
    trackAlpha: Float = 0.16f,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp)
            .clip(RoundedCornerShape(height.dp))
            .background(color.copy(alpha = trackAlpha)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(height.dp)
                .clip(RoundedCornerShape(height.dp))
                .background(color),
        )
    }
}

/** Centered message shown when a list has nothing in it yet. */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(48.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (subtitle != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Section heading with an optional trailing action. */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        trailing?.invoke()
    }
}

/** Shared list padding so screens with a FAB scroll clear of it. */
val ScreenListPadding = PaddingValues(start = 0.dp, end = 0.dp, top = 0.dp, bottom = 96.dp)
