package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CyberColorScheme = darkColorScheme(
  primary = CyberCyan,
  secondary = CyberPink,
  tertiary = CyberPurple,
  background = CyberDark,
  surface = CyberSurface,
  surfaceVariant = CyberCard,
  onPrimary = Color(0xFF02040A),
  onSecondary = Color.White,
  onTertiary = Color.White,
  onBackground = Color(0xFFECEFF1),
  onSurface = Color(0xFFF5F7FA),
  onSurfaceVariant = Color(0xFFECEFF1)
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force dark theme for cyberpunk vibe
  dynamicColor: Boolean = false, // Disable dynamic light/dark so it stays sleek cyber dark
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = CyberColorScheme,
    typography = Typography,
    content = content
  )
}
