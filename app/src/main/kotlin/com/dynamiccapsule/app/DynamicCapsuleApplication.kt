package com.dynamiccapsule.app

import android.app.Application
import com.dynamiccapsule.core.system.CapsuleServiceLocator

class DynamicCapsuleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CapsuleServiceLocator.initialize(this)
    }
}
