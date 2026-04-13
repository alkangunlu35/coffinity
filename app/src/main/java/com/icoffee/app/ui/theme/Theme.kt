package com.icoffee.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CoffeeLightColorScheme = lightColorScheme(
    primary = CoffeeColorTokens.accentPrimary,
    onPrimary = CoffeeColorTokens.textOnAccent,
    secondary = CoffeeColorTokens.accentSecondary,
    onSecondary = CoffeeColorTokens.textPrimary,
    tertiary = CoffeeColorTokens.surfaceSoft,
    background = CoffeeColorTokens.background,
    onBackground = CoffeeColorTokens.textPrimary,
    surface = CoffeeColorTokens.surface,
    onSurface = CoffeeColorTokens.textPrimary,
    surfaceVariant = CoffeeColorTokens.surfaceSoft,
    onSurfaceVariant = CoffeeColorTokens.textSecondary,
    outline = CoffeeColorTokens.borderSubtle
)

private val CoffeeDarkColorScheme = darkColorScheme(
    primary = meetCaramel,
    onPrimary = meetBgDeep,
    secondary = meetAmber,
    onSecondary = meetBgDeep,
    tertiary = Color(0xFF7A4A32),
    background = meetBgDeep,
    onBackground = meetCream,
    surface = meetSurface,
    onSurface = meetCream,
    surfaceVariant = meetSurfaceElevated,
    onSurfaceVariant = meetMutedTan,
    outline = meetBorderWarm
)

object ColorTokens {
    val surfaceVariant = CoffeeColorTokens.surfaceSoft
    val textPrimary = CoffeeColorTokens.textPrimary
    val textSecondary = CoffeeColorTokens.textSecondary
    val textMuted = CoffeeColorTokens.textMuted
    val accentPrimary = CoffeeColorTokens.accentPrimary
    val accentPrimaryPressed = CoffeeColorTokens.accentPrimaryPressed
    val accentSecondary = CoffeeColorTokens.accentSecondary
    val borderSubtle = CoffeeColorTokens.borderSubtle
    val borderStrong = CoffeeColorTokens.borderStrong
    val surface = CoffeeColorTokens.surface
    val surfaceSoft = CoffeeColorTokens.surfaceSoft
    val surfaceElevated = CoffeeColorTokens.surfaceElevated
    val success = CoffeeColorTokens.success
    val warning = CoffeeColorTokens.warning
    val error = CoffeeColorTokens.error
}

@Composable
fun ICoffeeTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) CoffeeDarkColorScheme else CoffeeLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = CoffeeTypography,
        shapes = CoffeeShapes,
        content = content
    )
}
