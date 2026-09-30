package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = DarkIndigoPrimary,
    onPrimary = DarkIndigoOnPrimary,
    primaryContainer = DarkIndigoContainer,
    onPrimaryContainer = DarkIndigoOnContainer,
    secondary = DarkSaffronSecondary,
    onSecondary = DarkSaffronOnSecondary,
    secondaryContainer = DarkSaffronContainer,
    onSecondaryContainer = DarkSaffronOnContainer,
    tertiary = DarkKathaTertiary,
    onTertiary = DarkKathaOnTertiary,
    tertiaryContainer = DarkKathaContainer,
    onTertiaryContainer = DarkKathaOnContainer,
    background = MidnightLoomBackground,
    surface = MidnightLoomSurface,
    surfaceVariant = MidnightSurfaceVariant,
    onBackground = IvoryOnDarkSurface,
    onSurface = IvoryOnDarkSurface,
    onSurfaceVariant = MutedIvoryOnDarkVariant
)

private val LightColorScheme = lightColorScheme(
    primary = IndigoLoomPrimary,
    onPrimary = IndigoLoomOnPrimary,
    primaryContainer = IndigoLoomPrimaryContainer,
    onPrimaryContainer = IndigoLoomOnPrimaryContainer,
    secondary = SaffronGoldSecondary,
    onSecondary = SaffronGoldOnSecondary,
    secondaryContainer = SaffronGoldContainer,
    onSecondaryContainer = SaffronGoldOnContainer,
    tertiary = KathaRaniTertiary,
    onTertiary = KathaRaniOnTertiary,
    tertiaryContainer = KathaRaniContainer,
    onTertiaryContainer = KathaRaniOnContainer,
    background = CottonIvoryBackground,
    surface = CottonIvorySurface,
    surfaceVariant = CottonSurfaceVariant,
    onBackground = InkSlateOnSurface,
    onSurface = InkSlateOnSurface,
    onSurfaceVariant = MutedSlateOnVariant
)

val ErpShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = ErpShapes,
        content = content
    )
}
