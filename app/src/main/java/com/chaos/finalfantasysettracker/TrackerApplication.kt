package com.chaos.finalfantasysettracker

import android.app.Application
import com.chaos.finalfantasysettracker.data.AppContainer

class TrackerApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
