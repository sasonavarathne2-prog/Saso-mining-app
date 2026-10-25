package com.example.mining

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import org.json.JSONArray
import org.json.JSONObject
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.layers.PropertyFactory.lineCap
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.lineJoin
import org.maplibre.android.style.layers.PropertyFactory.lineWidth
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.android.geometry.LatLng as MLLatLng

// Free vector map style. No account or API key needed.
private const val STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"

private fun ml(p: LatLng) = MLLatLng(p.latitude, p.longitude)

// ---------- small GeoJSON helpers ----------
private fun pointFeature(p: LatLng, props: JSONObject = JSONObject()): JSONObject =
    JSONObject()
        .put("type", "Feature")
        .put("properties", props)
        .put(
            "geometry",
            JSONObject().put("type", "Point")
                .put("coordinates", JSONArray().put(p.longitude).put(p.latitude))
        )

private fun lineFeature(points: List<LatLng>): JSONObject {
    val coords = JSONArray()
    points.forEach { coords.put(JSONArray().put(it.longitude).put(it.latitude)) }
    return JSONObject()
        .put("type", "Feature")
        .put("properties", JSONObject())
        .put("geometry", JSONObject().put("type", "LineString").put("coordinates", coords))
}

private fun collection(features: List<JSONObject>): String =
    JSONObject()
        .put("type", "FeatureCollection")
        .put("features", JSONArray(features))
        .toString()

private const val EMPTY = """{"type":"FeatureCollection","features":[]}"""

