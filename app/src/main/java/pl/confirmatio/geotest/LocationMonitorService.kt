package pl.confirmatio.geotest

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.IBinder
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LocationMonitorService : Service() {

    private lateinit var fused: FusedLocationProviderClient
    private lateinit var callback: LocationCallback

    override fun onCreate() {
        super.onCreate()
        fused = LocationServices.getFusedLocationProviderClient(this)
        createChannel()
        startForeground(NOTIFICATION_ID, notification("Uruchamianie monitoringu…"))
        GeoStore.setRunning(this, true)
        startUpdates()
    }

    private fun startUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            GeoStore.appendLog(this, stamp() + "  BŁĄD: brak uprawnienia lokalizacji")
            stopSelf()
            return
        }

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 120_000L)
            .setMinUpdateIntervalMillis(120_000L)
            .setMaxUpdateDelayMillis(120_000L)
            .setWaitForAccurateLocation(false)
            .build()

        callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                processLocation(location)
            }
        }

        fused.requestLocationUpdates(request, callback, mainLooper)
    }

    private fun processLocation(location: Location) {
        val center = GeoStore.getCenter(this)
        if (center == null) {
            GeoStore.appendLog(this, stamp() + "  BŁĄD: nie ustawiono strefy")
            return
        }

        val result = FloatArray(1)
        Location.distanceBetween(center.first, center.second, location.latitude, location.longitude, result)
        val distance = result[0]
        val inside = distance <= center.third
        val status = if (inside) "W STREFIE ✅" else "POZA STREFĄ ❌"
        val accuracy = location.accuracy.toInt()
        val line = "%s  %s | %.0f m od środka | dokładność ±%d m".format(
            stamp(), status, distance, accuracy
        )
        GeoStore.appendLog(this, line)

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification("$status • ${distance.toInt()} m • ±${accuracy} m"))
    }

    private fun notification(text: String) = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_menu_mylocation)
        .setContentTitle("Confirmatio – test lokalizacji")
        .setContentText(text)
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .build()

    private fun createChannel() {
        val nm = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(CHANNEL_ID, "Monitoring lokalizacji", NotificationManager.IMPORTANCE_LOW)
        nm.createNotificationChannel(channel)
    }

    private fun stamp(): String = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

    override fun onDestroy() {
        if (::callback.isInitialized) fused.removeLocationUpdates(callback)
        GeoStore.appendLog(this, stamp() + "  STOP monitoringu")
        GeoStore.setRunning(this, false)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL_ID = "geo_test_channel"
        private const val NOTIFICATION_ID = 1122
    }
}
