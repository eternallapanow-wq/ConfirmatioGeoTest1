package pl.confirmatio.geotest

import android.content.Context

object GeoStore {
    private const val PREFS = "geo_test"
    private const val KEY_LAT = "center_lat"
    private const val KEY_LON = "center_lon"
    private const val KEY_RADIUS = "radius_m"
    private const val KEY_LOG = "log"
    private const val KEY_RUNNING = "running"

    fun setCenter(context: Context, lat: Double, lon: Double, radius: Float) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putLong(KEY_LAT, java.lang.Double.doubleToRawLongBits(lat))
            .putLong(KEY_LON, java.lang.Double.doubleToRawLongBits(lon))
            .putFloat(KEY_RADIUS, radius)
            .apply()
    }

    fun getCenter(context: Context): Triple<Double, Double, Float>? {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!p.contains(KEY_LAT) || !p.contains(KEY_LON)) return null
        val lat = java.lang.Double.longBitsToDouble(p.getLong(KEY_LAT, 0L))
        val lon = java.lang.Double.longBitsToDouble(p.getLong(KEY_LON, 0L))
        val radius = p.getFloat(KEY_RADIUS, 100f)
        return Triple(lat, lon, radius)
    }

    fun setRadius(context: Context, radius: Float) {
        val c = getCenter(context) ?: return
        setCenter(context, c.first, c.second, radius)
    }

    fun appendLog(context: Context, line: String) {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val old = p.getString(KEY_LOG, "") ?: ""
        val lines = (old + line + "\n").lines().takeLast(250).joinToString("\n")
        p.edit().putString(KEY_LOG, lines).apply()
    }

    fun getLog(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_LOG, "") ?: ""

    fun clearLog(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_LOG, "").apply()
    }

    fun setRunning(context: Context, value: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_RUNNING, value).apply()
    }

    fun isRunning(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_RUNNING, false)
}
