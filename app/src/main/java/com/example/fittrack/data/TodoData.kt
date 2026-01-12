package com.example.fittrack.data

import android.content.Context
import android.net.Uri
import androidx.room.*
import com.example.fittrack.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

// 1) Catalog Model (assets JSON)
@Serializable
data class Exercise(
    val id: String,
    val name: String,
    val category: String,
    val sets: Int? = null,
    val duration: Int? = null,
    val repsPerSet: Int? = null,
    val calories: Double,
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

    // 사용자가 선택한 목표 값
    val sets: Int = 1,
    val repsPerSet: Int? = null,
    val duration: Int? = null, // 목표 시간(분)

    // 실제 수행 결과 필드
    val actualDurationSec: Int = 0, // 실제 운동 시간(초)
    val actualReps: Int = 0,        // 실제 수행 횟수

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

    @Query("SELECT DISTINCT dateKey FROM today_exercises")
    fun getAllExerciseDates(): kotlinx.coroutines.flow.Flow<List<String>>

    @Insert
    suspend fun insert(item: TodayExerciseEntity)

    @Query("UPDATE today_exercises SET isCompleted = :completed WHERE rowId = :rowId")
    suspend fun updateCompleted(rowId: Long, completed: Boolean)

    // ✅ 타이머 완료 후 실제 기록 업데이트용
    @Query("""
        UPDATE today_exercises 
        SET actualDurationSec = :actualSec, actualReps = :actualReps, calories = :calories, isCompleted = 1 
        WHERE rowId = :rowId
    """)
    suspend fun completeExerciseRecord(rowId: Long, actualSec: Int, actualReps: Int, calories: Int)

    // ✅ 체크 해제 시 기록 초기화용 (추가)
    @Query("""
        UPDATE today_exercises 
        SET actualDurationSec = 0, actualReps = 0, isCompleted = 0, calories = :calories 
        WHERE rowId = :rowId
    """)
    suspend fun resetExerciseRecord(rowId: Long, calories: Int)

    @Query("DELETE FROM today_exercises WHERE rowId = :rowId")
    suspend fun deleteById(rowId: Long)

    @Query("DELETE FROM today_exercises WHERE dateKey != :todayKey AND isCompleted = 0")
    suspend fun deleteNotToday(todayKey: String)

    @Query("UPDATE today_exercises SET sets = :sets, repsPerSet = :repsPerSet, duration = :duration, calories = :calories WHERE rowId = :rowId")
    suspend fun updateAmounts(rowId: Long, sets: Int?, repsPerSet: Int?, duration: Int?, calories: Int)

    @Query("""
        UPDATE today_exercises 
        SET name = :name, category = :category, difficulty = :difficulty, description = :description 
        WHERE exerciseId = :exerciseId
    """)
    suspend fun syncExerciseInfo(exerciseId: String, name: String, category: String, difficulty: String, description: String)
}

// 4) Database
@Database(
    entities = [TodayExerciseEntity::class, CustomExerciseData::class],
    version = 5,
    exportSchema = false
)
abstract class FitTrackDatabase : RoomDatabase() {
    abstract fun todayExerciseDao(): TodayExerciseDao
    abstract fun customExerciseDao(): CustomExerciseDao

    companion object {
        @Volatile private var INSTANCE: FitTrackDatabase? = null

        fun getInstance(context: Context): FitTrackDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    FitTrackDatabase::class.java,
                    "fittrack.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

// 5) Repository
class TodoRepository(
    private val context: Context,
    private val dao: TodayExerciseDao,
    private val customDao: CustomExerciseDao,
    private val photoRepository: PhotoRepository
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun observeCustomCatalog(): Flow<List<Exercise>> =
        customDao.observeAll().map { list ->
            list.map { e ->
                Exercise(
                    id = e.id,
                    name = e.name,
                    category = e.category,
                    sets = e.sets,
                    repsPerSet = e.repsPerSet,
                    duration = e.duration,
                    calories = e.calories,
                    difficulty = e.difficulty,
                    description = e.description
                )
            }
        }

    suspend fun upsertCustomExercise(ex: Exercise) {
        customDao.upsert(
            CustomExerciseData(
                id = ex.id,
                name = ex.name,
                category = ex.category,
                calories = ex.calories,
                difficulty = ex.difficulty,
                description = ex.description,
                sets = ex.sets ?: 1,
                repsPerSet = ex.repsPerSet,
                duration = ex.duration
            )
        )
        dao.syncExerciseInfo(
            exerciseId = ex.id,
            name = ex.name,
            category = ex.category,
            difficulty = ex.difficulty,
            description = ex.description
        )
    }

    suspend fun deleteCustomExercise(id: String) {
        customDao.deleteById(id)
    }

    fun todayKey(): String = LocalDate.now().toString()
    fun getAllExerciseDates(): kotlinx.coroutines.flow.Flow<List<String>> = dao.getAllExerciseDates()


    suspend fun loadCatalogFromAssets(): List<Exercise> = withContext(Dispatchers.IO) {
        val text = context.assets.open("exercise_database.json")
            .bufferedReader()
            .use { it.readText() }

        json.decodeFromString<List<Exercise>>(text)
    }

    fun observeToday(dateKey: String) = dao.observeToday(dateKey)

    suspend fun completeRecord(rowId: Long, actualSec: Int, actualReps: Int, calories: Int) {
        dao.completeExerciseRecord(rowId, actualSec, actualReps, calories)
    }

    // ✅ 체크 해제 시 초기화 메서드 추가
    suspend fun resetRecord(rowId: Long, calories: Int) {
        dao.resetExerciseRecord(rowId, calories)
    }

    suspend fun cleanupNotToday(todayKey: String) = withContext(Dispatchers.IO) {
        val dates = dao.getAllExerciseDates().first()
        for (date in dates) {
            if (date != todayKey) {
                val photos = photoRepository.getPhotosForDate(date).first()
                if (photos.isEmpty()) {
                    val drawableId = R.drawable.dumbel
                    val uri = Uri.parse("android.resource://com.example.fittrack/" + drawableId)
                    photoRepository.insertPhoto(uri, date)
                }
            }
        }

        dao.deleteNotToday(todayKey)
    }


    suspend fun addToToday(
        ex: Exercise,
        dateKey: String,
        sets: Int = 1,
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
