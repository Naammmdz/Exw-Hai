package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val ColorScheme =
  lightColorScheme(
    primary = Apricot,
    secondary = Sage,
    background = Cream,
    surface = Surface,
    onPrimary = Surface,
    onBackground = Cocoa,
    onSurface = Cocoa,
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  MaterialTheme(colorScheme = ColorScheme, typography = Typography, content = content)
}
