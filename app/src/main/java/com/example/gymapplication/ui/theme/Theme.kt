package com.example.gymapplication.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val McFitYellow = Color(0xFFFFCC00)
val McFitAmber = Color(0xFFB38600)
val AppleDarkGrey = Color(0xFF1C1C1E)

@Composable
fun SmoothMcFitTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) {
        darkColorScheme(
            primary = McFitYellow,
            onPrimary = Color.Black,
            surface = AppleDarkGrey,
            background = Color.Black,
            onSurface = Color.White
        )
    } else {
        lightColorScheme(
            primary = McFitAmber,
            onPrimary = Color.White,
            surface = Color(0xFFF2F2F7),
            background = Color.White,
            onSurface = Color.Black
        )
    }

    MaterialTheme(
        colorScheme = colors,
        shapes = Shapes(
            medium = RoundedCornerShape(28.dp),
            large = RoundedCornerShape(32.dp)
        ),
        content = content
    )
}