package com.example.trackearubicaciones

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.database.ServerValue

class TopografoMainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toolbar: Toolbar
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_topografo_main)

        // Inicializar Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        // Configurar toolbar y drawer
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener(this)

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Configurar header del NavigationView
        setupNavigationHeader()
    }

    private fun setupNavigationHeader() {
        val headerView = navigationView.getHeaderView(0)
        val tvNavNombre = headerView.findViewById<TextView>(R.id.tvNavNombre)
        val tvNavCorreo = headerView.findViewById<TextView>(R.id.tvNavCorreo)

        val currentUser = auth.currentUser
        tvNavCorreo.text = currentUser?.email ?: ""

        // Obtener datos adicionales del usuario desde Firebase
        currentUser?.uid?.let { userId ->
            database.child("usuarios").child(userId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val nombre = snapshot.child("nombre").getValue(String::class.java) ?: "Usuario"
                        val rol = snapshot.child("rol").getValue(String::class.java) ?: "Topógrafo"

                        tvNavNombre.text = nombre
                        // Puedes usar el rol para personalizar más si es necesario
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(this@TopografoMainActivity,
                            "Error al cargar datos del usuario",
                            Toast.LENGTH_SHORT).show()
                    }
                })
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_perfil -> {
                startActivity(Intent(this, PerfilActivity::class.java))
            }
            R.id.nav_registrar_terreno -> {
                startActivity(Intent(this, RegistrarTerrenoActivity::class.java))
            }
            R.id.nav_mis_terrenos -> {
                startActivity(Intent(this, MisTerrenosActivity::class.java))
            }
            R.id.nav_crear_sesion -> {
                crearSesionColaborativa()
            }
            R.id.nav_unirse_sesion -> {
                startActivity(Intent(this, UnirseSesionActivity::class.java))
            }
            R.id.nav_logout -> {
                auth.signOut()
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun crearSesionColaborativa() {
        val userId = auth.currentUser?.uid ?: return
        val userName = auth.currentUser?.displayName ?: "Usuario $userId"

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Crear Sesión Colaborativa")

        // Crear EditText programáticamente con estilo
        val input = EditText(this).apply {
            hint = "Nombre de la sesión"
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
        }

        // Contenedor para el EditText con padding
        val container = FrameLayout(this).apply {
            setPadding(50, 10, 50, 10)
            addView(input)
        }

        builder.setView(container)
        builder.setPositiveButton("Crear") { _, _ ->
            val nombreSesion = input.text.toString().trim()
            if (nombreSesion.isEmpty()) {
                Toast.makeText(this, "Ingresa un nombre para la sesión", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            val sesionesRef = database.child("sesiones")
            val sesionId = sesionesRef.push().key ?: return@setPositiveButton
            val codigo = (100000..999999).random().toString()

            val nuevaSesion = mapOf(
                "creadorUid" to userId,
                "nombre" to nombreSesion,
                "nombreCreador" to userName,
                "codigo" to codigo,
                "timestamp" to ServerValue.TIMESTAMP
            )

            sesionesRef.child(sesionId).setValue(nuevaSesion)
                .addOnSuccessListener {
                    sesionesRef.child("$sesionId/usuariosConectados/$userId").setValue(true)
                        .addOnSuccessListener {
                            val intent = Intent(this, MapaColaborativoActivity::class.java)
                            intent.putExtra("sesionId", sesionId)
                            startActivity(intent)
                        }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error al crear la sesión", Toast.LENGTH_SHORT).show()
                }
        }
        builder.setNegativeButton("Cancelar", null)
        builder.show()
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}