package com.example.trackearubicaciones

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.location.*
import com.google.firebase.database.FirebaseDatabase

class LocationForegroundService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private lateinit var notificationManager: NotificationManager
    private lateinit var database: FirebaseDatabase
    private var sessionId: String? = null
    private var userId: String? = null
    private var lastLocation: android.location.Location? = null
    private var lastFirebaseUpdateTime = 0L
    private val handler = Handler(Looper.getMainLooper())
    private var isServiceRunning = false

    companion object {
        private const val CHANNEL_ID = "location_tracking_channel"
        private const val NOTIFICATION_ID = 1234
        private const val LOCATION_UPDATE_INTERVAL = 1000L // 1 segundo
        private const val FASTEST_UPDATE_INTERVAL = 500L // 500 ms
        private const val FIREBASE_UPDATE_INTERVAL = 2000L // 2 segundos para Firebase
        private const val MAX_ACCURACY_THRESHOLD = 50.0f // Metros

        fun startService(context: Context, sessionId: String, userId: String) {
            val intent = Intent(context, LocationForegroundService::class.java).apply {
                putExtra("sessionId", sessionId)
                putExtra("userId", userId)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, LocationForegroundService::class.java)
            context.stopService(intent)
        }
    }

    override fun onBind(intent: Intent): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        database = FirebaseDatabase.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
        createLocationRequest()
        createLocationCallback()
        isServiceRunning = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        sessionId = intent?.getStringExtra("sessionId")
        userId = intent?.getStringExtra("userId")

        if (sessionId == null || userId == null) {
            Log.e("LocationForegroundService", "sessionId o userId nulos, deteniendo servicio")
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, createNotification())
        startLocationUpdates()

        return START_STICKY
    }

    private fun createLocationRequest() {
        locationRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, LOCATION_UPDATE_INTERVAL).apply {
                setMinUpdateIntervalMillis(FASTEST_UPDATE_INTERVAL)
                setWaitForAccurateLocation(true)
            }.build()
        } else {
            LocationRequest.create().apply {
                priority = Priority.PRIORITY_HIGH_ACCURACY
                interval = LOCATION_UPDATE_INTERVAL
                fastestInterval = FASTEST_UPDATE_INTERVAL
            }
        }
    }

    private fun createLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                locationResult.lastLocation?.let { location ->
                    if (location.hasAccuracy() && location.accuracy <= MAX_ACCURACY_THRESHOLD) {
                        // Enviar ubicación al BroadcastReceiver inmediatamente
                        val intent = Intent("LOCATION_UPDATE").apply {
                            putExtra("latitude", location.latitude)
                            putExtra("longitude", location.longitude)
                        }
                        LocalBroadcastManager.getInstance(this@LocationForegroundService).sendBroadcast(intent)
                        Log.d("LocationForegroundService", "Ubicación enviada al Broadcast: lat=${location.latitude}, lon=${location.longitude}, accuracy=${location.accuracy}")

                        // Enviar a Firebase con throttling
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastFirebaseUpdateTime >= FIREBASE_UPDATE_INTERVAL) {
                            saveLocationToFirebase(location)
                            lastFirebaseUpdateTime = currentTime
                        }
                        lastLocation = location
                    } else {
                        Log.d("LocationForegroundService", "Ubicación descartada por baja precisión: ${location.accuracy}")
                    }
                }
            }
        }
    }

    private fun saveLocationToFirebase(location: android.location.Location) {
        userId?.let { uid ->
            sessionId?.let { sid ->
                val refUser = database.getReference("sesiones").child(sid).child("usuariosConectados").child(uid)
                val datos = mapOf("latitud" to location.latitude, "longitud" to location.longitude)
                refUser.setValue(datos).addOnFailureListener { e ->
                    Log.e("LocationForegroundService", "Error al guardar ubicación en Firebase: ${e.message}")
                    // Reintentar después de 5 segundos si falla
                    handler.postDelayed({ saveLocationToFirebase(location) }, 5000L)
                }.addOnSuccessListener {
                    Log.d("LocationForegroundService", "Ubicación guardada en Firebase: lat=${location.latitude}, lon=${location.longitude}")
                }
            }
        }
    }

    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MapaColaborativoActivity::class.java).apply {
            putExtra("sesionId", sessionId)
            putExtra("userId", userId)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Rastreando ubicación")
            .setContentText("Seguimiento de ubicación activo en la sesión colaborativa")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setSilent(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Rastreo de ubicación",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Canal para notificaciones de seguimiento de ubicación"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            ).addOnFailureListener { e ->
                Log.e("LocationForegroundService", "Error al iniciar actualizaciones de ubicación: ${e.message}")
                // Notificar a la actividad del problema
                val intent = Intent("LOCATION_ERROR").apply {
                    putExtra("error", "No se pudo iniciar el rastreo de ubicación")
                }
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
                stopSelf()
            }
        } else {
            Log.e("LocationForegroundService", "Permiso ACCESS_FINE_LOCATION no otorgado")
            // Notificar a la actividad para que solicite permisos
            val intent = Intent("LOCATION_ERROR").apply {
                putExtra("error", "Permiso de ubicación no otorgado")
            }
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
            stopSelf()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        fusedLocationClient.removeLocationUpdates(locationCallback)
        handler.removeCallbacksAndMessages(null)
        // Limpiar datos en Firebase
        userId?.let { uid ->
            sessionId?.let { sid ->
                database.getReference("sesiones").child(sid).child("usuariosConectados").child(uid).removeValue()
                    .addOnFailureListener { e ->
                        Log.e("LocationForegroundService", "Error al remover usuario en Firebase: ${e.message}")
                    }
            }
        }
        Log.d("LocationForegroundService", "Servicio destruido")
    }
}