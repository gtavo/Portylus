package com.gtavoapps.portylus.ui
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gtavoapps.portylus.R
import com.gtavoapps.portylus.preferences.AppAppearance

// Fixed brand palette approved in the visual guide. Wallpaper colors must not replace it.
internal val PortylusLightColors = lightColorScheme(
    primary = Color(0xFF315DA8), onPrimary = Color.White,
    primaryContainer = Color(0xFFD8E2FF), onPrimaryContainer = Color(0xFF102F60),
    secondary = Color(0xFF525F78), onSecondary = Color.White,
    secondaryContainer = Color(0xFFDFE6F5), onSecondaryContainer = Color(0xFF1B263B),
    tertiary = Color(0xFF3F6374), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFC3E8FC), onTertiaryContainer = Color(0xFF001F2B),
    background = Color(0xFFF8F9FF), onBackground = Color(0xFF191C24),
    surface = Color(0xFFF8F9FF), onSurface = Color(0xFF191C24),
    surfaceVariant = Color(0xFFE2E6F0), onSurfaceVariant = Color(0xFF434B5B),
    surfaceDim = Color(0xFFD8DCE6), surfaceBright = Color(0xFFF8F9FF),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFF1F3FC),
    surfaceContainer = Color(0xFFEBEEF7), surfaceContainerHigh = Color(0xFFE2E6F0),
    surfaceContainerHighest = Color(0xFFDDE1EC),
    outline = Color(0xFF747D90), outlineVariant = Color(0xFFC4CBD9),
    inverseSurface = Color(0xFF2D313C), inverseOnSurface = Color(0xFFF0F2FC), inversePrimary = Color(0xFFAEC6FF),
    error = Color(0xFFBA1A1A), onError = Color.White,
    errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002),
    surfaceTint = Color(0xFF315DA8)
)
internal val PortylusDarkColors = darkColorScheme(
    primary = Color(0xFFAEC6FF), onPrimary = Color(0xFF102F60),
    primaryContainer = Color(0xFF284777), onPrimaryContainer = Color(0xFFD8E2FF),
    secondary = Color(0xFFBAC6E0), onSecondary = Color(0xFF263249),
    secondaryContainer = Color(0xFF3D475C), onSecondaryContainer = Color(0xFFDCE3F6),
    tertiary = Color(0xFFA7CCE0), onTertiary = Color(0xFF0A3445),
    tertiaryContainer = Color(0xFF264B5D), onTertiaryContainer = Color(0xFFC3E8FC),
    background = Color(0xFF11141C), onBackground = Color(0xFFE1E5F0),
    surface = Color(0xFF11141C), onSurface = Color(0xFFE1E5F0),
    surfaceVariant = Color(0xFF272C38), onSurfaceVariant = Color(0xFFC3CBDD),
    surfaceDim = Color(0xFF11141C), surfaceBright = Color(0xFF363B48),
    surfaceContainerLowest = Color(0xFF0C0F16), surfaceContainerLow = Color(0xFF191D26),
    surfaceContainer = Color(0xFF1D212C), surfaceContainerHigh = Color(0xFF272C38),
    surfaceContainerHighest = Color(0xFF323744),
    outline = Color(0xFF8E98AD), outlineVariant = Color(0xFF434B5B),
    inverseSurface = Color(0xFFE1E5F0), inverseOnSurface = Color(0xFF2D313C), inversePrimary = Color(0xFF315DA8),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
    surfaceTint = Color(0xFFAEC6FF)
)

@Composable fun PortylusTheme(appearance: AppAppearance = AppAppearance.SYSTEM, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (appearance.usesDarkTheme(isSystemInDarkTheme())) PortylusDarkColors else PortylusLightColors,
        typography = Typography(), shapes = Shapes(
            extraSmall = RoundedCornerShape(4.dp), small = RoundedCornerShape(8.dp),
            medium = RoundedCornerShape(12.dp), large = RoundedCornerShape(16.dp),
            extraLarge = RoundedCornerShape(28.dp)), content = content)
}
@Composable fun PortylusBrand(compact: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp)) {
        Image(painterResource(R.drawable.portylus_symbol), contentDescription = null, modifier = Modifier.size(if (compact) 26.dp else 36.dp))
        Column {
            Text("Portylus", fontSize = if (compact) 18.sp else 22.sp, maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            if (!compact) Text("gtavo.apps", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
