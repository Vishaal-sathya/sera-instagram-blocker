package com.example.leetcodegate.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import com.example.leetcodegate.MainActivity
import com.example.leetcodegate.ui.LockScreenOverlay

class AccessibilityOverlayManager(private val service: AccessibilityService) {
    private val windowManager = service.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: ComposeView? = null
    private var lifecycleOwner: ServiceLifecycleOwner? = null
    private var isShowing = false

    fun showOverlay() {
        if (isShowing) return

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        lifecycleOwner = ServiceLifecycleOwner()

        overlayView = ComposeView(service).apply {
            isFocusableInTouchMode = true
            requestFocus()
            setOnKeyListener { _, keyCode, event ->
                if (keyCode == android.view.KeyEvent.KEYCODE_BACK && event.action == android.view.KeyEvent.ACTION_UP) {
                    removeOverlay()
                    val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_HOME)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    service.startActivity(homeIntent)
                    true
                } else {
                    false
                }
            }
            setContent {
                com.example.leetcodegate.ui.DystopianTheme {
                    LockScreenOverlay(
                        onUnlockClicked = {
                            removeOverlay() // Eagerly remove to prevent visual flicker
                            val intent = Intent(service, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                putExtra("start_destination", "verification")
                            }
                            service.startActivity(intent)
                        },
                        onExitClicked = {
                        removeOverlay()
                        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                            addCategory(Intent.CATEGORY_HOME)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        service.startActivity(homeIntent)
                    }
                )
                } // closes DystopianTheme
            }
        }

        lifecycleOwner?.attachTo(overlayView!!)
        lifecycleOwner?.start()

        try {
            windowManager.addView(overlayView, params)
            isShowing = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun removeOverlay() {
        if (!isShowing) return
        try {
            lifecycleOwner?.stop()
            overlayView?.let { 
                it.disposeComposition()
                windowManager.removeView(it) 
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        overlayView = null
        lifecycleOwner = null
        isShowing = false
    }
}
