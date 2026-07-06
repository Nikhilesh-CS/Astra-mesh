package com.astramesh.app.engine

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * ChatSearchEngine
 * Manages search queries, highlighting, jumping between results,
 * and filtering by media type.
 */
class ChatSearchEngine(private val conversationEngine: ConversationEngine) {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _searchResults = MutableStateFlow<List<String>>(emptyList())
    val searchResults: StateFlow<List<String>> = _searchResults
    
    private val _currentResultIndex = MutableStateFlow(-1)
    val currentResultIndex: StateFlow<Int> = _currentResultIndex

    fun updateQuery(query: String) {
        _searchQuery.value = query
        if (query.isEmpty()) {
            _searchResults.value = emptyList()
            _currentResultIndex.value = -1
            return
        }

        // Perform case-insensitive search across all messages
        val results = conversationEngine.messages.value.filter { 
            it.text.contains(query, ignoreCase = true)
        }.map { it.id }
        
        _searchResults.value = results
        _currentResultIndex.value = if (results.isNotEmpty()) 0 else -1
    }

    fun nextResult() {
        val count = _searchResults.value.size
        if (count == 0) return
        _currentResultIndex.value = (_currentResultIndex.value + 1) % count
    }

    fun previousResult() {
        val count = _searchResults.value.size
        if (count == 0) return
        val current = _currentResultIndex.value
        _currentResultIndex.value = if (current - 1 < 0) count - 1 else current - 1
    }
    
    fun clearSearch() {
        updateQuery("")
    }
}
