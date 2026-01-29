package com.example.circ.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.circ.data.TimerConstants
import com.example.circ.ui.theme.*
import com.example.circ.ui.timer.PomodoroViewModel

@Composable
fun DashboardScreen(viewModel: PomodoroViewModel) {
    val credits by viewModel.weeklyCredits.collectAsState()
    val rank by viewModel.currentRank.collectAsState()
    val dailyLogs by viewModel.weeklyPointLog.collectAsState()
    val shieldBank by viewModel.weeklyShieldBank.collectAsState()

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(horizontal = 24.dp)) {
        Spacer(Modifier.height(32.dp))
        Text("WEEKLY STANDING", style = MaterialTheme.typography.labelLarge, color = TextLowEmphasis, fontSize = 10.sp)
        Spacer(Modifier.height(24.dp))

        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)).padding(vertical = 24.dp, horizontal = 8.dp)) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp)) {
                    Box(Modifier.size(12.dp).clip(RoundedCornerShape(2.dp)).background(rank.color))
                    Spacer(Modifier.width(10.dp))
                    Text(text = rank.name, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelLarge, fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                }
                Spacer(Modifier.height(32.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    GridItem(label = "CREDITS", value = if (credits >= 0) "+$credits" else credits.toString(), valueColor = if (credits >= 0) MaterialTheme.colorScheme.primary else RankOutcast, modifier = Modifier.weight(1f))
                    VerticalDivider()
                    GridItem(label = "GOAL", value = "${TimerConstants.DEFAULT_DAILY_GOAL_MINS}M", valueColor = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                    VerticalDivider()
                    GridItem(label = "SHIELD", value = "${shieldBank}M", valueColor = RankGuardian, modifier = Modifier.weight(1f))
                }
            }
        }

        Spacer(Modifier.height(32.dp))
        Text("DAILY BREAKDOWN", color = TextLowEmphasis, style = MaterialTheme.typography.labelLarge, fontSize = 10.sp)
        Spacer(Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
            items(dailyLogs) { log -> DailyLogItem(log) }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
        Spacer(Modifier.height(16.dp))
        Text("Shields block point loss from daily deficits. Earned at 1m per 6m focused.", color = TextDisabled, style = MaterialTheme.typography.labelLarge, fontSize = 9.sp, lineHeight = 14.sp, modifier = Modifier.padding(bottom = 32.dp))
    }
}

@Composable
fun GridItem(label: String, value: String, valueColor: Color, modifier: Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, color = TextLowEmphasis, fontSize = 8.sp, fontFamily = JetBrainsMono)
        Spacer(Modifier.height(4.dp))
        Text(text = value, color = valueColor, fontSize = 24.sp, fontFamily = JetBrainsMono, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun VerticalDivider() { Box(Modifier.height(30.dp).width(1.dp).background(MaterialTheme.colorScheme.outline)) }

@Composable
fun DailyLogItem(log: com.example.circ.ui.timer.DailyPointLog) {
    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface).padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text(text = log.dayName, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = JetBrainsMono)
            Text(text = "${log.minutes}M FOCUSED | ${log.multiplier}", color = TextLowEmphasis, fontSize = 10.sp, fontFamily = JetBrainsMono)
        }
        val isNegative = log.netPoints < 0
        Text(text = if (isNegative) "${log.netPoints}" else "+${log.netPoints}", color = if (isNegative) RankOutcast else MaterialTheme.colorScheme.primary, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = JetBrainsMono)
    }
}