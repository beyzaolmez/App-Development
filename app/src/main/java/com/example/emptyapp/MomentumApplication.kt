package com.example.emptyapp

import android.app.Application
import com.example.emptyapp.notification.NotificationHelper

class MomentumApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannel(this)
    }
}
