package io.buszi.nascope

import android.app.Application
import android.content.Context
import io.buszi.nascope.di.initNascope
import org.koin.dsl.module

class NascopeApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        initNascope(isDebug = BuildConfig.DEBUG) {
            modules(module { single<Context> { applicationContext } })
        }
    }
}
