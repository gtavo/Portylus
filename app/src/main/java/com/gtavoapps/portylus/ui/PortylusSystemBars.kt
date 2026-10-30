package com.gtavoapps.portylus.ui

import androidx.activity.ComponentActivity
import android.graphics.Color
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.LocalActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowInsetsControllerCompat

/** Match system icons to the selected app theme, including a manual override of the device theme. */
@Composable fun PortylusSystemBars() {
    val activity = LocalActivity.current as? ComponentActivity ?: return
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val surface = MaterialTheme.colorScheme.surface.toArgb()
    SideEffect {
        activity.enableEdgeToEdge(
            statusBarStyle = if (dark) SystemBarStyle.dark(Color.TRANSPARENT)
                else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = if (dark) SystemBarStyle.dark(surface) else SystemBarStyle.light(surface, surface))
        activity.window.isNavigationBarContrastEnforced = true
        WindowInsetsControllerCompat(activity.window, activity.window.decorView).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
    }
}
