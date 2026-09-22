package com.inception.android

import android.app.Application
import android.util.Log

/**
 * Main Application class for Inception Android.
 * Orchestrates application-wide lifecycle and foundations.
 */
class InceptionApplication : Application() {

    companion object {
        const val TAG = "InceptionApp"
        lateinit var instance: InceptionApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.i(TAG, "Inception Android initialized.")
    }
}
