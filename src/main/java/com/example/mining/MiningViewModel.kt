package com.example.mining

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.util.Patterns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class RouteOption(
    val points: List<LatLng>,
    val km: Double,
    val minutes: Int,
    val steps: List<String>
)

private fun distanceM(a: LatLng, b: LatLng): Float {
    val r = FloatArray(1)
    Location.distanceBetween(a.latitude, a.longitude, b.latitude, b.longitude, r)
    return r[0]
}

class MiningViewModel(app: Application) : AndroidViewModel(app) {
    private val sp = app.getSharedPreferences("saso_user", Context.MODE_PRIVATE)

    // ---- account ----
    var loggedIn by mutableStateOf(sp.getBoolean("logged_in", false))
    var userName by mutableStateOf(sp.getString("name", "") ?: "")
    var userEmail by mutableStateOf(sp.getString("email", "") ?: "")
    var userPhone by mutableStateOf(sp.getString("phone", "") ?: "")
    var userLocation by mutableStateOf<LatLng?>(null)

    /** Returns an error message, or null when registration worked. */
    fun register(name: String, email: String, phone: String): String? {
        if (name.isBlank()) return "Please enter your full name"
        if (!Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) return "Please enter a valid email"
        if (phone.isBlank()) return "Please enter your phone number"
        sp.edit()
            .putString("name", name.trim())
            .putString("email", email.trim())
            .putString("phone", phone.trim())
            .putBoolean("logged_in", false)
            .apply()
        userName = name.trim(); userEmail = email.trim(); userPhone = phone.trim()
        loggedIn = false
        return null
    }

    fun login(name: String, email: String): Boolean {
        val ok = name.isNotBlank() &&
                name.trim().equals(sp.getString("name", ""), ignoreCase = true) &&
                email.trim().equals(sp.getString("email", ""), ignoreCase = true)
        if (ok) {
            loggedIn = true
            sp.edit().putBoolean("logged_in", true).apply()
        }
        return ok
    }

    fun logout() {
        loggedIn = false
        sp.edit().putBoolean("logged_in", false).apply()
    }

