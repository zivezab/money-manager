package com.zive.moneymanager.ui.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zive.moneymanager.data.db.entity.Category
import com.zive.moneymanager.data.db.entity.IncomeKind
import com.zive.moneymanager.data.db.entity.TxType
import com.zive.moneymanager.data.repo.FinanceRepository
import com.zive.moneymanager.di.AppContainer
import com.zive.moneymanager.di.containerFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CategoriesUiState(
    val all: List<Category> = emptyList(),
    val loading: Boolean = true,
) {
    /** Parents of [type] paired with their children, in display order. */
    fun tree(type: TxType): List<Pair<Category, List<Category>>> {
        val ofType = all.filter { it.type == type }
        val childrenByParent = ofType.filter { it.parentId != null }.groupBy { it.parentId }
        return ofType.filter { it.parentId == null }
            .sortedWith(compareBy({ it.archived }, { it.sortOrder }, { it.name }))
            .map { parent ->
                parent to childrenByParent[parent.id].orEmpty().sortedWith(compareBy({ it.sortOrder }, { it.name }))
            }
    }
}

class CategoriesViewModel(
    private val repo: FinanceRepository,
) : ViewModel() {

    val state = repo.observeCategories()
        .map { CategoriesUiState(it, loading = false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CategoriesUiState())

    fun save(
        id: Long,
        name: String,
        type: TxType,
        parentId: Long?,
        colorHex: String,
        iconKey: String,
        incomeKind: IncomeKind,
        archived: Boolean,
    ) {
        viewModelScope.launch {
            val existing = if (id != 0L) repo.category(id) else null
            repo.upsertCategory(
                (existing ?: Category(name = name, type = type)).copy(
                    id = id,
                    name = name.trim(),
                    type = type,
                    parentId = parentId,
                    colorHex = colorHex,
                    iconKey = iconKey,
                    incomeKind = if (type == TxType.INCOME) incomeKind else IncomeKind.NONE,
                    archived = archived,
                )
            )
        }
    }

    /** Transactions keep their history: the category reference is nulled, not cascaded. */
    fun delete(category: Category) {
        viewModelScope.launch { repo.deleteCategory(category) }
    }

    companion object {
        val Factory = containerFactory { c: AppContainer ->
            CategoriesViewModel(c.repository)
        }
    }
}
