package com.example.leetcodegate.domain

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.sync.withLock

class InstagramUsageTracker(
    private val creditManager: CreditManager,
    private val timeProvider: TimeProvider,
    private val coroutineScope: CoroutineScope
) {
    private val isTracking = AtomicBoolean(false)
    private val trackingJob = AtomicReference<Job?>(null)
    
    // We use elapsedRealtime to prevent users from cheating by changing the device clock
    private val lastUpdateRealtime = AtomicLong(0L)
    // To prevent the micro-session cheat (opening < 1000ms repeatedly)
    private val residualMillis = AtomicLong(0L)

    private val mutex = kotlinx.coroutines.sync.Mutex()

    fun startTracking() {
        if (!isTracking.compareAndSet(false, true)) return
        
        coroutineScope.launch {
            creditManager.setTrackingState(true)
        }
        
        val newJob = coroutineScope.launch {
            // Subtract residualMillis before starting
            mutex.withLock {
                lastUpdateRealtime.set(timeProvider.elapsedRealtime() - residualMillis.get())
            }

            while (isActive && isTracking.get()) {
                delay(1000L) // Check every second
                mutex.withLock {
                    if (!isTracking.get()) return@withLock
                    val now = timeProvider.elapsedRealtime()
                    val elapsedMillis = now - lastUpdateRealtime.get()
                    val elapsedSeconds = (elapsedMillis / 1000).toInt()
                    
                    if (elapsedSeconds > 0) {
                        lastUpdateRealtime.addAndGet(elapsedSeconds * 1000L)
                        creditManager.consumeCredit(elapsedSeconds)
                    }
                }
            }
        }
        trackingJob.getAndSet(newJob)?.cancel()
    }

    fun stopTracking() {
        if (!isTracking.compareAndSet(true, false)) return
        
        trackingJob.getAndSet(null)?.cancel()
        
        coroutineScope.launch {
            mutex.withLock {
                // Final flush of remaining time just in case, and save residual
                val now = timeProvider.elapsedRealtime()
                val elapsedMillis = now - lastUpdateRealtime.get()
                val elapsedSeconds = (elapsedMillis / 1000).toInt()
                
                if (elapsedSeconds > 0) {
                    creditManager.stopTrackingAndConsume(elapsedSeconds)
                } else {
                    creditManager.setTrackingState(false)
                }
                
                // Store the remaining milliseconds < 1000 for the next session
                residualMillis.set(elapsedMillis % 1000)
            }
        }
    }
}
