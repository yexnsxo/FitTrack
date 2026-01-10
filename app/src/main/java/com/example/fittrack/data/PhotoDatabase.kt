package com.example.fittrack.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "photos", indices = [Index(value = ["date"], unique = true)])
data class Photo(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val uri: String,
    val date: String,
    val createdAt: Long
)

@Dao
interface PhotoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(photo: Photo)

    @Query("SELECT * FROM photos ORDER BY id DESC")
    fun getAllPhotos(): Flow<List<Photo>>

    @Query("SELECT * FROM photos WHERE date = :date")
    fun getPhotoForDate(date: String): Flow<List<Photo>>

    @Delete
    suspend fun delete(photo: Photo)
}

// Update database version to 4
@Database(entities = [Photo::class], version = 4, exportSchema = false)
abstract class PhotoDatabase : RoomDatabase() {
    abstract fun photoDao(): PhotoDao

    companion object {
        @Volatile
        private var INSTANCE: PhotoDatabase? = null

        fun getDatabase(context: Context): PhotoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PhotoDatabase::class.java,
                    "photo_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
