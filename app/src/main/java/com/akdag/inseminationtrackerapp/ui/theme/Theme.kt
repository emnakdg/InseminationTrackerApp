package com.akdag.inseminationtrackerapp.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val AppColorScheme = darkColorScheme(
    primary = GreenPrimary,
    onPrimary = Bg0,
    primaryContainer = GreenDim,
    onPrimaryContainer = GreenLight,
    background = Bg0,
    onBackground = TextPrimary,
    surface = CardColor,
    onSurface = TextPrimary,
    surfaceVariant = Bg3,
    onSurfaceVariant = TextMid,
    outline = BorderColor,
    error = RedAccent,
    onError = TextPrimary,
    secondary = YellowAccent,
    tertiary = BlueAccent,
)

@Composable
fun InseminationTrackerTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Bg1.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }
    MaterialTheme(colorScheme = AppColorScheme, typography = Typography, content = content)
}