// ---------- map layers ----------
private fun addLayers(st: Style) {
    st.addSource(GeoJsonSource("alt-src"))
    st.addSource(GeoJsonSource("main-src"))
    st.addSource(GeoJsonSource("places-src"))
    st.addSource(GeoJsonSource("start-src"))
    st.addSource(GeoJsonSource("me-src"))

    st.addLayer(
        LineLayer("alt-layer", "alt-src").withProperties(
            lineColor("#9AA5A6"), lineWidth(4f),
            lineCap(Property.LINE_CAP_ROUND), lineJoin(Property.LINE_JOIN_ROUND)
        )
    )
    st.addLayer(
        LineLayer("main-layer", "main-src").withProperties(
            lineColor("#2B6FE0"), lineWidth(6f),
            lineCap(Property.LINE_CAP_ROUND), lineJoin(Property.LINE_JOIN_ROUND)
        )
    )
    st.addLayer(
        CircleLayer("places-layer", "places-src").withProperties(
            circleColor(Expression.toColor(Expression.get("color"))),
            circleRadius(11f),
            circleStrokeColor("#FFFFFF"),
            circleStrokeWidth(3f)
        )
    )
    st.addLayer(
        CircleLayer("start-layer", "start-src").withProperties(
            circleColor("#8E3FC9"), circleRadius(10f),
            circleStrokeColor("#FFFFFF"), circleStrokeWidth(3f)
        )
    )
    st.addLayer(
        CircleLayer("me-layer", "me-src").withProperties(
            circleColor("#2B6FE0"), circleRadius(9f),
            circleStrokeColor("#FFFFFF"), circleStrokeWidth(3f)
        )
    )
    // Fill all sources with empty data so the map is valid from the start
    listOf("alt-src", "main-src", "places-src", "start-src", "me-src").forEach {
        st.getSourceAs<GeoJsonSource>(it)?.setGeoJson(EMPTY)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(vm: MiningViewModel) {
    val ctx = LocalContext.current
    val me = vm.userLocation ?: MY_POS
    var centered by remember { mutableStateOf(false) }
    var mlMap by remember { mutableStateOf<MapLibreMap?>(null) }
    var style by remember { mutableStateOf<Style?>(null) }

    val mapView = remember {
        MapLibre.getInstance(ctx)
        MapView(ctx).also { it.onCreate(null) }
    }

    DisposableEffect(mapView) {
        mapView.onStart()
        mapView.onResume()
        vm.startLocationUpdates()

        mapView.getMapAsync { map ->
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(ml(MY_POS), 10.5))
            map.setStyle(STYLE_URL) { st ->
                addLayers(st)
                style = st
            }

            // Tap a place pin
            map.addOnMapClickListener { point ->
                val px = map.projection.toScreenLocation(point)
                val hit = map.queryRenderedFeatures(px, "places-layer")
                val id = hit.firstOrNull()?.getNumberProperty("id")?.toInt()
                val place = PLACES.firstOrNull { it.id == id }
                if (place != null) {
                    vm.selectedPlace = place
                    vm.clearRoute()
                    true
                } else false
            }

            // Long-press anywhere to choose a custom start point
            map.addOnMapLongClickListener { p ->
                vm.pickCustomStart(LatLng(p.latitude, p.longitude))
                true
            }
            mlMap = map
        }

        onDispose {
            vm.stopLocationUpdates()
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    // Centre on the user the first time a real GPS fix arrives
    LaunchedEffect(vm.userLocation, mlMap) {
        val loc = vm.userLocation
        val map = mlMap
        if (loc != null && map != null && !centered) {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(ml(loc), 12.0), 800)
            centered = true
        }
    }

    // Zoom to the route when it is first calculated
    LaunchedEffect(vm.zoomTick, mlMap) {
        val map = mlMap
        val pts = vm.routes.getOrNull(vm.selectedRoute)?.points.orEmpty()
        if (map != null && pts.size > 1) {
            val bounds = LatLngBounds.Builder().includes(pts.map { ml(it) }).build()
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 120), 800)
        }
    }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { r ->
        if (r.values.any { it }) {
            vm.fetchLocation()
            vm.startLocationUpdates()
        }
    }

    Column(Modifier.fillMaxSize()) {
        Text("Maps", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp))
        Row(
            Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf("All", "Mines", "Markets").forEach { f ->
                FilterChip(
                    selected = vm.mapFilter == f,
                    onClick = { vm.mapFilter = f; vm.selectedPlace = null; vm.clearRoute() },
                    label = { Text(f) }
                )
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = {
                vm.userLocation?.let {
                    mlMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(ml(it), 13.0), 600)
                }
                permLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }) { Text("My location") }
        }

        AndroidView(
            modifier = Modifier.weight(1f).padding(top = 8.dp),
            factory = { mapView },
            update = { _ ->
                val st = style
                if (st != null) {
                    // Routes: grey alternatives, blue selected route
                    val alts = vm.routes.filterIndexed { i, r -> i != vm.selectedRoute && r.points.size > 1 }
                        .map { lineFeature(it.points) }
                    st.getSourceAs<GeoJsonSource>("alt-src")?.setGeoJson(collection(alts))

                    val main = vm.routes.getOrNull(vm.selectedRoute)
                        ?.takeIf { it.points.size > 1 }
                        ?.let { listOf(lineFeature(it.points)) } ?: emptyList()
                    st.getSourceAs<GeoJsonSource>("main-src")?.setGeoJson(collection(main))

                    // Places (orange = mines, green = markets)
                    val places = PLACES
                        .filter { vm.mapFilter == "All" || it.kind == vm.mapFilter }
                        .map {
                            pointFeature(
                                it.pos,
                                JSONObject()
                                    .put("id", it.id)
                                    .put("color", if (it.kind == "Mines") "#E07A1F" else "#2A8A3E")
                            )
                        }
                    st.getSourceAs<GeoJsonSource>("places-src")?.setGeoJson(collection(places))

                    // Custom start (purple)
                    val start = if (vm.startMode == "custom") vm.customStart else null
                    st.getSourceAs<GeoJsonSource>("start-src")
                        ?.setGeoJson(collection(listOfNotNull(start?.let { pointFeature(it) })))

                    // You (blue)
                    st.getSourceAs<GeoJsonSource>("me-src")
                        ?.setGeoJson(collection(listOf(pointFeature(me))))
                }
            }
        )

        vm.selectedPlace?.let { p ->
            Card(Modifier.fillMaxWidth().padding(12.dp)) {
                Column(
                    Modifier
                        .padding(12.dp)
                        .heightIn(max = 280.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(p.name, fontWeight = FontWeight.Bold)
                    Text(p.kind, color = Color.Gray)
                    Spacer(Modifier.height(8.dp))

                    Text("Start from", fontSize = 12.sp, color = Color.Gray)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = vm.startMode == "live",
                            onClick = { vm.useLiveStart() },
                            label = { Text("My location (live)") }
                        )
                        FilterChip(
                            selected = vm.startMode == "custom",
                            onClick = { vm.chooseStartMode() },
                            label = { Text("Choose start") }
                        )
                    }
                    if (vm.startMode == "custom") {
                        Text(
                            if (vm.customStart == null) "Long-press the map to pick a start point."
                            else "Custom start set. Long-press the map to change it.",
                            fontSize = 12.sp, color = Color.Gray
                        )
                    }
                    Spacer(Modifier.height(8.dp))

                    when {
                        vm.routeLoading -> Text("Finding the best routes…")
                        vm.routes.isNotEmpty() -> {
                            Row(
                                Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                vm.routes.forEachIndexed { i, r ->
                                    val name = if (i == 0) "Best" else "Option ${i + 1}"
                                    val time = if (r.minutes > 0) " · ${r.minutes} min" else ""
                                    FilterChip(
                                        selected = vm.selectedRoute == i,
                                        onClick = { vm.selectedRoute = i },
                                        label = { Text("$name · %.1f km$time".format(r.km)) }
                                    )
                                }
                            }
                            vm.routeError?.let {
                                Text(it, color = Color(0xFFB3261E), fontSize = 12.sp)
                            }
                            val sel = vm.routes.getOrNull(vm.selectedRoute)
                            if (sel != null && sel.steps.isNotEmpty()) {
                                Spacer(Modifier.height(6.dp))
                                sel.steps.forEachIndexed { i, s ->
                                    Text("${i + 1}. $s", fontSize = 13.sp)
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(onClick = { vm.clearRoute() }, modifier = Modifier.fillMaxWidth()) {
                                Text("Close route")
                            }
                        }
                        else -> Button(onClick = { vm.navigateTo(p) }, modifier = Modifier.fillMaxWidth()) {
                            Text("Navigate")
                        }
                    }
                }
            }
        }
    }
}