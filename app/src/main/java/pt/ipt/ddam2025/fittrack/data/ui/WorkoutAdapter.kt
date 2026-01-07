package pt.ipt.ddam2025.fittrack.ui

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import pt.ipt.ddam2025.fittrack.R
import pt.ipt.ddam2025.fittrack.data.FitTrackDatabase
import pt.ipt.ddam2025.fittrack.data.entities.Workout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class WorkoutAdapter(
    private val context: Context,
    private val workouts: MutableList<Workout>
) : RecyclerView.Adapter<WorkoutAdapter.WorkoutViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkoutViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_workout, parent, false)
        return WorkoutViewHolder(view)
    }

    override fun onBindViewHolder(holder: WorkoutViewHolder, position: Int) {
        val workout = workouts[position]
        holder.bind(workout)
    }

    override fun getItemCount(): Int = workouts.size

    inner class WorkoutViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvWorkoutInfo: TextView = itemView.findViewById(R.id.tvWorkoutInfo)
        private val btnShare: Button = itemView.findViewById(R.id.btnShare)
        private val btnDelete: Button = itemView.findViewById(R.id.btnDelete)

        fun bind(workout: Workout) {
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val date = sdf.format(Date(workout.startTime))

            val typeLabel = when (workout.type.uppercase()) {
                "RUN" -> "🏃 Corrida"
                "FLEXÕES" -> "💪 Flexões"
                "AGACHAMENTOS" -> "🦵 Agachamentos"
                "ABDOMINAIS" -> "🔥 Abdominais"
                else -> workout.type
            }

            val info = """
        $typeLabel
        $date
        Distância: %.2f km
        Tempo: %02d:%02d
        Ritmo: %d:%02d min/km
    """.trimIndent().format(
                workout.distanceMeters / 1000.0,
                workout.durationSec / 60, workout.durationSec % 60,
                workout.paceSecPerKm / 60, workout.paceSecPerKm % 60
            )

            tvWorkoutInfo.text = info

            // Partilhar
            btnShare.setOnClickListener {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "O meu treino no FitTrack:\n\n$info")
                }
                context.startActivity(Intent.createChooser(shareIntent, "Partilhar treino via"))
            }

            // Remover
            btnDelete.setOnClickListener {
                AlertDialog.Builder(context)
                    .setTitle("Remover treino")
                    .setMessage("Tens a certeza que queres eliminar este treino?")
                    .setPositiveButton("Sim") { _, _ -> deleteWorkout(workout) }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
        }


        private fun deleteWorkout(workout: Workout) {
            CoroutineScope(Dispatchers.IO).launch {
                val db = FitTrackDatabase.get(context)
                db.workoutDao().delete(workout)

                // atualizar lista na UI
                CoroutineScope(Dispatchers.Main).launch {
                    workouts.remove(workout)
                    notifyDataSetChanged()
                    Toast.makeText(context, "Treino removido ✅", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
