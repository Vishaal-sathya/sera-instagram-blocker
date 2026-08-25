package com.example.leetcodegate.accessibility

import android.view.accessibility.AccessibilityEvent

class ForegroundAppDetector {
    var currentForegroundPackage: String? = null
        private set

    private val ignoredPackages = setOf(
        "com.android.systemui",
        "com.google.android.inputmethod.latin",
        "com.sec.android.inputmethod"
    )

    fun processEvent(event: AccessibilityEvent): Boolean {
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString()
            val className = event.className?.toString()
            
            // Ignore events from our own overlay to prevent flicker loops
            if (packageName == "com.example.leetcodegate" && className != "com.example.leetcodegate.MainActivity") {
                return false
            }
            
            // Ignore system UI and keyboards
            if (ignoredPackages.contains(packageName)) {
                return false
            }
            
            if (packageName != null && packageName != currentForegroundPackage) {
                currentForegroundPackage = packageName
                return true
            }
        }
        return false
    }
}
