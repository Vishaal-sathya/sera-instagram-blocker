package com.example.leetcodegate.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.accessibility.AccessibilityEvent
import com.example.leetcodegate.App
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class InstagramAccessibilityService : AccessibilityService() {

    private val INSTAGRAM_PACKAGE = "com.instagram.android"
    
    private val appContainer by lazy { (application as App).container }
    private val usageTracker by lazy { appContainer.instagramUsageTracker }
    private val creditManager by lazy { appContainer.creditManager }
    
    private val foregroundDetector = ForegroundAppDetector()
    private lateinit var overlayManager: AccessibilityOverlayManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val isInstagramForeground = MutableStateFlow(false)

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                isInstagramForeground.value = false
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        overlayManager = AccessibilityOverlayManager(this)
        
        val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)
        registerReceiver(screenReceiver, filter)

        serviceScope.launch {
            combine(
                creditManager.getCreditFlow(),
                isInstagramForeground
            ) { creditSeconds, isForeground ->
                Pair(creditSeconds, isForeground)
            }.collectLatest { (creditSeconds, isForeground) ->
                if (isForeground) {
                    usageTracker.startTracking()
                    if (creditSeconds <= 0) {
                        overlayManager.showOverlay()
                    } else {
                        overlayManager.removeOverlay()
                    }
                } else {
                    usageTracker.stopTracking()
                    overlayManager.removeOverlay()
                }
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (foregroundDetector.processEvent(event)) {
            val currentApp = foregroundDetector.currentForegroundPackage
            isInstagramForeground.value = (currentApp == INSTAGRAM_PACKAGE)
        }
    }

    override fun onInterrupt() {
        isInstagramForeground.value = false
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(screenReceiver)
        serviceScope.cancel()
        usageTracker.stopTracking()
        overlayManager.removeOverlay()
    }
}
