package com.painkiller

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.painkiller.ui.navigation.PainkillerNavGraph
import com.painkiller.ui.theme.PainkillerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as PainkillerApplication).container
        setContent {
            PainkillerTheme {
                PainkillerNavGraph(container = container)
            }
        }
    }
}
