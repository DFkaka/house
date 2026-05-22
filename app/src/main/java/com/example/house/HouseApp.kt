
package com.example.house

import android.app.Application
import com.example.house.di.AppContainer

class HouseApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
