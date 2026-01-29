package com.example.circ

import android.Manifest
import android.app.AlarmManager
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.circ.data.TimerConstants
import com.example.circ.ui.components.AppDrawer
import com.example.circ.ui.components.Header
import com.example.circ.ui.screens.DashboardScreen
import com.example.circ.ui.screens.HistoryScreen
import com.example.circ.ui.screens.SettingsScreen
import com.example.circ.ui.theme.CIRCTheme
import com.example.circ.ui.timer.PomodoroScreen
import com.example.circ.ui.timer.PomodoroViewModel
import com.example.circ.ui.timer.TimerService
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: PomodoroViewModel by viewModels()

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as TimerService.TimerBinder
            viewModel.bindToService(binder.getService())
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            viewModel.unbindService()
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> checkExactAlarmPermission() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        checkNotificationPermission()

        Intent(this, TimerService::class.java).also { intent ->
            startService(intent)
            bindService(intent, serviceConnection, BIND_AUTO_CREATE)
        }

        setContent {
            CIRCTheme {
                val navController = rememberNavController()
                val drawerState = rememberDrawerState(DrawerValue.Closed)
                val scope = rememberCoroutineScope()

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    scrimColor = Color.Black.copy(alpha = 0.8f),
                    drawerContent = {
                        AppDrawer(navController) { scope.launch { drawerState.close() } }
                    }
                ) {
                    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
                        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                            Header(onMenuClick = { scope.launch { drawerState.open() } })

                            NavHost(
                                navController = navController,
                                startDestination = TimerConstants.ROUTE_TIMER,
                                modifier = Modifier.weight(1f).background(MaterialTheme.colorScheme.background),
                                enterTransition = { fadeIn(tween(400)) },
                                exitTransition = { fadeOut(tween(400)) }
                            ) {
                                composable(TimerConstants.ROUTE_TIMER) { PomodoroScreen(viewModel) }
                                composable(TimerConstants.ROUTE_DASHBOARD) { DashboardScreen(viewModel) }
                                composable(TimerConstants.ROUTE_HISTORY) { HistoryScreen(viewModel) }
                                composable(TimerConstants.ROUTE_SETTINGS) { SettingsScreen(viewModel) }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                checkExactAlarmPermission()
            }
        } else {
            checkExactAlarmPermission()
        }
    }

    private fun checkExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(AlarmManager::class.java)
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try { unbindService(serviceConnection) } catch (_: Exception) {}
    }
}