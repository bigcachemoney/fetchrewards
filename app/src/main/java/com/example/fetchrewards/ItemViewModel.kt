package com.example.fetchrewards

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ItemViewModel : ViewModel() {
    private val _groupedItems = MutableStateFlow<Map<Int, List<Item>>>(emptyMap())
    val groupedItems: StateFlow<Map<Int, List<Item>>> = _groupedItems.asStateFlow()

    init {
        loadItems()
    }

    private fun loadItems() {
        val sampleData = fetchSampleData()

        val filteredItems = sampleData.filter { !it.name.isNullOrBlank() }

        val sortedItems = filteredItems.sortedWith(
            compareBy<Item> { it.listId }.thenBy { it.name }
        )

        val grouped = sortedItems.groupBy { it.listId }

        _groupedItems.value = grouped
    }

    private fun fetchSampleData(): List<Item> {
        return listOf(
            Item(755, 2, ""),
            Item(203, 2, ""),
            Item(684, 1, "Item 684"),
            Item(276, 1, "Item 276"),
            Item(736, 3, null),
            Item(926, 4, null),
            Item(808, 4, "Item 808")
        )
    }
}
