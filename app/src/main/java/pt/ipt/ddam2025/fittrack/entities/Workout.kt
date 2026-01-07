package pt.ipt.ddam2025.fittrack.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Workout(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,             // "RUN"
    val startTime: Long,          // epoch millis
    val endTime: Long,            // epoch millis
    val durationSec: Int,
    val distanceMeters: Double,
    val paceSecPerKm: Int
)
