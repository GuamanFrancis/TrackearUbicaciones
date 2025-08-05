package com.example.trackearubicaciones

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class PerfilActivity : AppCompatActivity() {

    private lateinit var tvNombre: TextView
    private lateinit var tvCorreo: TextView
    private lateinit var tvRol: TextView
    private lateinit var btnEditarPerfil: Button
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil)

        // Inicializar vistas
        tvNombre = findViewById(R.id.tvNombre)
        tvCorreo = findViewById(R.id.tvCorreo)
        tvRol = findViewById(R.id.tvRol)
        btnEditarPerfil = findViewById(R.id.btnEditarPerfil)

        // Inicializar Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        // Cargar datos del usuario
        cargarDatosUsuario()

        // Configurar el clic del botón
        btnEditarPerfil.setOnClickListener {
            mostrarDialogoEditarPerfil()
        }
    }

    private fun cargarDatosUsuario() {
        val userId = auth.currentUser?.uid ?: return

        database.child("usuarios").child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val nombre = snapshot.child("nombre").getValue(String::class.java) ?: ""
                val correo = snapshot.child("correo").getValue(String::class.java) ?: ""
                val rol = snapshot.child("rol").getValue(String::class.java) ?: ""

                tvNombre.text = nombre
                tvCorreo.text = correo
                tvRol.text = "Rol: $rol"
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@PerfilActivity, "Error al cargar datos", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun mostrarDialogoEditarPerfil() {
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Editar Perfil")

        // Crear vista personalizada para el diálogo
        val view = layoutInflater.inflate(R.layout.dialog_editar_perfil, null)
        builder.setView(view)

        val etNombre = view.findViewById<android.widget.EditText>(R.id.etNombre)
        val etCorreo = view.findViewById<android.widget.EditText>(R.id.etCorreo)

        // Prellenar con los datos actuales
        etNombre.setText(tvNombre.text.toString())
        etCorreo.setText(tvCorreo.text.toString())

        builder.setPositiveButton("Guardar") { dialog, _ ->
            val nuevoNombre = etNombre.text.toString().trim()
            val nuevoCorreo = etCorreo.text.toString().trim()

            if (nuevoNombre.isEmpty() || nuevoCorreo.isEmpty()) {
                Toast.makeText(this, "Complete todos los campos", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            actualizarPerfil(nuevoNombre, nuevoCorreo)
            dialog.dismiss()
        }

        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.dismiss()
        }

        builder.create().show()
    }

    private fun actualizarPerfil(nombre: String, correo: String) {
        val userId = auth.currentUser?.uid ?: return

        // Actualizar en Firebase
        val updates = hashMapOf<String, Any>(
            "nombre" to nombre,
            "correo" to correo
        )

        database.child("usuarios").child(userId).updateChildren(updates)
            .addOnSuccessListener {
                tvNombre.text = nombre
                tvCorreo.text = correo
                Toast.makeText(this, "Perfil actualizado", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al actualizar", Toast.LENGTH_SHORT).show()
            }
    }
}