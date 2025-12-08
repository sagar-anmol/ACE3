package com.example.myapplication.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.myapplication.R
import com.example.myapplication.data.model.ActionStep
import com.example.myapplication.data.model.Question
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun ActionSequenceScreen(
    question: Question,
    onResult: (Int) -> Unit,
    onBack: () -> Unit
) {
    val steps = question.steps ?: emptyList()

    var currentStepIndex by remember { mutableStateOf(0) } // 0 = practice
    var score by remember { mutableStateOf(0) }

    // Canvas dimensions
    var canvasWidthPx by remember { mutableStateOf(0f) }
    var canvasHeightPx by remember { mutableStateOf(0f) }

    // Responsive sizes
    var pencilW by remember { mutableStateOf(0f) }
    var pencilH by remember { mutableStateOf(0f) }
    var paperW by remember { mutableStateOf(0f) }
    var paperH by remember { mutableStateOf(0f) }
    var boxH by remember { mutableStateOf(0f) }

    val density = LocalDensity.current

    // Object positions
    var pencilOffset by remember { mutableStateOf(Offset.Zero) }
    var paperOffset by remember { mutableStateOf(Offset.Zero) }
    var positionsInitialized by remember { mutableStateOf(false) }

    // Box top position
    var boxTop by remember { mutableStateOf(0f) }

    // Step logic trackers
    val tapSequence = remember { mutableStateListOf<String>() }
    var paperTouchedInStep3 by remember { mutableStateOf(false) }

    fun resetPositions() {
        if (canvasWidthPx > 0f && canvasHeightPx > 0f) {

            // Initial placement (beautiful layout)
            pencilOffset = Offset(canvasWidthPx * 0.15f, canvasHeightPx * 0.30f)
            paperOffset  = Offset(canvasWidthPx * 0.55f, canvasHeightPx * 0.25f)

            // Ensure inside bounds
            pencilOffset = Offset(
                pencilOffset.x.coerceIn(0f, canvasWidthPx - pencilW),
                pencilOffset.y.coerceIn(0f, canvasHeightPx - pencilH)
            )

            paperOffset = Offset(
                paperOffset.x.coerceIn(0f, canvasWidthPx - paperW),
                paperOffset.y.coerceIn(0f, canvasHeightPx - paperH)
            )
        }

        tapSequence.clear()
        paperTouchedInStep3 = false
    }

    fun overlapArea(r1: Rect, r2: Rect): Float {
        val left = max(r1.left, r2.left)
        val top = max(r1.top, r2.top)
        val right = min(r1.right, r2.right)
        val bottom = min(r1.bottom, r2.bottom)
        if (right <= left || bottom <= top) return 0f
        return (right - left) * (bottom - top)
    }

    fun evaluate(step: ActionStep): Boolean {
        val pencilRect = Rect(pencilOffset, androidx.compose.ui.geometry.Size(pencilW, pencilH))
        val paperRect = Rect(paperOffset, androidx.compose.ui.geometry.Size(paperW, paperH))
        val boxRect = Rect(Offset(0f, boxTop), androidx.compose.ui.geometry.Size(canvasWidthPx, boxH))

        return step.requiredActions.all { action ->
            when (action) {

                // PRACTICE
                "PICK_PENCIL" ->
                    currentStepIndex == 0 &&
                            tapSequence.size >= 1 &&
                            tapSequence[0] == "PENCIL"

                "PICK_PAPER" ->
                    currentStepIndex == 0 &&
                            tapSequence.size == 2 &&
                            tapSequence[1] == "PAPER"

                // STEP 1 — Paper must fully cover pencil
                "PLACE_PAPER_ON_PENCIL" -> {
                    paperRect.left <= pencilRect.left &&
                            paperRect.top <= pencilRect.top &&
                            paperRect.right >= pencilRect.right &&
                            paperRect.bottom >= pencilRect.bottom
                }

                // STEP 2 — Pencil inside box 90%, paper NOT inside
                "PICK_PENCIL_ONLY" -> {
                    val pencilArea = pencilW * pencilH
                    val inside = overlapArea(pencilRect, boxRect) >= 0.90f * pencilArea
                    val paperInside = overlapArea(paperRect, boxRect) > 0f
                    inside && !paperInside
                }

                // STEP 3 — tap paper first → then pass pencil
                "PICK_PENCIL_AFTER_TOUCH" -> {
                    val pencilArea = pencilW * pencilH
                    val inside = overlapArea(pencilRect, boxRect) >= 0.90f * pencilArea
                    paperTouchedInStep3 && inside
                }

                else -> false
            }
        }
    }

    val currentStep = steps[currentStepIndex]

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {

        Text("Instruction:", modifier = Modifier.padding(bottom = 8.dp))
        Text(currentStep.command, modifier = Modifier.padding(bottom = 12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(onClick = onBack) { Text("Back") }
            Button(onClick = { resetPositions() }) { Text("Reset objects") }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ===== CANVAS (no clipping!) =====
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 8.dp)
                .background(Color.White, RoundedCornerShape(20.dp))
                .border(1.dp, Color.LightGray, RoundedCornerShape(20.dp))
                .onGloballyPositioned { coords ->
                    canvasWidthPx = coords.size.width.toFloat()
                    canvasHeightPx = coords.size.height.toFloat()

                    // Slightly larger pencil & paper
                    pencilW = canvasWidthPx * 0.23f
                    pencilH = pencilW * 0.52f

                    paperW = canvasWidthPx * 0.36f
                    paperH = paperW * 1.20f

                    boxH = canvasHeightPx * 0.28f
                    boxTop = canvasHeightPx - boxH

                    if (!positionsInitialized) {
                        resetPositions()
                        positionsInitialized = true
                    }
                }
        ) {

            // --- PENCIL (draw first → behind paper) ---
            Image(
                painter = painterResource(id = R.drawable.pencil),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .offset { IntOffset(pencilOffset.x.roundToInt(), pencilOffset.y.roundToInt()) }
                    .width(with(density) { pencilW.toDp() })
                    .height(with(density) { pencilH.toDp() })
                    .pointerInput(Unit) {
                        detectTapGestures {
                            if (currentStepIndex == 0 && tapSequence.size < 2)
                                tapSequence.add("PENCIL")
                        }
                    }
                    .pointerInput(canvasWidthPx, canvasHeightPx) {
                        detectDragGestures { change, drag ->
                            change.consume()
                            val newX = (pencilOffset.x + drag.x)
                                .coerceIn(0f, canvasWidthPx - pencilW)
                            val newY = (pencilOffset.y + drag.y)
                                .coerceIn(0f, canvasHeightPx - pencilH)
                            pencilOffset = Offset(newX, newY)
                        }
                    }
            )

            // --- PAPER (draw after pencil → on top) ---
            Image(
                painter = painterResource(id = R.drawable.paper),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .offset { IntOffset(paperOffset.x.roundToInt(), paperOffset.y.roundToInt()) }
                    .width(with(density) { paperW.toDp() })
                    .height(with(density) { paperH.toDp() })
                    .pointerInput(Unit) {
                        detectTapGestures {
                            if (currentStepIndex == 0 && tapSequence.size < 2)
                                tapSequence.add("PAPER")

                            if (currentStepIndex == 2)
                                paperTouchedInStep3 = true
                        }
                    }
                    .pointerInput(canvasWidthPx, canvasHeightPx) {
                        detectDragGestures { change, drag ->
                            change.consume()
                            val newX = (paperOffset.x + drag.x)
                                .coerceIn(0f, canvasWidthPx - paperW)
                            val newY = (paperOffset.y + drag.y)
                                .coerceIn(0f, canvasHeightPx - paperH)
                            paperOffset = Offset(newX, newY)
                        }
                    }
            )

            // --- BOX (inside canvas, fully visible) ---
            Image(
                painter = painterResource(id = R.drawable.box),
                contentDescription = "Box",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(0.85f)
                    .height(with(density) { boxH.toDp() })
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val passed = evaluate(currentStep)

                if (!passed && currentStepIndex == 0) {
                    onResult(0)
                    return@Button
                }

                if (passed && currentStepIndex > 0) score += 1

                if (currentStepIndex >= steps.lastIndex) {
                    onResult(score)
                    return@Button
                }

                currentStepIndex++
                resetPositions()
            },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Done")
        }
    }
}
