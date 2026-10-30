package com.gtavoapps.portylus.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeContrastTest {
    private fun contrast(a: Color, b: Color): Float {
        val lighter = maxOf(a.luminance(), b.luminance())
        val darker = minOf(a.luminance(), b.luminance())
        return (lighter + 0.05f) / (darker + 0.05f)
    }

    @Test fun bothThemesKeepTextAndControlBoundariesReadable() {
        listOf("light" to PortylusLightColors, "dark" to PortylusDarkColors).forEach { (theme, c) ->
            val textPairs = mapOf(
                "primary" to (c.primary to c.onPrimary),
                "primary container" to (c.primaryContainer to c.onPrimaryContainer),
                "secondary" to (c.secondary to c.onSecondary),
                "secondary container" to (c.secondaryContainer to c.onSecondaryContainer),
                "tertiary" to (c.tertiary to c.onTertiary),
                "tertiary container" to (c.tertiaryContainer to c.onTertiaryContainer),
                "surface" to (c.surface to c.onSurface),
                "secondary text" to (c.surface to c.onSurfaceVariant),
                "dialog" to (c.surfaceContainerHigh to c.onSurface),
                "workspace" to (c.surfaceContainer to c.onSurfaceVariant),
                "error" to (c.error to c.onError),
                "error container" to (c.errorContainer to c.onErrorContainer),
                "snackbar" to (c.inverseSurface to c.inverseOnSurface),
                "snackbar action" to (c.inverseSurface to c.inversePrimary))
            textPairs.forEach { (role, pair) ->
                assertTrue("$theme $role: ${contrast(pair.first, pair.second)}", contrast(pair.first, pair.second) >= 4.5f)
            }
            assertTrue("$theme input outline", contrast(c.surface, c.outline) >= 3f)
        }
    }
}
