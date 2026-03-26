package com.mimicease.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val MimicDarkColorScheme = darkColorScheme(
    primary                = DarkPrimary,
    onPrimary              = DarkOnPrimary,
    primaryContainer       = DarkPrimaryContainer,
    onPrimaryContainer     = DarkOnPrimaryContainer,
    secondary              = DarkSecondary,
    onSecondary            = DarkOnSecondary,
    secondaryContainer     = DarkSecondaryContainer,
    onSecondaryContainer   = DarkOnSecondaryContainer,
    background             = DarkBackground,
    surface                = DarkSurface,
    surfaceVariant         = DarkSurfaceVar,
    onBackground           = DarkOnBackground,
    onSurface              = DarkOnSurface,
    onSurfaceVariant       = DarkOnSurfaceVar,
    outline                = DarkOutline,
    outlineVariant         = DarkOutlineVar,
    error                  = DarkError,
    errorContainer         = DarkErrorContainer,
    onError                = DarkOnError,
    onErrorContainer       = DarkOnErrorContainer,
)

private val MimicLightColorScheme = lightColorScheme(
    primary                = LightPrimary,
    onPrimary              = LightOnPrimary,
    primaryContainer       = LightPrimaryContainer,
    onPrimaryContainer     = LightOnPrimaryContainer,
    secondary              = LightSecondary,
    onSecondary            = LightOnSecondary,
    secondaryContainer     = LightSecondaryContainer,
    onSecondaryContainer   = LightOnSecondaryContainer,
    background             = LightBackground,
    surface                = LightSurface,
    surfaceVariant         = LightSurfaceVar,
    onBackground           = LightOnBackground,
    onSurface              = LightOnSurface,
    onSurfaceVariant       = LightOnSurfaceVar,
    outline                = LightOutline,
    outlineVariant         = LightOutlineVar,
    error                  = LightError,
    errorContainer         = LightErrorContainer,
    onError                = LightOnError,
    onErrorContainer       = LightOnErrorContainer,
)

@Composable
fun MimicEaseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) MimicDarkColorScheme else MimicLightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
