package com.zive.moneymanager.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.dp

/**
 * On-screen number pad. Entering an amount is the one thing this app does more
 * than anything else, so it gets large targets and stays reachable one-handed
 * instead of relying on the system keyboard.
 */
@Composable
fun AmountKeypad(
    onDigit: (Char) -> Unit,
    onDecimal: () -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onSave: () -> Unit,
    saveEnabled: Boolean,
    saveLabel: String,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current

    fun tap(action: () -> Unit): () -> Unit = {
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        action()
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 3.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
            ).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { key ->
                        KeypadKey(
                            label = key,
                            onClick = tap { onDigit(key[0]) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                KeypadKey(label = ".", onClick = tap(onDecimal), modifier = Modifier.weight(1f))
                KeypadKey(label = "0", onClick = tap { onDigit('0') }, modifier = Modifier.weight(1f))
                TextButton(
                    onClick = tap(onBackspace),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Backspace,
                        contentDescription = "Delete last digit",
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButton(
                    onClick = tap(onClear),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                ) {
                    Text("Clear")
                }
                Button(
                    onClick = onSave,
                    enabled = saveEnabled,
                    modifier = Modifier
                        .weight(2f)
                        .height(52.dp),
                ) {
                    Text(saveLabel)
                }
            }
        }
    }
}

@Composable
private fun KeypadKey(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.height(52.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
