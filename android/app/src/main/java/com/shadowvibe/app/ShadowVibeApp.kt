package com.shadowvibe.app

import android.app.Application
import com.shadowvibe.app.data.api.ApiClient

class ShadowVibeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ApiClient.init(this)
    }
}
