package com.salahtimes.app.data

import java.time.*
import kotlin.math.*

/** Offline astronomical approximation using NOAA solar equations. Times are local to the device timezone.
 * For travel, set the phone timezone to the location timezone. Polar fallback is an approximation.
 */
object PrayerCalculator {
    data class Prayer(val name: String, val time: ZonedDateTime)
    private val names = listOf("Fajr", "Sunrise", "Dhuhr", "Asr", "Maghrib", "Isha")
    private fun rad(x: Double) = Math.toRadians(x)
    private fun deg(x: Double) = Math.toDegrees(x)
    private fun sinD(x: Double) = sin(rad(x))
    private fun cosD(x: Double) = cos(rad(x))
    private fun acosD(x: Double) = deg(acos(x.coerceIn(-1.0, 1.0)))
    private fun atanD(x: Double) = deg(atan(x))
    private fun solar(date: LocalDate): Pair<Double, Double> {
        val day = date.dayOfYear.toDouble()
        val days = if (date.isLeapYear) 366.0 else 365.0
        val gamma = 2.0 * Math.PI / days * (day - 1.0)
        val eq = 229.18 * (0.000075 + 0.001868 * cos(gamma) - 0.032077 * sin(gamma) - 0.014615 * cos(2 * gamma) - 0.040849 * sin(2 * gamma))
        val dec = 0.006918 - 0.399912 * cos(gamma) + 0.070257 * sin(gamma) - 0.006758 * cos(2 * gamma) + 0.000907 * sin(2 * gamma) - 0.002697 * cos(3 * gamma) + 0.00148 * sin(3 * gamma)
        return eq to deg(dec)
    }
    private fun hourAngle(lat: Double, decl: Double, altitude: Double): Double? {
        val denominator = cosD(lat) * cosD(decl)
        if (abs(denominator) < 1e-9) return null
        val c = (sinD(altitude) - sinD(lat) * sinD(decl)) / denominator
        return if (c in -1.0..1.0) acosD(c) * 4.0 else null
    }
    fun calculate(date: LocalDate, latitude: Double, longitude: Double, method: String, hanafi: Boolean, zone: ZoneId = ZoneId.systemDefault()): List<Prayer> {
        val lat = latitude.coerceIn(-89.0, 89.0)
        val lon = longitude.coerceIn(-180.0, 180.0)
        val (eq, decl) = solar(date)
        val utcNoon = 720.0 - 4.0 * lon - eq
        val riseOffset = hourAngle(lat, decl, -0.833) ?: 360.0
        val (fajrAngle, ishaAngle) = when(method) {
            "MWL" -> 18.0 to 17.0
            "Egypt" -> 19.5 to 17.5
            "Umm al-Qura" -> 18.5 to 0.0
            else -> 15.0 to 15.0 // ISNA
        }
        val fajrOffset = hourAngle(lat, decl, -fajrAngle) ?: (riseOffset + 90.0).coerceAtMost(600.0)
        val ishaOffset = hourAngle(lat, decl, -ishaAngle) ?: (riseOffset + 90.0).coerceAtMost(600.0)
        val shadow = if (hanafi) 2.0 else 1.0
        val asrAltitude = atanD(1.0 / (shadow + tan(rad(abs(lat - decl)))))
        val asrOffset = hourAngle(lat, decl, asrAltitude) ?: 180.0
        // Convert UTC minutes to a zoned instant; correctly handles DST on the target date.
        fun at(utcMinutes: Double): ZonedDateTime = date.atStartOfDay(ZoneOffset.UTC).toInstant()
            .plusSeconds((utcMinutes * 60).roundToLong()).atZone(zone)
        val offsets = listOf(utcNoon - fajrOffset, utcNoon - riseOffset, utcNoon + 1.0,
            utcNoon + asrOffset, utcNoon + riseOffset, if (method == "Umm al-Qura") utcNoon + riseOffset + 90.0 else utcNoon + ishaOffset)
        return names.zip(offsets).map { (name, minute) -> Prayer(name, at(minute)) }
    }
    fun qiblaBearing(latitude: Double, longitude: Double): Double {
        val phi = rad(latitude)
        val deltaLon = rad(39.8262 - longitude)
        val y = sin(deltaLon)
        val x = cos(phi) * tan(rad(21.4225)) - sin(phi) * cos(deltaLon)
        return (deg(atan2(y, x)) + 360.0) % 360.0
    }
}
