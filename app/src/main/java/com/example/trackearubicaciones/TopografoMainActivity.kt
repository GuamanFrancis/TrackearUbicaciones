package com.example.trackearubicaciones

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth

class TopografoMainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toolbar: Toolbar

    // Botones principales en el contenido
    private lateinit var btnRegistrarTerreno: Button
    private lateinit var btnVerMisTerrenos: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_topografo_main)

        // Vincular Toolbar
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Vincular DrawerLayout y NavigationView
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener(this)

        // Toggle para abrir/cerrar drawer con botón en toolbar
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Vincular botones del layout principal
        btnRegistrarTerreno = findViewById(R.id.btnRegistrarTerreno)
        btnVerMisTerrenos = findViewById(R.id.btnVerMisTerrenos)

        // Manejar clicks de botones del contenido principal
        btnRegistrarTerreno.setOnClickListener {
            startActivity(Intent(this, RegistrarTerrenoActivity::class.java))
        }
        btnVerMisTerrenos.setOnClickListener {
            startActivity(Intent(this, MisTerrenosActivity::class.java))
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_registrar_terreno -> {
                startActivity(Intent(this, RegistrarTerrenoActivity::class.java))
                drawerLayout.closeDrawers()
                return true
            }
            R.id.nav_mis_terrenos -> {
                startActivity(Intent(this, MisTerrenosActivity::class.java))
                drawerLayout.closeDrawers()
                return true
            }
            R.id.nav_logout -> {
                FirebaseAuth.getInstance().signOut()
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
                return true
            }
        }
        return false
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(navigationView)) {
            drawerLayout.closeDrawers()
        } else {
            super.onBackPressed()
        }
    }
}
