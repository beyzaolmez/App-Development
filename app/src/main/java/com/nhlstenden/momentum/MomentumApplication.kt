package com.nhlstenden.momentum

import android.app.Application
import com.nhlstenden.momentum.notification.NotificationHelper

class MomentumApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannel(this)
    }
}
