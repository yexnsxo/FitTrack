package com.example.fittrack.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "custom_exercises")
data class CustomExerciseData(
    @PrimaryKey
    val id: String,
    val name: String,
    val category: String,

    val calories: Double,
    val difficulty: String,
    val description: String,

    val sets: Int,              // ✅ non-null (기본 1)
    val repsPerSet: Int?,       // ✅ 선택
    val duration: Int?          // ✅ 선택
)

@Dao
interface CustomExerciseDao {
    @Query("SELECT * FROM custom_exercises")
    fun observeAll(): Flow<List<CustomExerciseData>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CustomExerciseData)

    @Query("DELETE FROM custom_exercises WHERE id = :id")
    suspend fun deleteById(id: String)
}
