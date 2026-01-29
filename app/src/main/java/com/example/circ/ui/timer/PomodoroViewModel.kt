package com.example.circ.ui.timer

import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.circ.data.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.*

data class RankInfo(val name: String, val color: Color, val minPoints: Long)
data class DailyPointLog(val dayName: String, val date: String, val minutes: Long, val earnedPoints: Long, val netPoints: Long, val multiplier: String)
data class HourlyStat(val hour: Int, val minutes: Long)

class PomodoroViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val dao = db.sessionDao()
    private val gson = Gson()

    // --- SERVICE BINDING ---
    private var timerService: TimerService? = null
    var isServiceBound by mutableStateOf(false)

    val timeLeft = MutableStateFlow(TimerConstants.DEFAULT_WORK_MINS * 60L)
    val isRunning = MutableStateFlow(false)
    val isWorkMode = MutableStateFlow(true)
    private var serviceCollectionJob: Job? = null

    fun bindToService(service: TimerService) {
        timerService = service
        isServiceBound = true
        serviceCollectionJob?.cancel()
        serviceCollectionJob = viewModelScope.launch {
            // Collect state from the service to keep UI in sync
            launch { service.timeLeft.collect { timeLeft.value = it } }
            launch { service.isRunning.collect { isRunning.value = it } }
            launch { service.isWorkMode.collect { isWorkMode.value = it } }
        }
    }

    fun unbindService() {
        timerService = null
        isServiceBound = false
        serviceCollectionJob?.cancel()
    }

    fun toggleTimer() = timerService?.toggleTimer()
    fun resetTimer() = timerService?.resetTimer()
    fun switchMode(isWork: Boolean) = timerService?.switchMode(isWork)

    // --- HISTORY LOGIC ---
    var selectedDate: LocalDate by mutableStateOf(LocalDate.now())
    var isHistoryLoading by mutableStateOf(false)

    @OptIn(ExperimentalCoroutinesApi::class)
    val historyForSelectedDate: StateFlow<List<CompletedSession>> = snapshotFlow { selectedDate }
        .onEach { isHistoryLoading = true }
        .flatMapLatest { date -> dao.getSessionsForDate(date.toString()) }
        .onEach { isHistoryLoading = false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val hourlyStatsForSelectedDate: StateFlow<List<HourlyStat>> = historyForSelectedDate
        .map { sessions ->
            val stats = MutableList(24) { HourlyStat(it, 0L) }
            sessions.forEach { session ->
                try {
                    val hour = session.timestamp.split(":")[0].toInt()
                    if (hour in 0..23) {
                        stats[hour] = stats[hour].copy(minutes = stats[hour].minutes + session.durationMinutes)
                    }
                } catch (_: Exception) {}
            }
            stats.toList()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), List(24) { HourlyStat(it, 0L) })

    fun updateSelectedDate(date: LocalDate) { selectedDate = date }

    // --- DASHBOARD LOGIC ---

    @OptIn(ExperimentalCoroutinesApi::class)
    val weeklyPointLog: StateFlow<List<DailyPointLog>> = snapshotFlow {
        LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    }.flatMapLatest { monday ->
        dao.getSessionsFromDate(monday.toString()).map { sessions ->
            val daysPassedInWeek = mutableListOf<LocalDate>()
            var curr = monday
            val today = LocalDate.now()
            while (!curr.isAfter(today)) {
                daysPassedInWeek.add(curr)
                curr = curr.plusDays(1)
            }

            daysPassedInWeek.reversed().map { date ->
                val daySessions = sessions.filter { it.date == date.toString() }
                val totalMins = daySessions.sumOf { it.durationMinutes }
                val goal = TimerConstants.DEFAULT_DAILY_GOAL_MINS

                // Point calculation logic
                val net = if (totalMins >= goal) {
                    totalMins // Goal met: get full points
                } else if (date == today) {
                    totalMins // Today: no penalty yet
                } else {
                    totalMins - (goal / 2) // Past day deficit: penalty applied
                }

                DailyPointLog(
                    dayName = if (date == today) "TODAY" else date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).uppercase(),
                    date = date.toString(),
                    minutes = totalMins,
                    earnedPoints = totalMins,
                    netPoints = net,
                    multiplier = if (totalMins >= goal) "GOAL MET" else if (date == today) "IN PROGRESS" else "DEFICIT"
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val weeklyCredits: StateFlow<Long> = weeklyPointLog.map { logs -> logs.sumOf { it.netPoints } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val currentRank: StateFlow<RankInfo> = weeklyCredits.map { credits ->
        val threshold = TimerConstants.RANKS.firstOrNull { credits >= it.minPoints } ?: TimerConstants.RANKS.last()
        RankInfo(threshold.name, threshold.color, threshold.minPoints)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RankInfo("INITIATE", Color.Gray, 0))

    val weeklyShieldBank: StateFlow<Long> = weeklyPointLog.map { logs -> logs.sumOf { it.minutes } / 6 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val graphDataForSelectedDate: StateFlow<List<Float>> = hourlyStatsForSelectedDate.map { stats ->
        stats.map { it.minutes.toFloat() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), List(24) { 0f })

    // --- DATA MANAGEMENT (IMPORT/EXPORT) ---

    fun exportData(cr: ContentResolver, uri: Uri, cb: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val sessions = dao.getAllSessions().first()
                val json = gson.toJson(sessions)
                cr.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                withContext(Dispatchers.Main) { cb("Export Successful") }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { cb("Export Failed: ${e.message}") }
            }
        }
    }

    fun importData(cr: ContentResolver, uri: Uri, cb: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val inputStream = cr.openInputStream(uri) ?: throw Exception("File not found")
                val reader = BufferedReader(InputStreamReader(inputStream))
                val type = object : TypeToken<List<CompletedSession>>() {}.type
                val sessions: List<CompletedSession> = gson.fromJson(reader, type)

                if (sessions.isNotEmpty()) {
                    // Use the list-based insert to trigger Room's OnConflictStrategy.REPLACE
                    dao.insertSessions(sessions)
                    withContext(Dispatchers.Main) { cb("Imported ${sessions.size} sessions") }
                } else {
                    withContext(Dispatchers.Main) { cb("Import Failed: File is empty") }
                }
            } catch (e: Exception) {
                // Return a user-friendly error
                withContext(Dispatchers.Main) { cb("Import Failed: Invalid File Structure") }
            }
        }
    }
}