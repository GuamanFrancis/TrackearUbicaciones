package com.example.trackearubicaciones

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth

class AdminMainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toolbar: Toolbar
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_main)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        navigationView.setNavigationItemSelectedListener(this)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_gestionar_usuarios -> {
                val intent = Intent(this, GestionarUsuariosActivity::class.java)
                startActivity(intent)
                return true
            }
            R.id.nav_ver_terrenos -> {
                val intent = Intent(this, VerTerrenosActivity::class.java)
                startActivity(intent)
                return true
            }
            R.id.nav_configuracion -> {
                Toast.makeText(this, "Configuración no implementada aún", Toast.LENGTH_SHORT).show()
                return true
            }
            R.id.nav_cerrar_sesion -> {
                try {
                    auth.signOut()
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } catch (e: Exception) {
                    Log.e("Logout", "Error al cerrar sesión: ${e.message}")
                    Toast.makeText(this, "Error al cerrar sesión", Toast.LENGTH_SHORT).show()
                }
                return true
            }
        }
        return false
    }
}
