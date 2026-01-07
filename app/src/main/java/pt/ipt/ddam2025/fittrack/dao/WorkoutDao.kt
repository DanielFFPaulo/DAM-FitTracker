package pt.ipt.ddam2025.fittrack.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import pt.ipt.ddam2025.fittrack.data.entities.Workout

@Dao
interface WorkoutDao {
    @Insert
    suspend fun insert(workout: Workout): Long

    @Query("SELECT * FROM Workout ORDER BY startTime DESC")
    fun getAll(): androidx.lifecycle.LiveData<List<Workout>>

    @Delete
    suspend fun delete(workout: Workout)
}
