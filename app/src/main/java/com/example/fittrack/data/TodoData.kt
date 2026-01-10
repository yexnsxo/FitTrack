package com.example.fittrack.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDate

// 1) Catalog Model (assets JSON)
@Serializable
data class Exercise(
    val id: String,
    val name: String,
    val category: String,
    val sets: Int? = null,
    val duration: Int? = null,
    val calories: Int,
    val difficulty: String,
    val description: String
)

// 2) Today List Entity (Room) + 중복 추가 허용
@Entity(
    tableName = "today_exercises",
    indices = [Index(value = ["dateKey"])]
)
data class TodayExerciseEntity(
    @PrimaryKey(autoGenerate = true) val rowId: Long = 0L,
    val dateKey: String, // "yyyy-MM-dd"
    val exerciseId: String,
    val name: String,
    val category: String,
    val sets: Int? = null,
    val duration: Int? = null,
    val calories: Int,
    val difficulty: String,
    val description: String,
    val isCompleted: Boolean = false
)

// 3) DAO
@Dao
interface TodayExerciseDao {

    @Query("SELECT * FROM today_exercises WHERE dateKey = :dateKey ORDER BY rowId DESC")
    suspend fun getTodayOnce(dateKey: String): List<TodayExerciseEntity>

    @Query("SELECT * FROM today_exercises WHERE dateKey = :dateKey ORDER BY rowId DESC")
    fun observeToday(dateKey: String): kotlinx.coroutines.flow.Flow<List<TodayExerciseEntity>>

    @Insert
    suspend fun insert(item: TodayExerciseEntity)

    @Query("UPDATE today_exercises SET isCompleted = :completed WHERE rowId = :rowId")
    suspend fun updateCompleted(rowId: Long, completed: Boolean)

    @Query("DELETE FROM today_exercises WHERE rowId = :rowId")
    suspend fun deleteById(rowId: Long)

    @Query("DELETE FROM today_exercises WHERE dateKey != :todayKey")
    suspend fun deleteNotToday(todayKey: String)
}

// 4) Database
@Database(
    entities = [TodayExerciseEntity::class],
    version = 1,
    exportSchema = false
)
abstract class FitTrackDatabase : RoomDatabase() {
    abstract fun todayExerciseDao(): TodayExerciseDao

    companion object {
        @Volatile private var INSTANCE: FitTrackDatabase? = null

        fun getInstance(context: Context): FitTrackDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    FitTrackDatabase::class.java,
                    "fittrack.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}

// 5) Repository
class TodoRepository(
    private val context: Context,
    private val dao: TodayExerciseDao
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun todayKey(): String = LocalDate.now().toString() // "yyyy-MM-dd"

    suspend fun loadCatalogFromAssets(): List<Exercise> = withContext(Dispatchers.IO) {
        val text = context.assets.open("exercise_database.json")
            .bufferedReader()
            .use { it.readText() }

        json.decodeFromString<List<Exercise>>(text)
    }

    fun observeToday(dateKey: String) = dao.observeToday(dateKey)

    suspend fun cleanupNotToday(todayKey: String) {
        dao.deleteNotToday(todayKey)
    }

    suspend fun addToToday(ex: Exercise, dateKey: String) {
        dao.insert(
            TodayExerciseEntity(
                dateKey = dateKey,
                exerciseId = ex.id,
                name = ex.name,
                category = ex.category,
                sets = ex.sets,
                duration = ex.duration,
                calories = ex.calories,
                difficulty = ex.difficulty,
                description = ex.description
            )
        )
    }

    suspend fun setCompleted(rowId: Long, completed: Boolean) {
        dao.updateCompleted(rowId, completed)
    }

    suspend fun deleteRow(rowId: Long) {
        dao.deleteById(rowId)
    }
}