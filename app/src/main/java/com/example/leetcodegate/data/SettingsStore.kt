package com.example.leetcodegate.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.leetcodegate.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import androidx.datastore.preferences.core.Preferences

data class LlmConfig(
    val apiKey: String,
    val baseUrl: String,
    val model: String
)

class SettingsStore(private val context: Context) {
    companion object {
        val API_KEY = stringPreferencesKey("nim_api_key")
        val BASE_URL = stringPreferencesKey("nim_base_url")
        val MODEL = stringPreferencesKey("nim_model")
    }

    val llmConfig: Flow<LlmConfig> = context.dataStore.data.map { preferences ->
        LlmConfig(
            apiKey = preferences[API_KEY] ?: BuildConfig.NIM_API_KEY,
            baseUrl = preferences[BASE_URL] ?: BuildConfig.NIM_BASE_URL,
            model = preferences[MODEL] ?: BuildConfig.NIM_MODEL
        )
    }

    suspend fun updateConfig(apiKey: String, baseUrl: String, model: String) {
        context.dataStore.edit { preferences ->
            preferences[API_KEY] = apiKey
            preferences[BASE_URL] = baseUrl
            preferences[MODEL] = model
        }
    }

    suspend fun getLlmConfigSync(): LlmConfig {
        return llmConfig.first()
    }
}
