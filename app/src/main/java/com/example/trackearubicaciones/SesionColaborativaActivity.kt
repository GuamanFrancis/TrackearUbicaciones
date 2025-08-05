package com.example.trackearubicaciones

import android.app.AlertDialog
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class SesionColaborativaActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance().reference.child("sesiones")

    private lateinit var btnCrearSesion: Button
    private lateinit var btnUnirseSesion: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sesion_colaborativa)

        btnCrearSesion = findViewById(R.id.btnCrearSesion)
        btnUnirseSesion = findViewById(R.id.btnUnirseSesion)

        btnCrearSesion.setOnClickListener {
            crearSesion()
        }

        btnUnirseSesion.setOnClickListener {
            mostrarDialogoSesiones()
        }
    }

    private fun crearSesion() {
        val userId = auth.currentUser?.uid ?: return Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show()

        // Crear nueva sesión
        val nuevaSesionRef = db.push()
        val sesionId = nuevaSesionRef.key ?: return

        val sesionData = mapOf(
            "creadorUid" to userId,
            "timestamp" to ServerValue.TIMESTAMP,
            "usuariosConectados" to mapOf(userId to true) // El creador ya está conectado
        )

        nuevaSesionRef.setValue(sesionData)
            .addOnSuccessListener {
                Toast.makeText(this, "Sesión creada con ID:\n$sesionId", Toast.LENGTH_LONG).show()
                // Aquí podrías abrir la actividad del mapa colaborativo pasando sesionId
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al crear sesión", Toast.LENGTH_SHORT).show()
            }
    }

    private fun mostrarDialogoSesiones() {
        // Obtener la lista de sesiones
        db.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val sesiones = mutableListOf<String>()
                for (child in snapshot.children) {
                    sesiones.add(child.key ?: "")
                }
                if (sesiones.isEmpty()) {
                    Toast.makeText(this@SesionColaborativaActivity, "No hay sesiones disponibles", Toast.LENGTH_SHORT).show()
                    return
                }
                mostrarSelectorSesion(sesiones)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@SesionColaborativaActivity, "Error al cargar sesiones", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun mostrarSelectorSesion(sesiones: List<String>) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Selecciona una sesión")
        val sesionesArray = sesiones.toTypedArray()

        builder.setItems(sesionesArray) { dialog, which ->
            val sesionId = sesionesArray[which]
            unirseASesion(sesionId)
        }
        builder.show()
    }

    private fun unirseASesion(sesionId: String) {
        val userId = auth.currentUser?.uid ?: return Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show()

        // Referencia a usuariosConectados en la sesión
        val usuariosConectadosRef = db.child(sesionId).child("usuariosConectados")

        // Agregar usuario a usuariosConectados
        usuariosConectadosRef.child(userId).setValue(true)
            .addOnSuccessListener {
                Toast.makeText(this, "Te uniste a la sesión: $sesionId", Toast.LENGTH_LONG).show()
                // Aquí abre el mapa colaborativo pasando el sesionId para que se unan y compartan ubicación/puntos
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al unirse a la sesión", Toast.LENGTH_SHORT).show()
            }
    }
}
