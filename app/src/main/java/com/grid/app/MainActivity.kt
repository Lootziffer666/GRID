package com.grid.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.grid.app.ui.navigation.GridNavGraph
import com.grid.app.ui.theme.GridTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        val container = (application as GridApplication).container
        setContent {
            var darkModeEnabled by rememberSaveable { mutableStateOf(false) }
            GridTheme(darkTheme = darkModeEnabled) {
                GridNavGraph(
                    container = container,
                    darkModeEnabled = darkModeEnabled,
                    onToggleDarkMode = { darkModeEnabled = !darkModeEnabled },
                )
            }
        }
    }
}
