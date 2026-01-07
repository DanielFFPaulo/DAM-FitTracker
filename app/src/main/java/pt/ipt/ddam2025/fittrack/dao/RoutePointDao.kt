package pt.ipt.ddam2025.fittrack.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import pt.ipt.ddam2025.fittrack.data.entities.RoutePoint

@Dao
interface RoutePointDao {
    @Insert suspend fun insertAll(points: List<RoutePoint>)

    @Query("SELECT * FROM RoutePoint WHERE workoutId = :wId ORDER BY ts ASC")
    suspend fun forWorkout(wId: Long): List<RoutePoint>
}
