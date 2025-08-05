package com.example.trackearubicaciones

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import java.util.concurrent.atomic.AtomicBoolean

class MapaColaborativoActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var database: DatabaseReference
    private var sessionId: String = ""
    private val markersMap = mutableMapOf<String, Marker>()
    private val puntosMap = mutableMapOf<String, Marker>()
    private var userId: String? = null
    private lateinit var fabAgregarPunto: FloatingActionButton
    private lateinit var fabFinalizarSesion: FloatingActionButton
    private lateinit var fabCalcularArea: FloatingActionButton
    private lateinit var fabGuardarTerreno: FloatingActionButton
    private var polygon: Polygon? = null
    private var marcadorUsuarioActual: Marker? = null
    private var isFirstLocationUpdate = true
    private lateinit var tvSessionInfo: TextView
    private var isCreador: Boolean = false
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var lastLocation: GeoPoint? = null
    private var isActivityActive = AtomicBoolean(false) // Bandera para controlar actualizaciones de UI
    private val locationUpdateHandler = Handler(Looper.getMainLooper())
    private var lastFirebaseUpdateTime = 0L
    private val FIREBASE_UPDATE_INTERVAL = 2000L // 2 segundos para actualizaciones a Firebase

    companion object {
        private const val REQUEST_PERMISSIONS_REQUEST_CODE = 1
        private const val MAX_ACCURACY_THRESHOLD = 50.0f // Metros
    }

    private val locationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (!isActivityActive.get()) return // Evitar actualizaciones si la actividad no está activa
            val lat = intent?.getDoubleExtra("latitude", 0.0) ?: return
            val lon = intent?.getDoubleExtra("longitude", 0.0) ?: return
            Log.d("MapaColaborativoActivity", "Recibido broadcast: lat=$lat, lon=$lon")

            userId?.let {
                val refUser = database.child("usuariosConectados").child(it)
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastFirebaseUpdateTime >= FIREBASE_UPDATE_INTERVAL) {
                    val datos = mapOf("latitud" to lat, "longitud" to lon)
                    refUser.setValue(datos).addOnFailureListener { e ->
                        Log.e("MapaColaborativoActivity", "Error al actualizar ubicación en Firebase: ${e.message}")
                        Toast.makeText(this@MapaColaborativoActivity, "Error al actualizar ubicación", Toast.LENGTH_SHORT).show()
                    }.addOnSuccessListener {
                        lastFirebaseUpdateTime = currentTime
                        Log.d("MapaColaborativoActivity", "Ubicación enviada a Firebase: lat=$lat, lon=$lon")
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(applicationContext, androidx.preference.PreferenceManager.getDefaultSharedPreferences(applicationContext))
        setContentView(R.layout.activity_mapa_colaborativo)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        mapView = findViewById(R.id.mapView)
        tvSessionInfo = findViewById(R.id.tvSessionInfo)
        fabAgregarPunto = findViewById(R.id.fabAgregarPunto)
        fabFinalizarSesion = findViewById(R.id.fabFinalizarSesion)
        fabCalcularArea = findViewById(R.id.fabCalcularArea)
        fabGuardarTerreno = findViewById(R.id.fabGuardarTerreno)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setupMap()

        // Validar sessionId y userId antes de continuar
        sessionId = intent.getStringExtra("sesionId") ?: ""
        if (sessionId.isEmpty()) {
            Toast.makeText(this, "ID de sesión inválido", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        database = FirebaseDatabase.getInstance().getReference("sesiones").child(sessionId)
        userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "Usuario no autenticado, por favor inicia sesión", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Iniciar el servicio solo si no está corriendo
        if (!isServiceRunning(LocationForegroundService::class.java)) {
            LocationForegroundService.startService(this, sessionId, userId!!)
        }

        // Configurar LocationCallback para actualizaciones locales más frecuentes
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                if (!isActivityActive.get()) return // Evitar actualizaciones si la actividad no está activa
                locationResult.lastLocation?.let { location ->
                    if (location.hasAccuracy() && location.accuracy > MAX_ACCURACY_THRESHOLD) {
                        Log.d("MapaColaborativoActivity", "Ubicación descartada por baja precisión: ${location.accuracy}")
                        return@let
                    }
                    val newGeoPoint = GeoPoint(location.latitude, location.longitude)
                    runOnUiThread {
                        if (marcadorUsuarioActual == null) {
                            marcadorUsuarioActual = Marker(mapView).apply {
                                position = newGeoPoint
                                title = "Tú"
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                icon = ContextCompat.getDrawable(this@MapaColaborativoActivity, org.osmdroid.library.R.drawable.marker_default)?.apply {
                                    setTint(0xFF0000FF.toInt())
                                }
                                mapView.overlays.add(this)
                            }
                        } else {
                            marcadorUsuarioActual?.position = newGeoPoint
                        }

                        if (isFirstLocationUpdate) {
                            mapView.controller.setZoom(15.0)
                            mapView.controller.setCenter(newGeoPoint)
                            isFirstLocationUpdate = false
                        }
                        mapView.invalidate() // Renderizar solo después de actualizar el marcador local
                        Log.d("MapaColaborativoActivity", "Ubicación actualizada (local): lat=${location.latitude}, lon=${location.longitude}, accuracy=${location.accuracy}")
                    }
                    lastLocation = newGeoPoint
                }
            }
        }

        cargarInfoSesion()
        verificarCreador()
        requestPermissionsIfNecessary()
        escucharUsuariosYPosiciones()
        escucharPuntosArea()

        fabAgregarPunto.setOnClickListener { agregarPuntoActual() }
        fabFinalizarSesion.setOnClickListener { finalizarSesion() }
        fabCalcularArea.setOnClickListener { calcularArea() }
        fabGuardarTerreno.setOnClickListener { guardarTerreno() }
    }

    private fun setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(15.0)
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.create().apply {
            interval = 1000 // 1 segundo para actualizaciones más fluidas
            fastestInterval = 500 // 500 ms para el intervalo más rápido
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, mainLooper)
                .addOnFailureListener { e ->
                    Log.e("MapaColaborativoActivity", "Error al iniciar actualizaciones de ubicación: ${e.message}")
                    Toast.makeText(this, "Error al iniciar ubicación", Toast.LENGTH_SHORT).show()
                }
        } else {
            Log.e("MapaColaborativoActivity", "Permiso ACCESS_FINE_LOCATION no otorgado")
            requestPermissionsIfNecessary()
        }
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun cargarInfoSesion() {
        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val nombre = snapshot.child("nombre").getValue(String::class.java) ?: "Sesión sin nombre"
                val codigo = snapshot.child("codigo").getValue(String::class.java) ?: "Sin código"
                tvSessionInfo.text = "Sesión: $nombre | Código: $codigo"
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("MapaColaborativoActivity", "Error al cargar info de sesión: ${error.message}")
                Toast.makeText(this@MapaColaborativoActivity, "Error al cargar info de sesión", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun verificarCreador() {
        database.child("creadorUid").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                isCreador = snapshot.getValue(String::class.java) == userId
                fabGuardarTerreno.visibility = if (isCreador) View.VISIBLE else View.GONE
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("MapaColaborativoActivity", "Error al verificar creador: ${error.message}")
                Toast.makeText(this@MapaColaborativoActivity, "Error al verificar creador", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun requestPermissionsIfNecessary() {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), REQUEST_PERMISSIONS_REQUEST_CODE)
        } else {
            startLocationUpdates()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                startLocationUpdates()
            } else {
                Toast.makeText(this, "Permisos de ubicación necesarios para continuar", Toast.LENGTH_LONG).show()
                requestPermissionsIfNecessary() // Reintentar en lugar de finalizar
            }
        }
    }

    private fun escucharUsuariosYPosiciones() {
        val usuariosConectadosRef = database.child("usuariosConectados")
        usuariosConectadosRef.keepSynced(true)

        usuariosConectadosRef.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                actualizarMarcadorDesdeSnapshot(snapshot)
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                actualizarMarcadorDesdeSnapshot(snapshot)
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                val uid = snapshot.key ?: return
                runOnUiThread {
                    removerMarcador(uid)
                    mapView.invalidate() // Renderizar solo después de remover
                }
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {
                Log.e("MapaColaborativoActivity", "Error en usuariosConectados: ${error.message}")
                Toast.makeText(this@MapaColaborativoActivity, "Error al cargar usuarios conectados", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun actualizarMarcadorDesdeSnapshot(snapshot: DataSnapshot) {
        val uid = snapshot.key ?: return
        val lat = snapshot.child("latitud").getValue(Double::class.java) ?: return
        val lon = snapshot.child("longitud").getValue(Double::class.java) ?: return
        Log.d("MapaColaborativoActivity", "Actualizando marcador de usuario $uid: lat=$lat, lon=$lon")

        if (uid != userId) {
            runOnUiThread {
                agregarOModificarMarcador(uid, lat, lon)
                mapView.invalidate() // Renderizar solo después de actualizar
            }
        }
    }

    private fun agregarOModificarMarcador(uid: String, lat: Double, lon: Double) {
        val geoPoint = GeoPoint(lat, lon)
        val marker = markersMap[uid]
        if (marker == null) {
            val newMarker = Marker(mapView).apply {
                position = geoPoint
                title = "Usuario: $uid"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                icon = ContextCompat.getDrawable(this@MapaColaborativoActivity, org.osmdroid.library.R.drawable.marker_default)?.apply {
                    setTint(0xFF0000FF.toInt())
                }
                mapView.overlays.add(this)
            }
            markersMap[uid] = newMarker
        } else {
            marker.position = geoPoint
        }
    }

    private fun removerMarcador(uid: String) {
        markersMap[uid]?.let {
            mapView.overlays.remove(it)
            markersMap.remove(uid)
        }
    }

    private fun escucharPuntosArea() {
        val puntosRef = database.child("puntos")
        puntosRef.keepSynced(true)

        puntosRef.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                agregarOModificarPunto(snapshot)
                actualizarPoligono()
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                agregarOModificarPunto(snapshot)
                actualizarPoligono()
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                val id = snapshot.key ?: return
                runOnUiThread {
                    removerPunto(id)
                    actualizarPoligono()
                }
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {
                Log.e("MapaColaborativoActivity", "Error en puntos: ${error.message}")
                Toast.makeText(this@MapaColaborativoActivity, "Error al cargar puntos", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun agregarOModificarPunto(snapshot: DataSnapshot) {
        val id = snapshot.key ?: return
        val lat = snapshot.child("latitud").getValue(Double::class.java) ?: return
        val lon = snapshot.child("longitud").getValue(Double::class.java) ?: return

        runOnUiThread {
            val geoPoint = GeoPoint(lat, lon)
            val marker = puntosMap[id]
            if (marker == null) {
                val newMarker = Marker(mapView).apply {
                    position = geoPoint
                    title = "Punto: $id"
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    isDraggable = esUsuarioOcreador(id)
                    icon = ContextCompat.getDrawable(this@MapaColaborativoActivity, org.osmdroid.library.R.drawable.marker_default)?.apply {
                        setTint(0xFFFF0000.toInt())
                    }
                    setOnMarkerDragListener(object : Marker.OnMarkerDragListener {
                        override fun onMarkerDragStart(marker: Marker?) {}
                        override fun onMarkerDrag(marker: Marker?) {}
                        override fun onMarkerDragEnd(marker: Marker?) {
                            marker?.let {
                                actualizarPuntoEnFirebase(id, it.position.latitude, it.position.longitude)
                                actualizarPoligono()
                            }
                        }
                    })
                    setOnMarkerClickListener { _, _ ->
                        if (esMiPunto(id)) {
                            database.child("puntos").child(id).removeValue()
                            Toast.makeText(this@MapaColaborativoActivity, "Punto eliminado", Toast.LENGTH_SHORT).show()
                            true
                        } else false
                    }
                }
                mapView.overlays.add(newMarker)
                puntosMap[id] = newMarker
            } else {
                marker.position = geoPoint
            }
            mapView.invalidate()
        }
    }

    private fun removerPunto(id: String) {
        puntosMap[id]?.let {
            mapView.overlays.remove(it)
            puntosMap.remove(id)
        }
    }

    private fun actualizarPuntoEnFirebase(id: String, lat: Double, lon: Double) {
        database.child("puntos").child(id).setValue(mapOf("latitud" to lat, "longitud" to lon))
            .addOnFailureListener { e ->
                Log.e("MapaColaborativoActivity", "Error al actualizar punto: ${e.message}")
                Toast.makeText(this@MapaColaborativoActivity, "Error al actualizar punto", Toast.LENGTH_SHORT).show()
            }
    }

    private fun esUsuarioOcreador(idPunto: String): Boolean {
        return esMiPunto(idPunto) || isCreador
    }

    private fun esMiPunto(idPunto: String): Boolean {
        return idPunto == userId
    }

    private fun agregarPuntoActual() {
        val lastMarker = marcadorUsuarioActual
        if (lastMarker == null || userId == null) {
            Toast.makeText(this, "Ubicación no disponible aún", Toast.LENGTH_SHORT).show()
            return
        }

        val yaTienePunto = puntosMap.keys.any { esMiPunto(it) }
        if (yaTienePunto) {
            Toast.makeText(this, "Ya tienes un punto colocado", Toast.LENGTH_SHORT).show()
            return
        }

        val nuevoId = userId!!
        val lat = lastMarker.position.latitude
        val lon = lastMarker.position.longitude
        val datos = mapOf("latitud" to lat, "longitud" to lon)
        database.child("puntos").child(nuevoId).setValue(datos)
            .addOnSuccessListener {
                Toast.makeText(this, "Punto agregado", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e("MapaColaborativoActivity", "Error al agregar punto: ${e.message}")
                Toast.makeText(this, "Error al agregar punto", Toast.LENGTH_SHORT).show()
            }
    }

    private fun guardarTerreno() {
        if (!isCreador) {
            Toast.makeText(this, "Solo el creador puede guardar el terreno", Toast.LENGTH_SHORT).show()
            return
        }

        val points = puntosMap.values.map { it.position }
        if (points.size < 3) {
            Toast.makeText(this, "Se necesitan al menos 3 puntos para guardar el terreno", Toast.LENGTH_SHORT).show()
            return
        }

        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val nombre = snapshot.child("nombre").getValue(String::class.java) ?: "Terreno sin nombre"
                val nombreCreador = snapshot.child("nombreCreador").getValue(String::class.java) ?: "Desconocido"
                val codigo = snapshot.child("codigo").getValue(String::class.java) ?: "Sin código"
                val area = calcularAreaPoligono(points)

                val terrenosRef = FirebaseDatabase.getInstance().getReference("terrenos")
                val terrenoId = terrenosRef.push().key ?: return
                val puntosData = points.mapIndexed { index, point ->
                    "punto$index" to mapOf("latitud" to point.latitude, "longitud" to point.longitude)
                }.toMap()

                val terrenoData = mapOf(
                    "nombre" to nombre,
                    "area" to area,
                    "puntos" to puntosData,
                    "creadorUid" to userId,
                    "nombreCreador" to nombreCreador,
                    "timestamp" to ServerValue.TIMESTAMP,
                    "esCompartido" to true,
                    "sessionId" to sessionId,
                    "codigo" to codigo
                )

                terrenosRef.child(terrenoId).setValue(terrenoData)
                    .addOnSuccessListener {
                        Toast.makeText(this@MapaColaborativoActivity, "Terreno guardado con éxito", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Log.e("MapaColaborativoActivity", "Error al guardar terreno: ${e.message}")
                        Toast.makeText(this@MapaColaborativoActivity, "Error al guardar terreno", Toast.LENGTH_SHORT).show()
                    }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("MapaColaborativoActivity", "Error al leer sesión: ${error.message}")
                Toast.makeText(this@MapaColaborativoActivity, "Error al leer datos de la sesión", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun finalizarSesion() {
        userId?.let {
            database.child("usuariosConectados").child(it).removeValue()
                .addOnFailureListener { e ->
                    Log.e("MapaColaborativoActivity", "Error al finalizar sesión: ${e.message}")
                    Toast.makeText(this@MapaColaborativoActivity, "Error al finalizar sesión", Toast.LENGTH_SHORT).show()
                }
        }
        stopService(Intent(this, LocationForegroundService::class.java))
        stopLocationUpdates()
        Toast.makeText(this, "Sesión finalizada", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun actualizarPoligono() {
        polygon?.let { mapView.overlays.remove(it) }
        polygon = null

        if (puntosMap.size < 3) {
            mapView.invalidate()
            return
        }

        val points = puntosMap.values.map { it.position }
        polygon = Polygon().apply {
            fillPaint.color = 0x4D009688 // Verde con 30% opacidad
            outlinePaint.color = 0xFF009688.toInt() // Verde sólido
            outlinePaint.strokeWidth = 5f
            setPoints(points)
        }
        mapView.overlays.add(polygon)
        mapView.invalidate()
    }

    private fun calcularArea() {
        val points = puntosMap.values.map { it.position }
        if (points.size < 3) {
            Toast.makeText(this, "Se necesitan al menos 3 puntos para calcular el área", Toast.LENGTH_SHORT).show()
            return
        }

        val area = calcularAreaPoligono(points)
        Toast.makeText(this, "Área calculada: ${"%.2f".format(area)} m²", Toast.LENGTH_LONG).show()
    }

    private fun calcularAreaPoligono(points: List<GeoPoint>): Double {
        if (points.size < 3) return 0.0

        var area = 0.0
        val n = points.size
        val latPromedio = points.map { it.latitude }.average() * Math.PI / 180
        val escalaLongitud = 111_139.0 * Math.cos(latPromedio)
        val escalaLatitud = 111_139.0

        for (i in 0 until n) {
            val j = (i + 1) % n
            area += points[i].longitude * points[j].latitude
            area -= points[j].longitude * points[i].latitude
        }
        area = kotlin.math.abs(area) / 2.0
        return area * escalaLongitud * escalaLatitud
    }

    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        return manager.getRunningServices(Integer.MAX_VALUE).any { it.service.className == serviceClass.name }
    }

    override fun onStart() {
        super.onStart()
        isActivityActive.set(true)
        val filter = IntentFilter("LOCATION_UPDATE")
        LocalBroadcastManager.getInstance(this).registerReceiver(locationReceiver, filter)
        startLocationUpdates()
        // En MapaColaborativoActivity, dentro de onCreate o onStart
         val locationReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (!isActivityActive.get()) return
                when (intent?.action) {
                    "LOCATION_UPDATE" -> {
                        val lat = intent.getDoubleExtra("latitude", 0.0)
                        val lon = intent.getDoubleExtra("longitude", 0.0)
                        Log.d("MapaColaborativoActivity", "Recibido broadcast: lat=$lat, lon=$lon")

                        userId?.let {
                            val refUser = database.child("usuariosConectados").child(it)
                            val currentTime = System.currentTimeMillis()
                            if (currentTime - lastFirebaseUpdateTime >= FIREBASE_UPDATE_INTERVAL) {
                                val datos = mapOf("latitud" to lat, "longitud" to lon)
                                refUser.setValue(datos).addOnFailureListener { e ->
                                    Log.e("MapaColaborativoActivity", "Error al actualizar ubicación en Firebase: ${e.message}")
                                    Toast.makeText(this@MapaColaborativoActivity, "Error al actualizar ubicación", Toast.LENGTH_SHORT).show()
                                }.addOnSuccessListener {
                                    lastFirebaseUpdateTime = currentTime
                                    Log.d("MapaColaborativoActivity", "Ubicación enviada a Firebase: lat=$lat, lon=$lon")
                                }
                            }
                        }
                    }
                    "LOCATION_ERROR" -> {
                        val error = intent.getStringExtra("error") ?: "Error desconocido"
                        Toast.makeText(this@MapaColaborativoActivity, error, Toast.LENGTH_LONG).show()
                        requestPermissionsIfNecessary() // Reintentar permisos si es necesario
                    }
                }
            }
        }


    }

    override fun onStop() {
        super.onStop()
        isActivityActive.set(false)
        stopLocationUpdates()
        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(locationReceiver)
        } catch (e: IllegalArgumentException) {
            Log.e("MapaColaborativoActivity", "Error al desregistrar receiver: ${e.message}")
        }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Limpiar recursos
        markersMap.clear()
        puntosMap.clear()
        polygon?.let { mapView.overlays.remove(it) }
        mapView.overlays.clear()
        locationUpdateHandler.removeCallbacksAndMessages(null)
        if (isActivityActive.get()) {
            stopService(Intent(this, LocationForegroundService::class.java))
            userId?.let { uid ->
                database.child("usuariosConectados").child(uid).removeValue()
                    .addOnFailureListener { e ->
                        Log.e("MapaColaborativoActivity", "Error al remover usuario en onDestroy: ${e.message}")
                    }
            }
        }
    }
}