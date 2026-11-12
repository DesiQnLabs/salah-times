package com.salahtimes.app.data

import android.content.Context

class Settings(context: Context) {
    private val prefs = context.getSharedPreferences("salah_settings", Context.MODE_PRIVATE)
    var city: String
        get() = prefs.getString("city", "Alexandria, VA") ?: "Alexandria, VA"
        set(value) = prefs.edit().putString("city", value).apply()
    var latitude: Double
        get() = java.lang.Double.longBitsToDouble(prefs.getLong("lat", java.lang.Double.doubleToLongBits(38.8048)))
        set(value) = prefs.edit().putLong("lat", java.lang.Double.doubleToLongBits(value)).apply()
    var longitude: Double
        get() = java.lang.Double.longBitsToDouble(prefs.getLong("lon", java.lang.Double.doubleToLongBits(-77.0469)))
        set(value) = prefs.edit().putLong("lon", java.lang.Double.doubleToLongBits(value)).apply()
    var method: String
        get() = prefs.getString("method", "ISNA") ?: "ISNA"
        set(value) = prefs.edit().putString("method", value).apply()
    var hanafi: Boolean
        get() = prefs.getBoolean("hanafi", false)
        set(value) = prefs.edit().putBoolean("hanafi", value).apply()
    var notifications: Boolean
        get() = prefs.getBoolean("notifications", false)
        set(value) = prefs.edit().putBoolean("notifications", value).apply()
}
