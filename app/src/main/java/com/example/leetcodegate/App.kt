package com.example.leetcodegate

import android.app.Application
import com.example.leetcodegate.data.CompletedProblemStore
import com.example.leetcodegate.data.CreditStore
import com.example.leetcodegate.data.SettingsStore
import com.example.leetcodegate.domain.CreditManager
import com.example.leetcodegate.domain.InstagramUsageTracker
import com.google.gson.Gson
import okhttp3.OkHttpClient
import com.example.leetcodegate.llm.LlmValidator
import com.example.leetcodegate.ocr.OcrEngine
import com.example.leetcodegate.ocr.ProblemExtractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(private val application: Application) {
    // Application-wide CoroutineScope tied to the app lifecycle
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // DataStore Repositories
    val creditStore by lazy { CreditStore(application) }
    val completedProblemStore by lazy { CompletedProblemStore(application) }
    val settingsStore by lazy { SettingsStore(application) }

    // Shared Utilities
    val okHttpClient by lazy { 
        OkHttpClient.Builder()
            .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
            .build()
    }
    val gson by lazy { Gson() }

    // Subsystems
    val llmValidator by lazy { LlmValidator(okHttpClient, gson) }
    val ocrEngine by lazy { OcrEngine(application) }
    val problemExtractor by lazy { ProblemExtractor() }

    // Core Domain Logic
    val creditManager by lazy { CreditManager(creditStore) }
    val systemTimeProvider by lazy { com.example.leetcodegate.data.SystemTimeProvider() }
    val instagramUsageTracker by lazy { InstagramUsageTracker(creditManager, systemTimeProvider, applicationScope) }
}

class App : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        
        // Eagerly recover any time lost while the app was killed
        container.applicationScope.launch {
            container.creditManager.recoverAccurateTime()
        }
    }
}
