package com.aprax.aidesignstudio.data

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

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String,
    val prompt: String,
    val designJson: String,
    val favorite: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY favorite DESC, updatedAt DESC")
    fun observeAll(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    suspend fun get(id: String): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun delete(id: String)

    @Query("UPDATE projects SET favorite = :favorite, updatedAt = :time WHERE id = :id")
    suspend fun setFavorite(id: String, favorite: Boolean, time: Long = System.currentTimeMillis())
}

@Database(entities = [ProjectEntity::class], version = 1, exportSchema = false)
abstract class DesignoraDatabase : RoomDatabase() {
    abstract fun projects(): ProjectDao

    companion object {
        @Volatile private var INSTANCE: DesignoraDatabase? = null
        fun get(context: Context): DesignoraDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                DesignoraDatabase::class.java,
                "designora.db"
            ).build().also { INSTANCE = it }
        }
    }
}
