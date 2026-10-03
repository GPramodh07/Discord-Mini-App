package com.example.discordappmin

import android.app.Application

class DiscordApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: DiscordApplication
            private set
    }
}
