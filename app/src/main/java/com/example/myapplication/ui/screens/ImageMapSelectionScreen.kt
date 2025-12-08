package com.example.myapplication.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.myapplication.R

@Composable
fun ImageMapSelectionScreen(
    correctRegion: String,
    onResult: (Boolean) -> Unit
) {
    var imageSize by remember { mutableStateOf(IntSize.Zero) }

    val regionNames = listOf(
        listOf("book", "spoon", "goat"),
        listOf("candle", "flag", "camel"),
        listOf("sickle", "giraffe", "drum"),
        listOf("umbrella", "pig", "crocodile")
    )

    Image(
        painter = painterResource(id = R.drawable.full_grid),
        contentDescription = "ACE Grid",
        modifier = Modifier
            .fillMaxWidth(0.95f)     // Bigger, nearly full width
            .aspectRatio(1f)         // Perfect square
            .padding(top = 8.dp)
            .onGloballyPositioned { coords ->
                imageSize = coords.size
            }
            .pointerInput(Unit) {
                detectTapGestures { tapOffset ->

                    val tapped = detectRegion(tapOffset, imageSize, regionNames)
                    val isCorrect = tapped == correctRegion

                    // DO NOT auto-jump
                    onResult(isCorrect)
                }
            }
    )
}


fun detectRegion(
    tap: Offset,
    imageSize: IntSize,
    grid: List<List<String>>
): String? {
    if (imageSize.width == 0 || imageSize.height == 0) return null

    val colWidth = imageSize.width / 3
    val rowHeight = imageSize.height / 4

    val col = (tap.x / colWidth).toInt().coerceIn(0, 2)
    val row = (tap.y / rowHeight).toInt().coerceIn(0, 3)

    return grid[row][col]
}
