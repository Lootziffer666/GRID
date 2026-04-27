package com.painkiller

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.painkiller.ui.navigation.PainkillerNavGraph
import com.painkiller.ui.theme.PainkillerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        val container = (application as PainkillerApplication).container
        setContent {
            var darkModeEnabled by rememberSaveable { mutableStateOf(false) }
            PainkillerTheme(darkTheme = darkModeEnabled) {
                PainkillerNavGraph(
                    container = container,
                    darkModeEnabled = darkModeEnabled,
                    onToggleDarkMode = { darkModeEnabled = !darkModeEnabled },
                )
            }
        }
    }
}
