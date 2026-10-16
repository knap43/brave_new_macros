package com.knap43.macros

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.knap43.macros.ui.activity.ActivityScreen
import com.knap43.macros.ui.editor.EditorScreen
import com.knap43.macros.ui.home.HomeScreen
import com.knap43.macros.ui.theme.MacrosTheme

/** Which screen is up: null = the list, ACTIVITY = the log, NEW = a fresh draft, otherwise a macro id. */
private const val ACTIVITY = "activity"
private const val NEW = "new"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        val app = application as MacrosApp

        setContent {
            MacrosTheme {
                var open by rememberSaveable { mutableStateOf<String?>(null) }
                // The editor handles its own back press (to offer saving); the log just closes.
                BackHandler(enabled = open == ACTIVITY) { open = null }

                AnimatedContent(
                    targetState = open,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "screen",
                ) { target ->
                    when (target) {
                        null -> HomeScreen(
                            store = app.store,
                            appScope = app.appScope,
                            onOpen = { open = it },
                            onNew = { open = NEW },
                            onActivity = { open = ACTIVITY },
                        )
                        ACTIVITY -> ActivityScreen(app.store, app.appScope, onClose = { open = null })
                        else -> EditorScreen(
                            store = app.store,
                            appScope = app.appScope,
                            macroId = target.takeIf { it != NEW },
                            onClose = { open = null },
                        )
                    }
                }
            }
        }
    }
}
