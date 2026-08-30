package com.shadowvibe.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF8B5CF6),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF4C1D95),
    onPrimaryContainer = Color(0xFFEDE9FE),
    secondary = Color(0xFFA78BFA),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF312E81),
    onSecondaryContainer = Color(0xFFE0E7FF),
    tertiary = Color(0xFFF472B6),
    background = Color(0xFF09090F),
    onBackground = Color(0xFFFAFAFA),
    surface = Color(0xFF131318),
    onSurface = Color(0xFFFAFAFA),
    surfaceVariant = Color(0xFF1E1E28),
    onSurfaceVariant = Color(0xFFA1A1AA),
    surfaceContainer = Color(0xFF1C1C24),
    outline = Color(0xFF2E2E38),
    outlineVariant = Color(0xFF20202A),
    error = Color(0xFFEF4444),
    onError = Color.White
)

@Composable
fun ShadowVibeTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Enable edge-to-edge so content extends under system bars
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            window.statusBarColor = DarkColorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = DarkColorScheme,
        shapes = Shapes,
        typography = Typography,
        content = content
    )
}
