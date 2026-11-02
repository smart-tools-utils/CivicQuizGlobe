package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = CivicNavyDarkPrimary,
    onPrimary = Color(0xFF0F172A),
    primaryContainer = CivicNavyDarkContainer,
    onPrimaryContainer = CivicNavyDarkOnContainer,
    secondary = CivicNavyDarkSecondary,
    onSecondary = Color(0xFF0F172A),
    tertiary = CivicGoldTertiary,
    background = CivicNavyDarkBackground,
    surface = CivicNavyDarkSurface,
    onBackground = Color(0xFFF8FAFC),
    onSurface = Color(0xFFF8FAFC)
)

private val LightColorScheme = lightColorScheme(
    primary = CivicNavyPrimary,
    onPrimary = Color.White,
    primaryContainer = CivicNavyLightContainer,
    onPrimaryContainer = CivicNavyLightOnContainer,
    secondary = CivicNavyLightSecondary,
    onSecondary = Color.White,
    tertiary = CivicGoldTertiary,
    background = Color(0xFFF8FAFC),
    surface = Color.White,
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Set false to ensure our distinct civic theme is showcased
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
