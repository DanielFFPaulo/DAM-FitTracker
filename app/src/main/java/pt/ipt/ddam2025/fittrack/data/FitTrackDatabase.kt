package pt.ipt.ddam2025.fittrack.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import pt.ipt.ddam2025.fittrack.data.dao.RoutePointDao
import pt.ipt.ddam2025.fittrack.data.dao.WorkoutDao
import pt.ipt.ddam2025.fittrack.data.entities.RoutePoint
import pt.ipt.ddam2025.fittrack.data.entities.Workout

@Database(
    entities = [Workout::class, RoutePoint::class],
    version = 1,
    exportSchema = false
)
abstract class FitTrackDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao
    abstract fun routePointDao(): RoutePointDao

    companion object {
        @Volatile private var INSTANCE: FitTrackDatabase? = null

        fun get(ctx: Context): FitTrackDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    ctx.applicationContext,
                    FitTrackDatabase::class.java,
                    "fittrack.db"
                ).build().also { INSTANCE = it }
            }
    }
}
