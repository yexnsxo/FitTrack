package com.example.fittrack.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
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

    // 사용자가 선택한 값
    val sets: Int = 1,
    val repsPerSet: Int? = null,
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

    @Query("""UPDATE today_exercises SET sets = :sets, repsPerSet = :repsPerSet, duration = :duration, calories = :calories WHERE rowId = :rowId""")
    suspend fun updateAmounts(rowId: Long, sets: Int?, repsPerSet: Int?, duration: Int?, calories: Int)

    // ✅ 커스텀 운동 정보 수정 시 투두 항목들도 동기화하기 위한 쿼리
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
    version = 4,
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
    private val customDao: CustomExerciseDao
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
        // ✅ 마스터 정보 업데이트 시 오늘(혹은 과거)의 운동 내역들도 동기화
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
