package com.example.circ.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.circ.ui.theme.*
import com.example.circ.ui.timer.PomodoroViewModel

@Composable
fun SettingsScreen(viewModel: PomodoroViewModel) {
    val context = LocalContext.current

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        uri?.let { viewModel.exportData(context.contentResolver, it) { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        } }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let { viewModel.importData(context.contentResolver, it) { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        } }
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp)) {
        Text("SETTINGS", style = MaterialTheme.typography.labelLarge, color = TextLowEmphasis)
        Spacer(Modifier.height(32.dp))

        Text("FIXED CONFIGURATION", color = TextLowEmphasis, fontSize = 10.sp, letterSpacing = 2.sp)
        Spacer(Modifier.height(16.dp))
        Text("FOCUS: 30M", color = TextHighEmphasis, fontSize = 14.sp, fontFamily = JetBrainsMono)
        Text("BREAK: 30M", color = TextHighEmphasis, fontSize = 14.sp, fontFamily = JetBrainsMono)
        Text("DAILY GOAL: 480M (8H)", color = TextHighEmphasis, fontSize = 14.sp, fontFamily = JetBrainsMono)

        Spacer(Modifier.weight(1f))

        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("DATA MANAGEMENT", color = TextLowEmphasis, style = MaterialTheme.typography.labelLarge, fontSize = 10.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DataBox("EXPORT") {
                    val unix = System.currentTimeMillis() / 1000
                    exportLauncher.launch("circ_backup_$unix.json")
                }
                DataBox("IMPORT") {
                    importLauncher.launch(arrayOf("application/json"))
                }
            }
        }
    }
}

@Composable
fun RowScope.DataBox(label: String, onClick: () -> Unit) {
    Box(Modifier.weight(1f).background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp)).clickable { onClick() }.padding(16.dp), contentAlignment = Alignment.Center) {
        Text(text = label, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = JetBrainsMono)
    }
}