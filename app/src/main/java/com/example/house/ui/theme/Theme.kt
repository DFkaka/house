
package com.example.house.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = Blue700,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = Blue50,
    secondary = Blue800,
    background = androidx.compose.ui.graphics.Color.White,
    surface = androidx.compose.ui.graphics.Color.White,
    surfaceVariant = Gray100,
    onBackground = Gray800,
    onSurface = Gray800,
    outline = Gray200,
)

@Composable
fun HouseUtilityTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography(),
        content = content
    )
}
