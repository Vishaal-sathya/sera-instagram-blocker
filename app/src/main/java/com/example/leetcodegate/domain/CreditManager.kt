package com.example.leetcodegate.domain

import com.example.leetcodegate.data.CreditStore
import kotlinx.coroutines.flow.Flow

class CreditManager(private val creditStore: CreditStore) {
    
    companion object {
        const val MAX_CREDIT_SECONDS = 300 // 5 minutes cap
    }

    fun getCreditFlow(): Flow<Int> = creditStore.creditSeconds

    suspend fun addCredit(seconds: Int) {
        require(seconds >= 0) { "Seconds must be non-negative" }
        creditStore.addCreditTransaction(seconds, MAX_CREDIT_SECONDS)
    }

    suspend fun consumeCredit(seconds: Int) {
        require(seconds >= 0) { "Seconds must be non-negative" }
        creditStore.consumeCreditTransaction(seconds)
    }

    suspend fun stopTrackingAndConsume(seconds: Int) {
        require(seconds >= 0) { "Seconds must be non-negative" }
        creditStore.consumeCreditTransaction(seconds)
        creditStore.setTrackingState(false)
    }

    suspend fun isUnlocked(): Boolean {
        return creditStore.getCreditSync() > 0
    }

    suspend fun setTrackingState(isTracking: Boolean) {
        creditStore.setTrackingState(isTracking)
    }

    suspend fun recoverAccurateTime() {
        // App death shouldn't punish users since OS background killers can kill the app unjustly.
        // Also mitigates Time Spoofing Vulnerability from using System.currentTimeMillis.
        creditStore.setTrackingState(false)
    }
}
