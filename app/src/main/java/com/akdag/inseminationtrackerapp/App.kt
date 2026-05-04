package com.akdag.inseminationtrackerapp

import android.app.Application

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        PremiumManager.init(this)
    }
}
