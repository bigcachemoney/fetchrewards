package com.example.fetchrewards

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

class ItemViewModel : ViewModel() {
    private val _items = MutableStateFlow<List<Item>>(emptyList())
    val items: StateFlow<List<Item>> = _items

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _expandedGroups = MutableStateFlow<Set<Int>>(setOf())
    val expandedGroups: StateFlow<Set<Int>> = _expandedGroups

    init {
        fetchItems()
    }

    fun fetchItems() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val response = fetchItemsFromApi()

                val processedItems = response
                    .filter { !it.name.isNullOrEmpty() }
                    .sortedBy { it.name }

                _items.value = processedItems
            } catch (e: Exception) {
                _error.value = "Failed to load items: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun fetchItemsFromApi(): List<Item> = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://fetch-hiring.s3.amazonaws.com/hiring.json")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val apiResponse = connection.inputStream.bufferedReader().use { it.readText() }
                Log.d("API_RESPONSE", "First 500 chars: ${apiResponse.take(500)}")

                val json = Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    coerceInputValues = true
                }

                val items = json.decodeFromString<List<Item>>(apiResponse)
                return@withContext items.filter { !it.name.isNullOrEmpty() }
                    .sortedWith(compareBy({ it.listId }, {
                        it.name?.replace("Item ", "")?.toIntOrNull() ?: 0
                    }))
            } else {
                Log.e("ItemViewModel", "HTTP Error: $responseCode")
                return@withContext emptyList()
            }
        } catch (e: Exception) {
            Log.e("ItemViewModel", "Error fetching items: ${e.message}", e)
            return@withContext emptyList()
        }
    }

    fun toggleGroup(listId: Int) {
        val currentExpanded = _expandedGroups.value.toMutableSet()
        if (currentExpanded.contains(listId)) {
            currentExpanded.remove(listId)
        } else {
            currentExpanded.add(listId)
        }
        _expandedGroups.value = currentExpanded
    }
}
