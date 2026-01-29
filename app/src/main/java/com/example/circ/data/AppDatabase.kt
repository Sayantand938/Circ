package com.example.circ.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Entity(
    tableName = "sessions",
    // This index ensures that we don't have two entries for the exact same minute.
    // When you import, if the date and timestamp match, it will REPLACE the old one.
    indices = [Index(value = ["date", "timestamp"], unique = true)]
)
data class CompletedSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String = LocalDate.now().toString(),
    val timestamp: String = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")),
    val durationMinutes: Long
)

@Dao
interface SessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: CompletedSession)

    // Helper to insert a whole list at once (better for performance during import)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessions(sessions: List<CompletedSession>)

    @Query("SELECT * FROM sessions WHERE date = :date")
    fun getSessionsForDate(date: String): Flow<List<CompletedSession>>

    @Query("SELECT * FROM sessions WHERE date >= :startDate")
    fun getSessionsFromDate(startDate: String): Flow<List<CompletedSession>>

    @Query("SELECT * FROM sessions")
    fun getAllSessions(): Flow<List<CompletedSession>>
}

@Database(entities = [CompletedSession::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "circ_database"
                )
                    // If you already have the app installed, this allows the DB to update
                    // the schema with the new Unique Index without crashing.
                    .fallbackToDestructiveMigration(false)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}