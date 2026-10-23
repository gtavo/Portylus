package com.gtavoapps.portylus.ui
import androidx.compose.foundation.Image
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

private val PortylusColors = darkColorScheme(
    primary = Color(0xFF20C9FF), onPrimary = Color(0xFF061426),
    primaryContainer = Color(0xFF0D4882), onPrimaryContainer = Color(0xFFF7FAFF),
    secondary = Color(0xFFAFC2DB), onSecondary = Color(0xFF071A33),
    secondaryContainer = Color(0xFF0B355C), onSecondaryContainer = Color(0xFFF7FAFF),
    tertiary = Color(0xFFFFC857), background = Color(0xFF061426), onBackground = Color(0xFFF7FAFF),
    surface = Color(0xFF071A33), onSurface = Color(0xFFF7FAFF),
    surfaceVariant = Color(0xFF0B2748), onSurfaceVariant = Color(0xFFAFC2DB),
    surfaceContainer = Color(0xFF0B2748), surfaceContainerHigh = Color(0xFF103455),
    surfaceContainerHighest = Color(0xFF143E63),
    outline = Color(0xFF456181), outlineVariant = Color(0xFF234460),
    error = Color(0xFFFF5A6F), onError = Color(0xFF071A33)
)
@Composable fun PortylusTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = PortylusColors, shapes = Shapes(
        small = RoundedCornerShape(10.dp), medium = RoundedCornerShape(18.dp),
        large = RoundedCornerShape(24.dp)), content = content)
}
@Composable fun PortylusBrand() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Image(painterResource(R.drawable.portylus_symbol), contentDescription = null, modifier = Modifier.size(44.dp))
        Column {
            Text("Portylus", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text("gtavo.apps", fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary)
        }
    }
}
