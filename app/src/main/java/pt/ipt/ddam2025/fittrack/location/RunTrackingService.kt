package pt.ipt.ddam2025.fittrack.location

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.location.FusedLocationProviderClient

class RunTrackingService : Service() {

    companion object {
        const val CHANNEL_ID = "run_tracking_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "pt.ipt.fittrack.ACTION_START"
        const val ACTION_STOP = "pt.ipt.fittrack.ACTION_STOP"

        const val ACTION_LOCATION_UPDATE = "pt.ipt.fittrack.ACTION_LOCATION_UPDATE"
        const val EXTRA_LOCATION = "extra_location"
    }

    private lateinit var fused: FusedLocationProviderClient
    private lateinit var request: LocationRequest

    override fun onCreate() {
        super.onCreate()
        fused = LocationServices.getFusedLocationProviderClient(this)
        request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            2000L // 2s
        )
            .setMinUpdateIntervalMillis(1000L)
            .setMinUpdateDistanceMeters(2f)
            .build()

        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startForeground(NOTIFICATION_ID, buildNotification())
            ACTION_STOP  -> stopSelf()
        }
        startLocationUpdates()
        return START_STICKY
    }

    override fun onDestroy() {
        fused.removeLocationUpdates(locationCb)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startLocationUpdates() {
        try {
            fused.requestLocationUpdates(request, locationCb, Looper.getMainLooper())
        } catch (_: SecurityException) {
            stopSelf()
        }
    }

    private val locationCb = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val last: Location = result.lastLocation ?: return

            // Broadcast para o fragment
            val i = Intent(ACTION_LOCATION_UPDATE).apply {
                putExtra(EXTRA_LOCATION, last)
            }
            sendBroadcast(i)

            // Atualiza a notificação com as coordenadas
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(
                NOTIFICATION_ID,
                buildNotification("Lat: ${last.latitude.format(5)}  Lon: ${last.longitude.format(5)}")
            )
        }
    }

    private fun buildNotification(text: String = "A gravar a sua corrida..."): Notification {
        val stopIntent = Intent(this, RunTrackingService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPending = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play) // <- não precisamos importar android.R
            .setContentTitle("FitTrack")
            .setContentText(text)
            .setOngoing(true)
            .addAction(0, "Parar", stopPending) // <- addAction (não setAction)
            .build()
    }

    @SuppressLint("NewApi") // já protegemos com o if (SDK >= 26)
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Rastreamento de Corrida",
                NotificationManager.IMPORTANCE_LOW
            )
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }
}

private fun Double.format(digits: Int) = "%.${digits}f".format(this)
