package com.example.myapplication.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.theme.MintInactive

@Composable
fun AnimatedProgressBar(progress: Float, modifier: Modifier = Modifier) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = androidx.compose.animation.core.spring(stiffness = 50f)
    )

    // Custom height and styling
    Box(modifier = modifier.fillMaxWidth()) {
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp) // Slightly thicker for that "juicy" look
                .clip(RoundedCornerShape(50)), // Fully rounded pills
            color = MaterialTheme.colorScheme.primary, // Active Green (#51CD89)
            trackColor = MintInactive // Inactive Green (#9EE3BE)
        )
    }
}
