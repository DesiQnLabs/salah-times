package com.salahtimes.app

import android.Manifest
import android.app.DatePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.*
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.salahtimes.app.alerts.PrayerAlerts
import com.salahtimes.app.data.PrayerCalculator
import com.salahtimes.app.data.Settings
import kotlinx.coroutines.delay
import java.time.*
import java.time.format.DateTimeFormatter
import kotlin.math.*

private val bg = Color(0xFF062F2B)
private val panel = Color(0xFF10433D)
private val gold = Color(0xFFB9D8A7)
private val soft = Color(0xFFB9D0C8)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PrayerAlerts.channel(this)
        setContent { SalahApp() }
    }
}

@Composable
fun SalahApp() {
    val context = LocalContext.current
    val settings = remember { Settings(context) }
    var refresh by remember { mutableIntStateOf(0) }
    var tab by remember { mutableIntStateOf(0) }
    var darkMode by remember { mutableStateOf(settings.darkMode) }
    var now by remember { mutableStateOf(ZonedDateTime.now()) }
    LaunchedEffect(Unit) { while (true) { now = ZonedDateTime.now(); delay(1000) } }
    val prayers = remember(now.toLocalDate(), refresh) { PrayerCalculator.calculate(now.toLocalDate(), settings.latitude, settings.longitude, settings.method, settings.hanafi) }
    val tomorrow = remember(now.toLocalDate(), refresh) { PrayerCalculator.calculate(now.toLocalDate().plusDays(1), settings.latitude, settings.longitude, settings.method, settings.hanafi) }
    val next = (prayers + tomorrow).firstOrNull { it.name != "Sunrise" && it.time.isAfter(now) }
    val background = if (darkMode) bg else Color(0xFFF4F8F5)
    val surface = if (darkMode) panel else Color.White
    val foreground = if (darkMode) Color.White else Color(0xFF173D36)
    val muted = if (darkMode) soft else Color(0xFF547268)
    MaterialTheme(colorScheme = if (darkMode)
        darkColorScheme(primary = gold, background = background, surface = surface, onSurface = foreground, onBackground = foreground, onSurfaceVariant = muted)
        else lightColorScheme(primary = Color(0xFF236C56), background = background, surface = surface, onSurface = foreground, onBackground = foreground, onSurfaceVariant = muted)) {
        Scaffold(containerColor = background, bottomBar = {
            NavigationBar(containerColor = surface) {
                listOf(Triple("Today", Icons.Rounded.Schedule, 0), Triple("Calendar", Icons.Rounded.DateRange, 1), Triple("Qibla", Icons.Rounded.Explore, 2), Triple("Settings", Icons.Rounded.Settings, 3)).forEach { (title, icon, id) ->
                    NavigationBarItem(selected = tab == id, onClick = { tab = id }, icon = { Icon(icon, null) }, label = { Text(title) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = background, selectedTextColor = MaterialTheme.colorScheme.primary, indicatorColor = MaterialTheme.colorScheme.primary))
                }
            }
        }) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(22.dp)) {
                Text("SALAH TIMES", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, letterSpacing = 3.sp, fontSize = 14.sp)
                Spacer(Modifier.height(22.dp))
                when(tab) {
                    0 -> TodayScreen(settings.city, now, prayers, next)
                    1 -> CalendarScreen(settings, refresh)
                    2 -> QiblaScreen(settings.latitude, settings.longitude)
                    else -> SettingsScreen(settings, { refresh++; PrayerAlerts.schedule(context) }, darkMode, { darkMode = it; settings.darkMode = it })
                }
            }
        }
    }
}

