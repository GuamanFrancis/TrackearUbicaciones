package com.example.trackearubicaciones

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.trackearubicaciones.utils.FirebaseUtils

class RegisterActivity : AppCompatActivity() {

    private lateinit var etNombre: EditText
    private lateinit var etCorreo: EditText
    private lateinit var etContrasena: EditText
    private lateinit var etConfirmarContrasena: EditText
    private lateinit var etCodigoAdmin: EditText
    private lateinit var btnRegistrar: Button
    private lateinit var btnIrLogin: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        etNombre = findViewById(R.id.etNombre)
        etCorreo = findViewById(R.id.etCorreo)
        etContrasena = findViewById(R.id.etContrasena)
        etConfirmarContrasena = findViewById(R.id.etConfirmarContrasena)
        etCodigoAdmin = findViewById(R.id.etCodigoAdmin)
        btnRegistrar = findViewById(R.id.btnRegistrar)
        btnIrLogin = findViewById(R.id.btnIrLogin)

        btnRegistrar.setOnClickListener {
            val nombre = etNombre.text.toString().trim()
            val correo = etCorreo.text.toString().trim()
            val contrasena = etContrasena.text.toString()
            val confirmarContrasena = etConfirmarContrasena.text.toString()
            val codigoAdmin = etCodigoAdmin.text.toString().trim()

            if (nombre.isEmpty() || correo.isEmpty() || contrasena.isEmpty() || confirmarContrasena.isEmpty()) {
                Toast.makeText(this, "Por favor completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
                Toast.makeText(this, "Correo inválido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (contrasena.length < 6) {
                Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (contrasena != confirmarContrasena) {
                Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val rol = if (codigoAdmin in listOf("1", "2", "3", "4", "5", "6")) "Administrador" else "Topografo"

            FirebaseUtils.registrarUsuario(nombre, correo, contrasena, rol, this)
        }

        btnIrLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}
