package com.qoocca.parentapp

import android.app.Application

class ParentAppApplication : Application() {
    val appContainer by lazy { AppContainer(this) }
}
