package pt.ipt.ddam2025.fittrack

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import pt.ipt.ddam2025.fittrack.data.FitTrackDatabase
import pt.ipt.ddam2025.fittrack.data.entities.Workout

class RepsFragment : Fragment(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null

    private var reps = 0
    private var isGoingDown = false
    private var isGoingUp = false
    private var tracking = false
    private var startElapsed: Long = 0L

    private lateinit var tvReps: TextView
    private lateinit var tvTime: TextView
    private lateinit var btnStart: Button
    private lateinit var btnStop: Button
    private lateinit var spExercise: Spinner
    private var selectedExercise = "Flexões"

    // Novo Handler que funciona SEMPRE
    private val handler = Handler()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_reps, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvReps = view.findViewById(R.id.tvReps)
        tvTime = view.findViewById(R.id.tvTime)
        btnStart = view.findViewById(R.id.btnStart)
        btnStop = view.findViewById(R.id.btnStop)
        spExercise = view.findViewById(R.id.spExercise)

        tvTime.text = "Tempo: 00:00"
        tvReps.text = "Repetições: 0"

        sensorManager = requireActivity().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        setupExerciseSelector()

        btnStart.setOnClickListener { startTracking() }
        btnStop.setOnClickListener { stopTracking() }

        updateUi()
    }

    private fun setupExerciseSelector() {
        val exercises = listOf("Flexões", "Agachamentos", "Abdominais")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, exercises)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spExercise.adapter = adapter

        spExercise.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                selectedExercise = exercises[pos]
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun startTracking() {
        reps = 0
        isGoingDown = false
        isGoingUp = false
        tracking = true
        startElapsed = SystemClock.elapsedRealtime()

        // Atualizar tempo imediatamente
        tvTime.text = "Tempo: 00:00"

        // Botões
        btnStart.isEnabled = false
        btnStart.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#DDDDDD"))

        btnStop.isEnabled = true
        btnStop.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F44336"))

        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)

        handler.post(tickRunnable)  // INICIA O CRONÓMETRO

        updateUi()
    }

    private fun stopTracking() {
        tracking = false
        sensorManager.unregisterListener(this)
        handler.removeCallbacks(tickRunnable)

        saveWorkout()

        // Reset visual
        btnStart.isEnabled = true
        btnStart.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#4CAF50"))

        btnStop.isEnabled = false
        btnStop.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#CCCCCC"))

        tvTime.text = "Tempo: 00:00"

        updateUi()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (!tracking || event == null) return

        val z = event.values[2]
        val y = event.values[1]

        when (selectedExercise) {

            "Flexões" -> {
                if (z < -5 && !isGoingDown) isGoingDown = true
                if (z > 5 && isGoingDown) {
                    reps++
                    isGoingDown = false
                    tvReps.text = "Repetições: $reps"
                }
            }

            "Agachamentos" -> {
                if (y < -4 && !isGoingDown) isGoingDown = true
                if (y > 4 && isGoingDown) {
                    reps++
                    isGoingDown = false
                    tvReps.text = "Repetições: $reps"
                }
            }

            "Abdominais" -> {
                if (z > 6 && !isGoingUp) isGoingUp = true
                if (z < 2 && isGoingUp) {
                    reps++
                    isGoingUp = false
                    tvReps.text = "Repetições: $reps"
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun updateUi() {
        btnStart.isEnabled = !tracking
        btnStop.isEnabled = tracking

        if (!tracking) {
            tvTime.text = "Tempo: " + formatHMS(elapsedSeconds())
        }
    }

    private fun elapsedSeconds(): Int =
        ((SystemClock.elapsedRealtime() - startElapsed) / 1000).toInt()

    private fun formatHMS(totalSec: Int): String {
        val m = totalSec / 60
        val s = totalSec % 60
        return "%02d:%02d".format(m, s)
    }

    // NOVO TICKER 100% WORKING
    private val tickRunnable = object : Runnable {
        override fun run() {
            if (tracking) {
                tvTime.text = "Tempo: " + formatHMS(elapsedSeconds())
                handler.postDelayed(this, 1000)
            }
        }
    }

    private fun saveWorkout() {
        val ctx = requireContext().applicationContext
        val durationSec = elapsedSeconds()

        lifecycleScope.launch {
            val db = FitTrackDatabase.get(ctx)

            db.workoutDao().insert(
                Workout(
                    type = selectedExercise.uppercase(),
                    startTime = System.currentTimeMillis() - durationSec * 1000,
                    endTime = System.currentTimeMillis(),
                    durationSec = durationSec,
                    distanceMeters = 0.0,
                    paceSecPerKm = 0
                )
            )

            Toast.makeText(ctx, "Treino guardado ✅", Toast.LENGTH_SHORT).show()
        }
    }
}