@Composable
private fun TodayScreen(city: String, now: ZonedDateTime, prayers: List<PrayerCalculator.Prayer>, next: PrayerCalculator.Prayer?) {
    Text(city, fontSize = 27.sp, fontWeight = FontWeight.SemiBold)
    Text(now.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")), color = soft)
    Spacer(Modifier.height(24.dp))
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(25.dp)) {
            Text("NEXT PRAYER", color = gold, letterSpacing = 2.sp, fontSize = 12.sp)
            Spacer(Modifier.height(10.dp))
            Text(next?.name ?: "—", fontSize = 38.sp, fontWeight = FontWeight.Bold)
            val remaining = next?.let { Duration.between(now, it.time).seconds.coerceAtLeast(0) } ?: 0L
            Text("%02d:%02d:%02d remaining".format(remaining / 3600, (remaining % 3600) / 60, remaining % 60), color = gold, fontSize = 22.sp)
            Spacer(Modifier.height(12.dp))
            Text(next?.time?.format(DateTimeFormatter.ofPattern("h:mm a")) ?: "", color = soft)
        }
    }
    Spacer(Modifier.height(26.dp))
    Text("Today's prayers", fontSize = 21.sp, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(12.dp))
    prayers.forEach { prayer ->
        val selected = next?.name == prayer.name && next.time.toLocalDate() == prayer.time.toLocalDate()
        Row(Modifier.fillMaxWidth().padding(vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (prayer.name == "Sunrise") Icons.Rounded.WbSunny else Icons.Rounded.NightsStay, null, tint = if(selected) gold else soft, modifier = Modifier.size(21.dp))
            Spacer(Modifier.width(15.dp))
            Text(prayer.name, color = if (selected) gold else MaterialTheme.colorScheme.onSurface, fontWeight = if(selected) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.weight(1f), fontSize = 17.sp)
            Text(prayer.time.format(DateTimeFormatter.ofPattern("h:mm a")), color = if(selected) gold else soft)
        }
        HorizontalDivider(color = Color.White.copy(alpha = 0.07f))
    }
    Spacer(Modifier.height(20.dp))
    Text("Explore", fontSize = 21.sp, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(12.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        listOf("🕋" to "Qibla", "🤲" to "Duas", "📿" to "Tasbih", "📜" to "Inspiration", "📔" to "Journal").forEach { (emoji, label) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Text(emoji, fontSize = 27.sp)
                Text(label, fontSize = 10.sp, maxLines = 1)
            }
        }
    }
    Text("Feature icons are previews; only Qibla is currently functional.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
    Spacer(Modifier.height(14.dp))
    Text("Times are astronomical estimates. Confirm your local mosque's timetable, especially at high latitudes.", color = soft, fontSize = 12.sp)
}

@Composable
private fun QiblaScreen(latitude: Double, longitude: Double) {
    val context = LocalContext.current
    val bearing = remember(latitude, longitude) { PrayerCalculator.qiblaBearing(latitude, longitude) }
    var heading by remember { mutableFloatStateOf(0f) }
    var hasCompass by remember { mutableStateOf(false) }
    DisposableEffect(context) {
        val manager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val rotation = manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        hasCompass = rotation != null
        val listener = object : SensorEventListener {
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            override fun onSensorChanged(event: SensorEvent) {
                val matrix = FloatArray(9)
                SensorManager.getRotationMatrixFromVector(matrix, event.values)
                val orientation = FloatArray(3)
                SensorManager.getOrientation(matrix, orientation)
                heading = ((Math.toDegrees(orientation[0].toDouble()) + 360.0) % 360.0).toFloat()
            }
        }
        if (rotation != null) manager.registerListener(listener, rotation, SensorManager.SENSOR_DELAY_UI)
        onDispose { manager.unregisterListener(listener) }
    }
    Text("Qibla direction", fontSize = 28.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(8.dp))
    Text("Point your phone toward the Kaaba", color = soft)
    Spacer(Modifier.height(34.dp))
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(290.dp)) {
            val center = Offset(size.width / 2, size.height / 2)
            drawCircle(soft.copy(alpha = 0.25f), radius = size.minDimension * 0.46f, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx()))
            for (i in 0..71) {
                rotate(i * 5f, center) {
                    drawLine(if(i % 9 == 0) gold else soft.copy(alpha = 0.35f), Offset(center.x, 15.dp.toPx()), Offset(center.x, if(i % 9 == 0) 32.dp.toPx() else 24.dp.toPx()), strokeWidth = if(i % 9 == 0) 3.dp.toPx() else 1.dp.toPx(), cap = StrokeCap.Round)
                }
            }
            rotate((bearing - heading).toFloat(), center) {
                drawLine(gold, center, Offset(center.x, 43.dp.toPx()), strokeWidth = 9.dp.toPx(), cap = StrokeCap.Round)
                drawCircle(gold, 10.dp.toPx(), center)
            }
        }
        Text("N", color = gold, modifier = Modifier.align(Alignment.TopCenter).padding(top = 2.dp), fontWeight = FontWeight.Bold)
    }
    Spacer(Modifier.height(16.dp))
    Text("${bearing.roundToInt()}° from true north", color = gold, fontSize = 23.sp, fontWeight = FontWeight.Bold, modifier = Modifier)
    Spacer(Modifier.height(10.dp))
    Text(if (hasCompass) "Rotate the phone until the gold pointer faces forward. Compass sensors may report magnetic north; calibrate for best results." else "No rotation sensor detected. Use the bearing above with a true-north compass.", color = soft, textAlign = TextAlign.Center)
}

