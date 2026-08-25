package com.example.leetcodegate.data

import android.os.SystemClock
import com.example.leetcodegate.domain.TimeProvider

class SystemTimeProvider : TimeProvider {
    override fun elapsedRealtime(): Long {
        return SystemClock.elapsedRealtime()
    }
}
