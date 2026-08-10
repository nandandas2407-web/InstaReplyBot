package com.instareply

import android.app.Application
import com.instareply.data.db.AppDatabase

class InstaReplyApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: InstaReplyApp
            private set
    }
}
