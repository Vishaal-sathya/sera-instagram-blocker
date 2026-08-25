package com.example.leetcodegate.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlin.math.max
import kotlin.math.min

import androidx.datastore.preferences.core.booleanPreferencesKey

class CreditStore(private val context: Context) {
    companion object {
        val CREDIT_SECONDS = intPreferencesKey("credit_seconds")
        val LAST_PERSISTED_AT = longPreferencesKey("last_credit_persisted_at")
        val IS_TRACKING = booleanPreferencesKey("is_tracking")
    }

    val creditSeconds: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[CREDIT_SECONDS] ?: 0
    }

    suspend fun setTrackingState(isTracking: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_TRACKING] = isTracking
        }
    }
    
    suspend fun updateCredit(seconds: Int) {
        context.dataStore.edit { preferences ->
            preferences[CREDIT_SECONDS] = seconds
            preferences[LAST_PERSISTED_AT] = System.currentTimeMillis()
        }
    }

    suspend fun addCreditTransaction(seconds: Int, maxCredit: Int) {
        context.dataStore.edit { preferences ->
            val current = preferences[CREDIT_SECONDS] ?: 0
            preferences[CREDIT_SECONDS] = min(current + seconds, maxCredit)
            preferences[LAST_PERSISTED_AT] = System.currentTimeMillis()
        }
    }

    suspend fun consumeCreditTransaction(seconds: Int) {
        context.dataStore.edit { preferences ->
            val current = preferences[CREDIT_SECONDS] ?: 0
            preferences[CREDIT_SECONDS] = max(0, current - seconds)
            preferences[LAST_PERSISTED_AT] = System.currentTimeMillis()
        }
    }

    suspend fun getCreditSync(): Int {
        return creditSeconds.first()
    }
}
