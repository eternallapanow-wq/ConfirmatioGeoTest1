package pl.confirmatio.geotest

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var status: TextView
    private lateinit var logView: TextView
    private lateinit var radiusInput: EditText
    private val fused by lazy { LocationServices.getFusedLocationProviderClient(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildUi())
        requestNeededPermissions()
        refresh()
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun buildUi(): ScrollView {
        val scroll = ScrollView(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(24))
        }
        scroll.addView(root)

        root.addView(TextView(this).apply {
            text = "Confirmatio – Geo Test"
            textSize = 26f
            setTypeface(null, Typeface.BOLD)
        })
        root.addView(TextView(this).apply {
            text = "Test strefy wokół domu. Ekran może być wygaszony. Odczyt żądany co 2 minuty."
            textSize = 15f
            setPadding(0, dp(8), 0, dp(16))
        })

        status = TextView(this).apply {
            textSize = 17f
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }
        root.addView(status, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        root.addView(TextView(this).apply { text = "Promień strefy (metry):" })
        radiusInput = EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText("100")
        }
        root.addView(radiusInput)

        root.addView(button("1. USTAW TUTAJ ŚRODEK STREFY") { setCurrentAsCenter() })
        root.addView(button("2. START TESTU") { startMonitoring() })
        root.addView(button("STOP TESTU") { stopMonitoring() })
        root.addView(button("ODŚWIEŻ LOG") { refresh() })
        root.addView(button("KOPIUJ LOG") { copyLog() })
        root.addView(button("WYCZYŚĆ LOG") {
            GeoStore.clearLog(this)
            refresh()
        })

        root.addView(TextView(this).apply {
            text = "Historia pomiarów"
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            setPadding(0, dp(20), 0, dp(8))
        })

        logView = TextView(this).apply {
            textSize = 14f
            typeface = Typeface.MONOSPACE
            setTextIsSelectable(true)
            setPadding(dp(10), dp(10), dp(10), dp(10))
        }
        root.addView(logView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        return scroll
    }

    private fun button(label: String, action: () -> Unit): Button = Button(this).apply {
        text = label
        gravity = Gravity.CENTER
        setOnClickListener { action() }
    }

    private fun requestNeededPermissions() {
        val permissions = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissions += Manifest.permission.ACCESS_FINE_LOCATION
        }
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            permissions += Manifest.permission.POST_NOTIFICATIONS
        }
        if (permissions.isNotEmpty()) ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 200)
    }

    private fun setCurrentAsCenter() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            toast("Najpierw zezwól na dokładną lokalizację")
            requestNeededPermissions()
            return
        }
        val radius = radiusInput.text.toString().toFloatOrNull()?.coerceIn(20f, 1000f) ?: 100f
        fused.lastLocation.addOnSuccessListener { loc ->
            if (loc == null) {
                toast("Brak lokalizacji. Wyjdź na chwilę na zewnątrz i spróbuj ponownie.")
                return@addOnSuccessListener
            }
            GeoStore.setCenter(this, loc.latitude, loc.longitude, radius)
            GeoStore.appendLog(this, stamp() + "  USTAWIONO STREFĘ | promień ${radius.toInt()} m | dokładność ±${loc.accuracy.toInt()} m")
            refresh()
            toast("Środek strefy ustawiony")
        }.addOnFailureListener {
            toast("Nie udało się pobrać lokalizacji: ${it.message}")
        }
    }

    private fun startMonitoring() {
        if (GeoStore.getCenter(this) == null) {
            toast("Najpierw ustaw środek strefy")
            return
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            toast("Brak uprawnienia dokładnej lokalizacji")
            return
        }
        val radius = radiusInput.text.toString().toFloatOrNull()?.coerceIn(20f, 1000f) ?: 100f
        GeoStore.setRadius(this, radius)
        GeoStore.appendLog(this, stamp() + "  START monitoringu | interwał żądany 2 min | promień ${radius.toInt()} m")
        ContextCompat.startForegroundService(this, Intent(this, LocationMonitorService::class.java))
        refresh()
        toast("Monitoring działa. Możesz zgasić ekran.")
    }

    private fun stopMonitoring() {
        stopService(Intent(this, LocationMonitorService::class.java))
        refresh()
    }

    private fun refresh() {
        val center = GeoStore.getCenter(this)
        val running = GeoStore.isRunning(this)
        status.text = buildString {
            append(if (running) "MONITORING: WŁĄCZONY ✅\n" else "MONITORING: WYŁĄCZONY\n")
            if (center == null) append("Strefa: nieustawiona")
            else append("Strefa: ustawiona • promień ${center.third.toInt()} m")
        }
        if (center != null) radiusInput.setText(center.third.toInt().toString())
        logView.text = GeoStore.getLog(this).ifBlank { "Brak pomiarów." }
    }

    private fun copyLog() {
        val text = GeoStore.getLog(this)
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("Confirmatio log", text))
        toast("Log skopiowany")
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    private fun stamp(): String = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
