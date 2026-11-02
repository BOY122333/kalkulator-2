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
    primary = CyanNeon,
    onPrimary = TechNavy900,
    primaryContainer = CyanPrimary,
    onPrimaryContainer = Color.White,
    secondary = PurpleAccent,
    onSecondary = TechNavy900,
    secondaryContainer = TechNavy700,
    onSecondaryContainer = Color.White,
    tertiary = EmeraldSuccess,
    onTertiary = TechNavy900,
    background = TechNavy900,
    onBackground = Color(0xFFF1F5F9),
    surface = TechNavy800,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = TechNavy700,
    onSurfaceVariant = SlateTextSecondary,
    outline = SlateBorder,
    error = RoseError,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = CyanPrimary,
    onPrimary = Color.White,
    primaryContainer = CyanLight,
    onPrimaryContainer = TechNavy900,
    secondary = PurpleAccent,
    onSecondary = Color.White,
    tertiary = EmeraldSuccess,
    onTertiary = Color.White,
    background = LightSurfaceBg,
    onBackground = TechNavy900,
    surface = LightCardBg,
    onSurface = TechNavy900,
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1),
    error = RoseError,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
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
