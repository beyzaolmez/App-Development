package com.nhlstenden.momentum.util

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View

object HapticHelper {
    // Called when a quest is marked complete. Uses the system haptic engine so the
    // feedback is automatically suppressed when the user has haptic feedback disabled.
    fun questComplete(view: View) {
        view.post {
            val handled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            } else {
                false
            }

            if (!handled) {
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            }
        }
    }
}
