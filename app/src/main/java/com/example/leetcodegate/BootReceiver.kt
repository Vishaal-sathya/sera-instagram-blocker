package com.example.leetcodegate

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            // The system spins up our process to deliver this broadcast, which triggers App.onCreate()
            // This ensures our eager time recovery (CreditStore.recoverAccurateTime()) runs immediately on boot.
        }
    }
}
