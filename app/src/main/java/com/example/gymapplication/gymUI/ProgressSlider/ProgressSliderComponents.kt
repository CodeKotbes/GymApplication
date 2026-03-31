package com.example.gymapplication.gymUI.ProgressSlider

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class FractionalRectShape(private val fraction: Float) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        return Outline.Rectangle(
            Rect(
                left = size.width * fraction,
                top = 0f,
                right = size.width,
                bottom = size.height
            )
        )
    }
}

@Composable
fun InteractiveProgressSlider(
    beforeUri: String,
    afterUri: String,
    beforeDateMillis: Long,
    beforeValue: Float,
    afterDateMillis: Long,
    afterValue: Float,
    unit: String,
    modifier: Modifier = Modifier
) {
    var sliderPosition by remember { mutableFloatStateOf(0.5f) }
    var containerWidthPx by remember { mutableFloatStateOf(1f) }
    var containerHeightPx by remember { mutableFloatStateOf(1f) }
    val density = LocalDensity.current
    val handleSize = 55.dp
    val dateFormat = remember { SimpleDateFormat("dd. MMM yyyy", Locale.getDefault()) }
    val primaryColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .onSizeChanged { size ->
                containerWidthPx = size.width.toFloat().coerceAtLeast(1f)
                containerHeightPx = size.height.toFloat().coerceAtLeast(1f)
            }
    ) {
        val handleSizePx = with(density) { handleSize.toPx() }

        AsyncImage(
            model = beforeUri,
            contentDescription = "Vorher Foto",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        AsyncImage(
            model = afterUri,
            contentDescription = "Nachher Foto",
            modifier = Modifier
                .fillMaxSize()
                .clip(FractionalRectShape(sliderPosition)),
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(listOf(Color.Black.copy(0.6f), Color.Transparent))
                )
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.7f)))
                )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "START",
                fontWeight = FontWeight.Black,
                color = primaryColor,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                "AKTUELL",
                fontWeight = FontWeight.Black,
                color = primaryColor,
                style = MaterialTheme.typography.titleMedium
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Text(
                dateFormat.format(Date(beforeDateMillis)),
                color = Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                "${String.format(Locale.getDefault(), "%.1f", beforeValue)} $unit",
                color = Color.White,
                fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.titleLarge
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                dateFormat.format(Date(afterDateMillis)),
                color = Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                "${String.format(Locale.getDefault(), "%.1f", afterValue)} $unit",
                color = Color.White,
                fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.titleLarge
            )
        }

        val lineX = sliderPosition * containerWidthPx

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(4.dp)
                .offset { IntOffset(lineX.roundToInt() - 2, 0) }
                .background(Color.White)
        )

        Box(
            modifier = Modifier
                .size(handleSize)
                .offset {
                    IntOffset(
                        (lineX - handleSizePx / 2f).roundToInt(),
                        (containerHeightPx / 2f - handleSizePx / 2f).roundToInt()
                    )
                }
                .shadow(8.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.White)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val newX = (sliderPosition * containerWidthPx) + dragAmount.x
                        sliderPosition = (newX / containerWidthPx).coerceIn(0f, 1f)
                    }
                }
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.UnfoldMore,
                contentDescription = "Slider Griff",
                tint = primaryColor,
                modifier = Modifier.rotate(90f)
            )
        }
    }
}