package com.nhlstenden.momentum.util

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View

object HapticHelper {
    // Called when a quest is marked complete. Uses the system haptic engine so the
    // feedback is automatically suppressed when the user has haptic feedback disabled.
    fun questComplete(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // CONFIRM (API 30+) gives a two-pulse "done" pattern — stronger than a click
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        } else {
            // LONG_PRESS is the closest available equivalent on API 24-29
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
    }
}
