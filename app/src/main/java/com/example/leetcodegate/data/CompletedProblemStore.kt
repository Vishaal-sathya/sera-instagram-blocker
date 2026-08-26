package com.example.leetcodegate.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import androidx.datastore.preferences.core.Preferences

class CompletedProblemStore(private val context: Context) {
    companion object {
        val COMPLETED_PROBLEMS = stringSetPreferencesKey("completed_problems")
    }

    val completedProblems: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[COMPLETED_PROBLEMS] ?: emptySet()
    }

    suspend fun addCompleted(problemId: String) {
        val normalizedId = problemId.trim().uppercase()
        context.dataStore.edit { preferences ->
            val current = preferences[COMPLETED_PROBLEMS] ?: emptySet()
            preferences[COMPLETED_PROBLEMS] = current + normalizedId
        }
    }

    suspend fun removeCompleted(problemId: String) {
        val normalizedId = problemId.trim().uppercase()
        context.dataStore.edit { preferences ->
            val current = preferences[COMPLETED_PROBLEMS] ?: emptySet()
            preferences[COMPLETED_PROBLEMS] = current - normalizedId
        }
    }
    
    suspend fun isCompleted(problemId: String): Boolean {
        val normalizedId = problemId.trim().uppercase()
        return completedProblems.first().contains(normalizedId)
    }

    suspend fun clearAll() {
        context.dataStore.edit { preferences ->
            preferences[COMPLETED_PROBLEMS] = emptySet()
        }
    }
}
