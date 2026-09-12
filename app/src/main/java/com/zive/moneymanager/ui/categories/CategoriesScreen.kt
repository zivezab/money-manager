package com.zive.moneymanager.ui.categories

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zive.moneymanager.data.db.entity.Category
import com.zive.moneymanager.data.db.entity.IncomeKind
import com.zive.moneymanager.data.db.entity.TxType
import com.zive.moneymanager.ui.components.IconBadge
import com.zive.moneymanager.ui.components.TagChip

/** Manage the category tree used to classify income and expenses. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    state: CategoriesUiState,
    onBack: () -> Unit,
    onSave: (Long, String, TxType, Long?, String, String, IncomeKind, Boolean) -> Unit,
    onDelete: (Category) -> Unit,
) {
    var side by remember { mutableStateOf(TxType.EXPENSE) }
    var editing by remember { mutableStateOf<Category?>(null) }
    var creatingUnder by remember { mutableStateOf<Long?>(null) }
    var creating by remember { mutableStateOf(false) }

    val tree = state.tree(side)

    Scaffold(
        // The root scaffold already inset for the status and navigation bars;
        // taking them again here would double the gap under the top bar.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = { Text("Categories") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                creatingUnder = null
                creating = true
            }) {
                Icon(Icons.Filled.Add, contentDescription = "New category")
            }
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                listOf(TxType.EXPENSE, TxType.INCOME).forEachIndexed { index, type ->
                    SegmentedButton(
                        selected = side == type,
                        onClick = { side = type },
                        shape = SegmentedButtonDefaults.itemShape(index, 2),
                        label = { Text(if (type == TxType.EXPENSE) "Expense" else "Income") },
                    )
                }
            }

            LazyColumn(contentPadding = PaddingValues(bottom = 100.dp)) {
                items(tree, key = { it.first.id }) { (parent, children) ->
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { editing = parent }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconBadge(parent.iconKey, parent.colorHex)
                            Column(
                                Modifier
                                    .weight(1f)
                                    .padding(horizontal = 12.dp)
                            ) {
                                Text(
                                    text = parent.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (parent.type == TxType.INCOME && parent.incomeKind != IncomeKind.NONE) {
                                        TagChip(
                                            text = if (parent.incomeKind == IncomeKind.JOB) "From work" else "Not from work",
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                    if (parent.archived) {
                                        TagChip("Archived")
                                    }
                                    if (children.isNotEmpty()) {
                                        TagChip("${children.size} sub")
                                    }
                                }
                            }
                            TextButton(onClick = {
                                creatingUnder = parent.id
                                creating = true
                            }) { Text("Add sub") }
                        }

                        children.forEach { child ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { editing = child }
                                    .padding(start = 68.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = child.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (child.archived) TagChip("Archived")
                            }
                        }

                        HorizontalDivider()
                    }
                }
            }
        }
    }

    if (creating || editing != null) {
        CategoryEditorSheet(
            category = editing,
            defaultType = side,
            defaultParentId = creatingUnder,
            parents = tree.map { it.first },
            onDismiss = {
                creating = false
                creatingUnder = null
                editing = null
            },
            onSave = { id, name, type, parentId, color, icon, kind, archived ->
                onSave(id, name, type, parentId, color, icon, kind, archived)
                creating = false
                creatingUnder = null
                editing = null
            },
            onDelete = { category ->
                onDelete(category)
                creating = false
                creatingUnder = null
                editing = null
            },
        )
    }
}
