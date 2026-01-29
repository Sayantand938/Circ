package com.example.circ.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.circ.data.TimerConstants
import com.example.circ.ui.theme.*
import com.example.circ.ui.timer.HourlyStat
import com.example.circ.ui.timer.PomodoroViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: PomodoroViewModel) {
    val selectedDate = viewModel.selectedDate
    val history by viewModel.historyForSelectedDate.collectAsState()
    val hourlyStats by viewModel.hourlyStatsForSelectedDate.collectAsState()
    val isLoading = viewModel.isHistoryLoading

    val totalMinutes = remember(history) { history.sumOf { it.durationMinutes } }

    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )

        MaterialTheme(colorScheme = darkColorScheme(primary = BrandPrimary, onSurface = TextHighEmphasis)) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                            viewModel.updateSelectedDate(date)
                        }
                        showDatePicker = false
                    }) { Text("SELECT", fontFamily = JetBrainsMono) }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) { Text("CANCEL", fontFamily = JetBrainsMono) }
                }
            ) { DatePicker(state = datePickerState) }
        }
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        CenteredDateHeader(selectedDate = selectedDate, onOpenPicker = { showDatePicker = true })

        Crossfade(targetState = isLoading, label = "HistoryFade") { loading ->
            if (loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    item {
                        Spacer(Modifier.height(24.dp))
                        MinimalStats(totalMinutes)
                        Spacer(Modifier.height(40.dp))

                        // UPDATED BUCKET SYSTEM (Increased height & No Percentage)
                        FocusBucketSystem(totalMinutes)

                        Spacer(Modifier.height(52.dp))

                        Text(
                            text = "HOURLY DISTRIBUTION",
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.labelLarge,
                            color = TextLowEmphasis,
                            fontFamily = JetBrainsMono,
                            fontSize = 10.sp,
                            letterSpacing = 2.sp
                        )
                        Spacer(Modifier.height(20.dp))
                    }

                    val activeHours = hourlyStats.filter { it.minutes > 0 }

                    if (activeHours.isEmpty()) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                                Text(text = "NO ACTIVITY RECORDED", color = TextDisabled, fontSize = 11.sp, fontFamily = JetBrainsMono)
                            }
                        }
                    } else {
                        items(activeHours) { stat ->
                            HourlyLogItem(stat, TimerConstants.DEFAULT_HOURLY_GOAL_MINS)
                        }
                    }
                    item { Spacer(Modifier.height(40.dp)) }
                }
            }
        }
    }
}

@Composable
fun FocusBucketSystem(totalMinutes: Long) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            text = "DAILY QUOTA (8H)",
            color = TextLowEmphasis,
            fontSize = 10.sp,
            fontFamily = JetBrainsMono,
            letterSpacing = 1.5.sp
        )
        Spacer(Modifier.height(20.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp), // Increased height for better visual impact
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(8) { index ->
                val bucketStartMinute = index * 60
                val bucketFill = ((totalMinutes - bucketStartMinute).coerceIn(0L, 60L) / 60f)
                val isCurrentlyFilling = totalMinutes in bucketStartMinute until (bucketStartMinute + 60)

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Bucket Container
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(4.dp))
                            .background(BrandOutline.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        // "Water" Level
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(bucketFill)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(BrandPrimary, BrandPrimary.copy(alpha = 0.7f))
                                    )
                                )
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    // Hour Label
                    Text(
                        text = "${index + 1}H",
                        color = if (isCurrentlyFilling) BrandPrimary else if (bucketFill >= 1f) TextHighEmphasis else TextDisabled,
                        fontSize = 9.sp,
                        fontFamily = JetBrainsMono,
                        fontWeight = if (isCurrentlyFilling) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
fun HourlyLogItem(stat: HourlyStat, goal: Long) {
    val progress = (stat.minutes.toFloat() / goal).coerceAtMost(1f)
    val isGoalMet = stat.minutes >= goal

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${stat.hour.toString().padStart(2, '0')}:00",
            modifier = Modifier.width(52.dp),
            color = if (isGoalMet) TextHighEmphasis else TextLowEmphasis,
            fontSize = 11.sp,
            fontFamily = JetBrainsMono,
            fontWeight = if (isGoalMet) FontWeight.Bold else FontWeight.Normal
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .height(4.dp)
                .padding(horizontal = 12.dp)
                .background(BrandOutline.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .background(if (isGoalMet) BrandPrimary else RankGuardian, RoundedCornerShape(2.dp))
            )
        }

        Text(
            text = "${stat.minutes}M",
            color = if (isGoalMet) BrandPrimary else TextHighEmphasis,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = JetBrainsMono,
            modifier = Modifier.width(35.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}

@Composable
fun CenteredDateHeader(selectedDate: LocalDate, onOpenPicker: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(modifier = Modifier.fillMaxWidth().padding(top = 32.dp, bottom = 16.dp), contentAlignment = Alignment.Center) {
        HorizontalDivider(modifier = Modifier.padding(horizontal = 40.dp), color = BrandOutline.copy(alpha = 0.2f), thickness = 1.dp)
        Surface(
            color = Color.Transparent,
            modifier = Modifier
                .wrapContentSize()
                .clickable(interactionSource = interactionSource, indication = null, onClick = onOpenPicker)
                .padding(horizontal = 12.dp)
        ) {
            Row(
                modifier = Modifier.background(BrandSurface, RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.DateRange, null, tint = BrandPrimary, modifier = Modifier.size(14.dp))
                Text(text = selectedDate.toString(), color = BrandPrimary, fontFamily = JetBrainsMono, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun MinimalStats(totalMinutes: Long) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = "FOCUS TIME", color = TextLowEmphasis, fontSize = 10.sp, fontFamily = JetBrainsMono, letterSpacing = 1.5.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = totalMinutes.toString(), color = TextHighEmphasis, fontSize = 64.sp, fontWeight = FontWeight.ExtraLight, fontFamily = JetBrainsMono)
            Text(text = "MIN", color = BrandPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp, top = 24.dp), fontFamily = JetBrainsMono)
        }
    }
}