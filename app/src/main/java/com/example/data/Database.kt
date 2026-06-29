package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "protein_logs")
data class ProteinLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val foodName: String,
    val proteinGrams: Float,
    val calories: Float,
    val carbsGrams: Float,
    val fatsGrams: Float,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface ProteinDao {
    @Query("SELECT * FROM protein_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<ProteinLog>>

    @Query("SELECT * FROM protein_logs WHERE timestamp >= :startTime ORDER BY timestamp DESC")
    fun getLogsSince(startTime: Long): Flow<List<ProteinLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ProteinLog)

    @Query("DELETE FROM protein_logs WHERE id = :id")
    suspend fun deleteLogById(id: Int)

    @Query("DELETE FROM protein_logs")
    suspend fun clearAll()
}

@Database(entities = [ProteinLog::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun proteinDao(): ProteinDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "protein_tracker_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class ProteinRepository(private val proteinDao: ProteinDao) {
    val allLogs: Flow<List<ProteinLog>> = proteinDao.getAllLogs()

    fun getLogsSince(startTime: Long): Flow<List<ProteinLog>> = proteinDao.getLogsSince(startTime)

    suspend fun insertLog(log: ProteinLog) {
        proteinDao.insertLog(log)
    }

    suspend fun deleteLogById(id: Int) {
        proteinDao.deleteLogById(id)
    }

    suspend fun clearAll() {
        proteinDao.clearAll()
    }
}
