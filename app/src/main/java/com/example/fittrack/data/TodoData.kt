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
    val calories: Double, // ✅ JSON에 5.5, 6.5 등이 있어서 Double로
    val difficulty: String,
    val description: String
)

// 2) Today List Entity (Room)
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

    // ✅ 사용자가 선택한 값
    val sets: Int? = null,
    val repsPerSet: Int? = null,   // ✅ 추가 (세트당 횟수)
    val duration: Int? = null,     // 유산소/유연성은 분 단위

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

    @Query("DELETE FROM today_exercises WHERE dateKey != :todayKey AND isCompleted = 0")
    suspend fun deleteNotToday(todayKey: String)

    @Query("""UPDATE today_exercises SET sets = :sets, repsPerSet = :repsPerSet, duration = :duration, calories = :calories WHERE rowId = :rowId""")
    suspend fun updateAmounts(rowId: Long, sets: Int?, repsPerSet: Int?, duration: Int?, calories: Int)
}

// 4) Database
@Database(
    entities = [TodayExerciseEntity::class],
    version = 2, // ✅ schema 변경 -> version 올림
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
                )
                    // ✅ 개발 단계에서 마이그레이션 귀찮으면 이게 가장 안전
                    // (기존 DB 날리고 새로 생성)
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

// 5) Repository (아래에서 addToToday 변경)
class TodoRepository(
    private val context: Context,
    private val dao: TodayExerciseDao
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun todayKey(): String = LocalDate.now().toString()

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

    suspend fun addToToday(
        ex: Exercise,
        dateKey: String,
        sets: Int? = null,
        repsPerSet: Int? = null,
        duration: Int? = null,
        calories: Int
    ) {
        dao.insert(
            TodayExerciseEntity(
                dateKey = dateKey,
                exerciseId = ex.id,
                name = ex.name,
                category = ex.category,
                sets = sets,
                repsPerSet = repsPerSet,
                duration = duration,
                calories = calories,
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

    suspend fun updateTodayAmounts(
        rowId: Long,
        sets: Int?,
        repsPerSet: Int?,
        duration: Int?,
        calories: Int
    ) {
        dao.updateAmounts(rowId, sets, repsPerSet, duration, calories)
    }
}