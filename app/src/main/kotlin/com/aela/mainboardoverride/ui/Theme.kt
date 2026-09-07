package com.aela.mainboardoverride.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Void = Color(0xFF07110F)
val Panel = Color(0xFF10231D)
val Terminal = Color(0xFFB6FF2E)
val Cyan = Color(0xFF39E7E0)
val Warning = Color(0xFFFFD43B)
val Danger = Color(0xFFFF4155)
val Muted = Color(0xFF76968C)

private val Scheme = darkColorScheme(
    primary = Terminal,
    secondary = Cyan,
    tertiary = Warning,
    error = Danger,
    background = Void,
    surface = Panel,
    onPrimary = Void,
    onBackground = Color(0xFFE7FFF2),
    onSurface = Color(0xFFE7FFF2),
)

@Composable
fun MainboardTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, typography = MaterialTheme.typography, content = content)
}
