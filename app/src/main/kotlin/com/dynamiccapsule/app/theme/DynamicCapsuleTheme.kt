package com.dynamiccapsule.app.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import com.dynamiccapsule.core.model.AppFontFamily
import com.dynamiccapsule.core.model.AppGeneralSettings
import com.dynamiccapsule.core.model.AppThemeMode

private val DarkCapsuleScheme = darkColorScheme(
    primary = Color(0xFF7C4DFF),
    secondary = Color(0xFF00E5FF),
    tertiary = Color(0xFF00E676),
    background = Color(0xFF090B10),
    surface = Color(0xFF131722)
)

private val LightCapsuleScheme = lightColorScheme(
    primary = Color(0xFF5E35B1),
    secondary = Color(0xFF0097A7),
    tertiary = Color(0xFF00897B),
    background = Color(0xFFF6F8FC),
    surface = Color(0xFFFFFFFF)
)

@Composable
fun DynamicCapsuleTheme(
    settings: AppGeneralSettings,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val darkTheme = when (settings.themeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }

    val colorScheme = when {
        settings.dynamicColorsEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkCapsuleScheme
        else -> LightCapsuleScheme
    }

    val selectedFont = when (settings.fontFamily) {
        AppFontFamily.SYSTEM_SANS -> FontFamily.SansSerif
        AppFontFamily.ROUNDED_GEOMETRIC -> FontFamily.Default
        AppFontFamily.TECH_MONOSPACE -> FontFamily.Monospace
        AppFontFamily.ELEGANT_SERIF -> FontFamily.Serif
    }

    val baseTypography = Typography()
    val customTypography = Typography(
        displayLarge = baseTypography.displayLarge.copy(fontFamily = selectedFont),
        headlineLarge = baseTypography.headlineLarge.copy(fontFamily = selectedFont),
        headlineMedium = baseTypography.headlineMedium.copy(fontFamily = selectedFont),
        titleLarge = baseTypography.titleLarge.copy(fontFamily = selectedFont),
        titleMedium = baseTypography.titleMedium.copy(fontFamily = selectedFont),
        titleSmall = baseTypography.titleSmall.copy(fontFamily = selectedFont),
        bodyLarge = baseTypography.bodyLarge.copy(fontFamily = selectedFont),
        bodyMedium = baseTypography.bodyMedium.copy(fontFamily = selectedFont),
        bodySmall = baseTypography.bodySmall.copy(fontFamily = selectedFont),
        labelLarge = baseTypography.labelLarge.copy(fontFamily = selectedFont),
        labelMedium = baseTypography.labelMedium.copy(fontFamily = selectedFont),
        labelSmall = baseTypography.labelSmall.copy(fontFamily = selectedFont)
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = customTypography,
        content = content
    )
}
