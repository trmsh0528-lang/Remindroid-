package com.example.myreminders

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

data class AccentOption(val name: String, val color: Color)

/** צבעי ההדגשה לבחירה. אפשר להוסיף כאן עוד צבעים. */
val AccentOptions = listOf(
    AccentOption("ירוק", Color(0xFF00E676)),
    AccentOption("כתום", Color(0xFFFF9100)),
    AccentOption("כחול", Color(0xFF40C4FF)),
    AccentOption("סגול", Color(0xFFB388FF)),
    AccentOption("ורוד", Color(0xFFFF4081)),
)

@Composable
fun AppTheme(accent: Color, content: @Composable () -> Unit) {
    val colors = darkColorScheme(
        primary = accent,
        onPrimary = Color.Black,
        secondary = accent,
        onSecondary = Color.Black,
        secondaryContainer = accent,
        onSecondaryContainer = Color.Black,
        background = Color.Black,
        onBackground = Color.White,
        surface = Color(0xFF0D0D0D),
        onSurface = Color.White,
        surfaceVariant = Color(0xFF1A1A1A),
        onSurfaceVariant = Color(0xFFBDBDBD),
        surfaceContainerHigh = Color(0xFF1A1A1A),
        outline = Color(0xFF616161),
    )
    MaterialTheme(colorScheme = colors, content = content)
}