@Composable
private fun SettingsScreen(settings: Settings, updated: () -> Unit, darkMode: Boolean, onDarkModeChange: (Boolean) -> Unit) {
    val context = LocalContext.current
    var city by remember { mutableStateOf(settings.city) }
    var lat by remember { mutableStateOf(settings.latitude.toString()) }
    var lon by remember { mutableStateOf(settings.longitude.toString()) }
    var method by remember { mutableStateOf(settings.method) }
    var hanafi by remember { mutableStateOf(settings.hanafi) }
    var notify by remember { mutableStateOf(settings.notifications) }
    var message by remember { mutableStateOf("") }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        message = if (granted) "Notification permission granted" else "Enable notifications in Android settings"
    }
    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        if (grants.values.any { it }) {
            try {
                val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                val location = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER).mapNotNull { provider ->
                    if (manager.isProviderEnabled(provider)) manager.getLastKnownLocation(provider) else null
                }.maxByOrNull { it.time }
                if (location != null) { lat = location.latitude.toString(); lon = location.longitude.toString(); city = "My location"; message = "Coordinates loaded. Tap Save settings." }
                else message = "No recent location available. Enter coordinates manually."
            } catch (_: SecurityException) { message = "Location permission required" }
        } else message = "Location permission denied"
    }
    Text("Preferences", fontSize = 28.sp, fontWeight = FontWeight.Bold)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("Dark mode", modifier = Modifier.weight(1f))
        Switch(checked = darkMode, onCheckedChange = onDarkModeChange)
    }
    Spacer(Modifier.height(18.dp))
    OutlinedTextField(city, { city = it }, label = { Text("Location label") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
    Spacer(Modifier.height(10.dp))
    OutlinedTextField(lat, { lat = it }, label = { Text("Latitude (-90 to 90)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
    Spacer(Modifier.height(10.dp))
    OutlinedTextField(lon, { lon = it }, label = { Text("Longitude (-180 to 180)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
    Spacer(Modifier.height(12.dp))
    OutlinedButton(onClick = { locationPermission.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)) }) { Icon(Icons.Rounded.MyLocation, null); Spacer(Modifier.width(8.dp)); Text("Use device location") }
    Spacer(Modifier.height(20.dp))
    Text("Calculation method", color = gold)
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }) { Text(method); Icon(Icons.Rounded.ArrowDropDown, null) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            listOf("ISNA", "MWL", "Egypt", "Umm al-Qura").forEach { option -> DropdownMenuItem(text = { Text(option) }, onClick = { method = option; expanded = false }) }
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text("Hanafi Asr", modifier = Modifier.weight(1f)); Switch(hanafi, { hanafi = it })
    }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f)) { Text("Prayer alerts"); Text("System notification tone", color = soft, fontSize = 12.sp) }
        Switch(notify, { notify = it; if (it && Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) permission.launch(Manifest.permission.POST_NOTIFICATIONS) })
    }
    Spacer(Modifier.height(15.dp))
    Button(onClick = {
        val latitude = lat.toDoubleOrNull(); val longitude = lon.toDoubleOrNull()
        if (latitude == null || longitude == null || latitude !in -90.0..90.0 || longitude !in -180.0..180.0) message = "Enter valid latitude and longitude"
        else {
            settings.city = city.ifBlank { "My location" }; settings.latitude = latitude; settings.longitude = longitude
            settings.method = method; settings.hanafi = hanafi; settings.notifications = notify
            updated(); message = "Settings saved"
        }
    }, modifier = Modifier.fillMaxWidth()) { Text("Save settings") }
    if (message.isNotEmpty()) { Spacer(Modifier.height(10.dp)); Text(message, color = gold) }
    Spacer(Modifier.height(20.dp))
    Text("Prayer times use your device's timezone. If the selected coordinates are in another timezone, adjust your device timezone or use a local device. Notifications are reminders, not full Adhan recordings; Android may delay alarms when exact alarm access is unavailable.", color = soft, fontSize = 12.sp)
}

@Composable
private fun CalendarScreen(settings: Settings, refresh: Int) {
    val context = LocalContext.current
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    val prayers = remember(selectedDate, refresh) {
        PrayerCalculator.calculate(selectedDate, settings.latitude, settings.longitude, settings.method, settings.hanafi)
    }
    Text("Prayer calendar", fontSize = 28.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(8.dp))
    Text("Browse prayer times for any date", color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(20.dp))
    OutlinedButton(onClick = {
        DatePickerDialog(context, { _, year, month, day -> selectedDate = LocalDate.of(year, month + 1, day) },
            selectedDate.year, selectedDate.monthValue - 1, selectedDate.dayOfMonth).show()
    }, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Rounded.DateRange, contentDescription = null)
        Spacer(Modifier.width(10.dp))
        Text(selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")))
    }
    Spacer(Modifier.height(16.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        OutlinedButton(onClick = { selectedDate = selectedDate.minusDays(1) }) { Text("Previous") }
        OutlinedButton(onClick = { selectedDate = LocalDate.now() }) { Text("Today") }
        OutlinedButton(onClick = { selectedDate = selectedDate.plusDays(1) }) { Text("Next") }
    }
    Spacer(Modifier.height(20.dp))
    prayers.forEach { prayer ->
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(prayer.name, fontWeight = FontWeight.Medium)
            Text(prayer.time.format(DateTimeFormatter.ofPattern("h:mm a")), color = MaterialTheme.colorScheme.primary)
        }
        HorizontalDivider()
    }
    Spacer(Modifier.height(16.dp))
    Text("Calculated for ${settings.city} using your device timezone. Verify against your local mosque timetable.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
