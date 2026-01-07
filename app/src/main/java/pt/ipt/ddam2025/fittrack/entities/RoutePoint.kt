package pt.ipt.ddam2025.fittrack.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(indices = [Index("workoutId")])
data class RoutePoint(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutId: Long,
    val ts: Long,
    val lat: Double,
    val lon: Double,
    val alt: Double?
)
