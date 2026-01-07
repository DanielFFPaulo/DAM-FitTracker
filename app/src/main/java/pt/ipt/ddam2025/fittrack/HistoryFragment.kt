package pt.ipt.ddam2025.fittrack

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import pt.ipt.ddam2025.fittrack.data.FitTrackDatabase
import pt.ipt.ddam2025.fittrack.data.entities.Workout
import pt.ipt.ddam2025.fittrack.ui.WorkoutAdapter

class HistoryFragment : Fragment() {

    private lateinit var rvHistory: RecyclerView

    private lateinit var adapter: WorkoutAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_history, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        rvHistory = view.findViewById(R.id.rvHistory)
        rvHistory.layoutManager = LinearLayoutManager(requireContext())

        val db = FitTrackDatabase.get(requireContext())
        db.workoutDao().getAll().observe(viewLifecycleOwner, Observer { workouts ->
            adapter = WorkoutAdapter(requireContext(), workouts.toMutableList())
            rvHistory.adapter = adapter
        })
    }
}
