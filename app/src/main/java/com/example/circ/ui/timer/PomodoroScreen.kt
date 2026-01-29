package com.example.circ.ui.timer

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.circ.data.TimerConstants
import com.example.circ.ui.components.ModeButton
import com.example.circ.ui.theme.TextHighEmphasis
import com.example.circ.ui.theme.TextLowEmphasis
import com.example.circ.ui.theme.TextMediumEmphasis
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PomodoroScreen(viewModel: PomodoroViewModel) {
    val haptic = LocalHapticFeedback.current
    val timeLeft by viewModel.timeLeft.collectAsState()
    val isRunning by viewModel.isRunning.collectAsState()
    val isWorkMode by viewModel.isWorkMode.collectAsState()

    val totalSecondsForProgress = (if (isWorkMode) TimerConstants.DEFAULT_WORK_MINS else TimerConstants.DEFAULT_BREAK_MINS) * 60L
    val isFinished = timeLeft == 0L

    val ringOutlineColor = MaterialTheme.colorScheme.outline
    val ringPrimaryColor = MaterialTheme.colorScheme.primary
    val backgroundColor = MaterialTheme.colorScheme.background
    val surfaceColor = MaterialTheme.colorScheme.surface

    Column(Modifier.fillMaxSize().background(backgroundColor), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            val timeText = String.format(Locale.getDefault(), "%02d:%02d", timeLeft / 60, timeLeft % 60)
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(320.dp).clip(CircleShape).combinedClickable(
                interactionSource = remember { MutableInteractionSource() }, indication = null,
                onClick = {
                    if (!isFinished) {
                        viewModel.toggleTimer()
                        // Using LongPress for a strong, tactile "thump" on interaction
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                },
                onLongClick = {
                    viewModel.resetTimer()
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            )
            ) {
                val progress by animateFloatAsState(targetValue = if (totalSecondsForProgress > 0) timeLeft.toFloat() / totalSecondsForProgress else 0f, label = "RingProgress")
                Canvas(modifier = Modifier.size(260.dp)) {
                    drawCircle(color = ringOutlineColor, style = Stroke(width = 8.dp.toPx()))
                    drawArc(color = ringPrimaryColor, startAngle = -90f, sweepAngle = 360f * progress, useCenter = false, style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = timeText, style = MaterialTheme.typography.displayLarge, color = if (isRunning || isFinished) TextHighEmphasis else TextMediumEmphasis)
                    val statusText = when {
                        isFinished -> "FINISHED"
                        !isRunning && timeLeft >= totalSecondsForProgress -> "TAP TO START"
                        !isRunning -> "PAUSED"
                        isWorkMode -> "FOCUSING"
                        else -> "RESTING"
                    }
                    Text(text = statusText, style = MaterialTheme.typography.labelLarge, fontSize = 10.sp, color = if (isFinished) ringPrimaryColor else TextLowEmphasis)
                }
            }
        }
        Row(modifier = Modifier.padding(bottom = 48.dp).clip(RoundedCornerShape(30.dp)).background(surfaceColor).padding(6.dp)) {
            ModeButton("WORK", active = isWorkMode) {
                if (!isRunning && !isWorkMode) {
                    viewModel.switchMode(true)
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            }
            ModeButton("BREAK", active = !isWorkMode) {
                if (!isRunning && isWorkMode) {
                    viewModel.switchMode(false)
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            }
        }
    }
}