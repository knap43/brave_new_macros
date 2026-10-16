package com.knap43.macros

import android.app.Application
import com.knap43.macros.data.MacroStore
import com.knap43.macros.engine.Prompts
import com.knap43.macros.engine.Shell
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class MacrosApp : Application() {
    /** Outlives any screen, so a save or test run started on the way out always finishes. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val store by lazy { MacroStore.get(this) }

    override fun onCreate() {
        super.onCreate()
        Shell.init()
        Prompts.createChannel(this)
    }
}
