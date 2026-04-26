package com.painkiller.app.ui

import androidx.compose.runtime.Composable
import com.painkiller.app.ui.screens.PainkillerPlaceholderScreen
import com.painkiller.app.ui.theme.PainkillerTheme

/**
 * Top-level Compose entry point for the app.
 *
 * Gate 0 only renders a placeholder screen that demonstrates the
 * Painkiller theme and reusable components. The real upload flow lands
 * in later gates.
 */
@Composable
fun PainkillerApp() {
    PainkillerTheme {
        PainkillerPlaceholderScreen()
    }
}
