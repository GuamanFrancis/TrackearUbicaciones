package com.example.trackearubicaciones

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.trackearubicaciones.databinding.ActivityRegistrarTerrenoBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class RegistrarTerrenoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegistrarTerrenoBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var map: MapView
    private var currentLocation: Location? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance().load(applicationContext, getSharedPreferences("osmdroid", MODE_PRIVATE))

        binding = ActivityRegistrarTerrenoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        map = binding.map

        setupMap()

        binding.btnObtenerUbicacion.setOnClickListener {
            requestLocation()
        }

        binding.btnGuardarTerreno.setOnClickListener {
            guardarTerreno()
        }
    }

    private fun setupMap() {
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
        map.controller.setZoom(15.0)
    }

    private fun requestLocation() {
        val permisoUbicacion = Manifest.permission.ACCESS_FINE_LOCATION

        if (ActivityCompat.checkSelfPermission(this, permisoUbicacion) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(permisoUbicacion)
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                currentLocation = location
                val geoPoint = GeoPoint(location.latitude, location.longitude)
                map.controller.setCenter(geoPoint)

                val marker = Marker(map)
                marker.position = geoPoint
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                marker.title = "Ubicación actual"
                map.overlays.clear()
                map.overlays.add(marker)
                map.invalidate()

                binding.tvUbicacion.text = "Ubicación: ${location.latitude}, ${location.longitude}"
            } else {
                Toast.makeText(this, "Ubicación no disponible", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            requestLocation()
        } else {
            Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show()
        }
    }

    private fun guardarTerreno() {
        val nombre = binding.edtNombreTerreno.text.toString().trim()
        val descripcion = binding.edtDescripcion.text.toString().trim()
        val ubicacion = currentLocation

        if (nombre.isEmpty() || descripcion.isEmpty() || ubicacion == null) {
            Toast.makeText(this, "Completa todos los campos y obtén la ubicación", Toast.LENGTH_SHORT).show()
            return
        }

        val terrenoId = FirebaseDatabase.getInstance().reference.child("terrenos").push().key ?: return

        val terreno = mapOf(
            "id" to terrenoId,
            "nombre" to nombre,
            "descripcion" to descripcion,
            "latitud" to ubicacion.latitude,
            "longitud" to ubicacion.longitude,
            "creadorUid" to FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        )

        FirebaseDatabase.getInstance().reference
            .child("terrenos")
            .child(terrenoId)
            .setValue(terreno)
            .addOnSuccessListener {
                Toast.makeText(this, "Terreno guardado", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al guardar", Toast.LENGTH_SHORT).show()
            }
    }
}
