package pt.ipt.ddam2025.fittrack

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.*
import kotlinx.coroutines.launch
import pt.ipt.ddam2025.fittrack.data.FitTrackDatabase
import pt.ipt.ddam2025.fittrack.data.entities.RoutePoint
import pt.ipt.ddam2025.fittrack.data.entities.Workout
import kotlin.math.round
import android.content.res.ColorStateList
import android.graphics.Color

class RunFragment : Fragment() {

    // GPS
    private lateinit var fused: FusedLocationProviderClient
    private lateinit var request: LocationRequest
    private lateinit var callback: LocationCallback

    private val routeBuffer = mutableListOf<RoutePoint>()
    private var startWallClock: Long = 0L

    // Estado
    private var tracking = false
    private var lastLocation: Location? = null
    private var metersTotal = 0.0
    private var startElapsed: Long = 0L

    // UI
    private lateinit var tvDistance: TextView
    private lateinit var tvTime: TextView
    private lateinit var tvPace: TextView
    private lateinit var btnStart: Button
    private lateinit var btnStop: Button

    private val handler = Handler()

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
            val ok = (grants[Manifest.permission.ACCESS_FINE_LOCATION] == true) ||
                    (grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true)

            if (ok) startTracking() else showPermissionDeniedToast()
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_run, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvDistance = view.findViewById(R.id.tvDistance)
        tvTime = view.findViewById(R.id.tvTime)
        tvPace = view.findViewById(R.id.tvPace)
        btnStart = view.findViewById(R.id.btnStart)
        btnStop = view.findViewById(R.id.btnStop)

        tvTime.text = "Tempo: 00:00:00"

        fused = LocationServices.getFusedLocationProviderClient(requireActivity())
        request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
            .setMinUpdateIntervalMillis(500L)
            .build()

        callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                for (loc in result.locations) onNewLocation(loc)
            }
        }

        btnStart.setOnClickListener { startTracking() }
        btnStop.setOnClickListener { stopTracking() }

        updateUi()
    }

    // ---------------- TRACKING ----------------

    private fun startTracking() {
        if (!hasLocationPermission()) {
            requestLocationPermission()
            return
        }

        tracking = true
        lastLocation = null
        metersTotal = 0.0
        startElapsed = SystemClock.elapsedRealtime()
        tvTime.text = "Tempo: 00:00:00"

        safeRequestLocationUpdates()
        handler.post(tickRunnable)

        // 🔵 Botões — quando começa
        btnStart.isEnabled = false
        btnStart.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#DDDDDD")) // cinzento

        btnStop.isEnabled = true
        btnStop.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F44336")) // vermelho

        routeBuffer.clear()
        startWallClock = System.currentTimeMillis()

        updateUi()
    }

    private fun stopTracking() {
        tracking = false
        safeRemoveLocationUpdates()
        handler.removeCallbacks(tickRunnable)

        saveWorkout()
        updateUi()

        // Botões — quando para
        btnStart.isEnabled = true
        btnStart.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#4CAF50")) // verde

        btnStop.isEnabled = false
        btnStop.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#CCCCCC")) // cinzento
    }

    private fun onNewLocation(loc: Location) {
        lastLocation?.let { prev -> metersTotal += prev.distanceTo(loc) }
        lastLocation = loc

        routeBuffer += RoutePoint(
            workoutId = 0,
            ts = System.currentTimeMillis(),
            lat = loc.latitude,
            lon = loc.longitude,
            alt = if (loc.hasAltitude()) loc.altitude else null
        )

        refreshStats()
    }

    // ---------------- UI + CRONÓMETRO ----------------

    private fun updateUi() {
        btnStart.isEnabled = !tracking
        btnStop.isEnabled = tracking

        if (!tracking) refreshStats()
    }

    private fun refreshStats() {
        val km = metersTotal / 1000.0
        tvDistance.text = "Distância: %.2f km".format(km)

        val secs = elapsedSeconds()
        tvTime.text = "Tempo: " + formatHMS(secs)

        tvPace.text = if (km > 0.01) {
            val paceSecPerKm = secs / km
            val paceMin = (paceSecPerKm / 60).toInt()
            val paceSec = round(paceSecPerKm % 60).toInt()
            "Ritmo: %d:%02d min/km".format(paceMin, paceSec)
        } else {
            "Ritmo: 0:00 min/km"
        }
    }

    private fun elapsedSeconds(): Int =
        ((SystemClock.elapsedRealtime() - startElapsed) / 1000).toInt()

    private fun formatHMS(totalSec: Int): String {
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return "%02d:%02d:%02d".format(h, m, s)
    }

    private val tickRunnable = object : Runnable {
        override fun run() {
            if (tracking) {
                refreshStats()
                handler.postDelayed(this, 1000)
            }
        }
    }

    // ---------------- SAVE ----------------

    private fun saveWorkout() {
        val ctx = requireContext().applicationContext
        val distance = metersTotal
        val durationSec = elapsedSeconds()
        val paceSecPerKm = if (distance > 10.0) (durationSec / (distance / 1000.0)).toInt() else 0

        lifecycleScope.launch {
            val db = FitTrackDatabase.get(ctx)
            val workoutId = db.workoutDao().insert(
                Workout(
                    type = "RUN",
                    startTime = startWallClock,
                    endTime = System.currentTimeMillis(),
                    durationSec = durationSec,
                    distanceMeters = distance,
                    paceSecPerKm = paceSecPerKm
                )
            )

            if (routeBuffer.isNotEmpty()) {
                val points = routeBuffer.map { it.copy(workoutId = workoutId) }
                db.routePointDao().insertAll(points)
            }

            Toast.makeText(ctx, "Treino guardado ✅", Toast.LENGTH_SHORT).show()
        }
    }

    // ---------------- PERMISSÕES ----------------

    private fun hasLocationPermission(): Boolean {
        val ctx = requireContext()
        return ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun showPermissionDeniedToast() =
        Toast.makeText(requireContext(), "Permissão de localização recusada", Toast.LENGTH_SHORT).show()

    private fun safeRequestLocationUpdates() {
        if (!hasLocationPermission()) return
        try {
            fused.requestLocationUpdates(request, callback, requireActivity().mainLooper)
        } catch (_: SecurityException) {}
    }

    private fun safeRemoveLocationUpdates() {
        try { fused.removeLocationUpdates(callback) } catch (_: SecurityException) {}
    }

    override fun onStart() {
        super.onStart()
        if (tracking && hasLocationPermission()) handler.post(tickRunnable)
    }

    override fun onStop() {
        super.onStop()
        handler.removeCallbacks(tickRunnable)
    }
}