    // ---- location ----
    private fun hasLocationPermission(): Boolean {
        val ctx = getApplication<Application>()
        return ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    /** One-time position read. */
    @SuppressLint("MissingPermission")
    fun fetchLocation() {
        if (!hasLocationPermission()) return
        LocationServices.getFusedLocationProviderClient(getApplication<Application>())
            .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { loc ->
                if (loc != null) userLocation = LatLng(loc.latitude, loc.longitude)
            }
    }

    private var locCallback: LocationCallback? = null

    /** Live updates: call while the Maps screen is open. Safe to call more than once. */
    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        if (locCallback != null || !hasLocationPermission()) return
        val req = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L)
            .setMinUpdateDistanceMeters(20f)
            .build()
        val cb = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val l = result.lastLocation ?: return
                val now = LatLng(l.latitude, l.longitude)
                userLocation = now
                // Auto re-route when moving along a live route
                val dest = routeTo
                val last = lastRouteFrom
                if (dest != null && startMode == "live" && !rerouting && !routeLoading &&
                    last != null && distanceM(last, now) > 150f
                ) navigateTo(dest, userInitiated = false)
            }
        }
        locCallback = cb
        LocationServices.getFusedLocationProviderClient(getApplication<Application>())
            .requestLocationUpdates(req, cb, Looper.getMainLooper())
    }

    fun stopLocationUpdates() {
        locCallback?.let {
            LocationServices.getFusedLocationProviderClient(getApplication<Application>())
                .removeLocationUpdates(it)
        }
        locCallback = null
    }

    override fun onCleared() {
        stopLocationUpdates()
        super.onCleared()
    }

    // ---- market + map ----
    var category by mutableStateOf("All")
    var mapFilter by mutableStateOf("All")
    var selectedPlace by mutableStateOf<Place?>(null)

    // ---- start point ----
    var startMode by mutableStateOf("live")            // "live" or "custom"
    var customStart by mutableStateOf<LatLng?>(null)

    fun startPoint(): LatLng =
        if (startMode == "custom" && customStart != null) customStart!!
        else userLocation ?: MY_POS

    fun useLiveStart() {
        startMode = "live"
        routeTo?.let { navigateTo(it) }
    }

    fun chooseStartMode() { startMode = "custom" }

    fun pickCustomStart(p: LatLng) {
        customStart = p
        startMode = "custom"
        routeTo?.let { navigateTo(it) }
    }

    // ---- routes ----
    var routeTo by mutableStateOf<Place?>(null)
    var routes by mutableStateOf<List<RouteOption>>(emptyList())
    var selectedRoute by mutableIntStateOf(0)
    var routeLoading by mutableStateOf(false)
    var routeError by mutableStateOf<String?>(null)
    var zoomTick by mutableIntStateOf(0)               // bumps when the map should zoom to the route

    private var lastRouteFrom: LatLng? = null
    private var rerouting = false

    fun clearRoute() {
        routeTo = null; routes = emptyList(); selectedRoute = 0
        routeLoading = false; routeError = null; lastRouteFrom = null
    }

    fun navigateTo(p: Place, userInitiated: Boolean = true) {
        val from = startPoint()
        routeTo = p
        lastRouteFrom = from
        routeError = null
        if (userInitiated) {
            routeLoading = true
            routes = emptyList()
            selectedRoute = 0
        } else rerouting = true

        viewModelScope.launch {
            try {
                val list = withContext(Dispatchers.IO) { fetchRoutes(from, p.pos) }
                routes = list
                if (selectedRoute >= list.size) selectedRoute = 0
                if (userInitiated) zoomTick++
            } catch (e: Exception) {
                if (routes.isEmpty()) {
                    routes = listOf(
                        RouteOption(listOf(from, p.pos), distanceM(from, p.pos) / 1000.0, 0, emptyList())
                    )
                    routeError = "Couldn't get road routes (check internet). Showing a straight line."
                    if (userInitiated) zoomTick++
                }
            }
            routeLoading = false
            rerouting = false
        }
    }

    private fun fetchRoutes(a: LatLng, b: LatLng): List<RouteOption> {
        val url = URL(
            "https://router.project-osrm.org/route/v1/driving/" +
                    "${a.longitude},${a.latitude};${b.longitude},${b.latitude}" +
                    "?overview=full&geometries=geojson&steps=true&alternatives=true"
        )
        val conn = url.openConnection() as HttpURLConnection
        conn.setRequestProperty("User-Agent", "SasoMiningApp/1.0 (your_real_email@example.com)")
        conn.connectTimeout = 10000
        conn.readTimeout = 15000
        val text = conn.inputStream.bufferedReader().use { it.readText() }
        val arr = JSONObject(text).getJSONArray("routes")
        return (0 until arr.length()).map { parseRoute(arr.getJSONObject(it)) }
    }

    private fun parseRoute(route: JSONObject): RouteOption {
        val coords = route.getJSONObject("geometry").getJSONArray("coordinates")
        val pts = (0 until coords.length()).map {
            val c = coords.getJSONArray(it)
            LatLng(c.getDouble(1), c.getDouble(0))
        }
        val steps = mutableListOf<String>()
        val legs = route.getJSONArray("legs")
        for (l in 0 until legs.length()) {
            val st = legs.getJSONObject(l).getJSONArray("steps")
            for (s in 0 until st.length()) {
                val step = st.getJSONObject(s)
                val man = step.getJSONObject("maneuver")
                val type = man.optString("type")
                val mod = man.optString("modifier")
                val name = step.optString("name")
                val dist = step.getDouble("distance")
                if (type == "arrive") { steps.add("You have arrived"); continue }
                val action = when {
                    type == "depart" -> "Head out"
                    type == "roundabout" || type == "rotary" -> "Take the roundabout"
                    mod == "straight" || mod.isEmpty() -> "Continue straight"
                    else -> "Turn $mod"
                }
                val road = if (name.isNotBlank()) " onto $name" else ""
                val d = if (dist >= 1000) "%.1f km".format(dist / 1000) else "${dist.toInt()} m"
                steps.add("$action$road ($d)")
            }
        }
        return RouteOption(
            points = pts,
            km = route.getDouble("distance") / 1000.0,
            minutes = (route.getDouble("duration") / 60.0).toInt().coerceAtLeast(1),
            steps = steps
        )
    }

    // ---- chat ----
    val chats = mutableStateMapOf<Int, List<Msg>>()
    val unread = mutableStateMapOf<Int, Boolean>()
    var openThread by mutableStateOf<Int?>(null)

    private val auto = listOf(
        "Hello! Yes, it's still available.",
        "I can show it to you this week in Gwangju.",
        "Price is slightly negotiable for a quick sale."
    )

    fun send(id: Int, text: String) {
        if (text.isBlank()) return
        chats[id] = (chats[id] ?: emptyList()) + Msg(true, text.trim())
        viewModelScope.launch {
            delay(900)
            val n = chats[id].orEmpty().count { !it.mine }
            chats[id] = chats[id].orEmpty() + Msg(false, auto[minOf(n, auto.size - 1)])
            if (openThread != id) unread[id] = true
        }
    }
}