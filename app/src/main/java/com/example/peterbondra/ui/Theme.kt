package com.example.peterbondra.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.peterbondra.ThemeMode

private val LightColors = lightColorScheme(
    primary = Color(0xFF005BC2),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF576500),
    tertiary = Color(0xFF9D3D00),
    background = Color(0xFFF7FAFF),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE8EEF8),
    outline = Color(0xFF6F7888),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFAAC7FF),
    onPrimary = Color(0xFF002F67),
    secondary = Color(0xFFD1E964),
    tertiary = Color(0xFFFFB68A),
    background = Color(0xFF0E141F),
    surface = Color(0xFF121A26),
    surfaceVariant = Color(0xFF1D2736),
    outline = Color(0xFF8993A4),
)

@Composable
fun PeterBondraTheme(
    themeMode: ThemeMode,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content,
    )
}
