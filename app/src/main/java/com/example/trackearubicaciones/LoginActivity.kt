package com.example.trackearubicaciones

import android.app.AlertDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.trackearubicaciones.utils.FirebaseUtils
import android.content.Intent
import com.example.trackearubicaciones.AdminMainActivity
import com.example.trackearubicaciones.TopografoMainActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var edtCorreo: EditText
    private lateinit var edtContrasena: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnRegistrar: Button
    private lateinit var tvForgotPassword: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        edtCorreo = findViewById(R.id.edtCorreo)
        edtContrasena = findViewById(R.id.edtContrasena)
        btnLogin = findViewById(R.id.btnLogin)
        btnRegistrar = findViewById(R.id.btnRegistrar)
        tvForgotPassword = findViewById(R.id.tvForgotPassword)

        btnLogin.setOnClickListener {
            val correo = edtCorreo.text.toString().trim()
            val contrasena = edtContrasena.text.toString()

            if (correo.isEmpty() || contrasena.isEmpty()) {
                Toast.makeText(this, "Por favor completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            FirebaseUtils.loginUsuario(correo, contrasena, this) { success, rol ->
                if (success && rol != null) {
                    when (rol) {
                        "Administrador" -> {
                            startActivity(Intent(this, AdminMainActivity::class.java))
                            finish()
                        }
                        "Topografo" -> {
                            startActivity(Intent(this, TopografoMainActivity::class.java))
                            finish()
                        }
                        else -> Toast.makeText(this, "Rol no reconocido", Toast.LENGTH_SHORT).show()
                    }
                }
                // Toasts de error se manejan dentro de FirebaseUtils.loginUsuario
            }
        }

        btnRegistrar.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        // Aquí el manejo para cambiar la contraseña
        tvForgotPassword.setOnClickListener {
            mostrarDialogoRecuperarContrasena()
        }
    }

    private fun mostrarDialogoRecuperarContrasena() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Recuperar contraseña")

        val inputCorreo = EditText(this)
        inputCorreo.hint = "Ingresa tu correo"
        inputCorreo.inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        inputCorreo.setPadding(50, 40, 50, 40)
        builder.setView(inputCorreo)

        builder.setPositiveButton("Enviar") { dialog, _ ->
            val correo = inputCorreo.text.toString().trim()
            if (correo.isEmpty()) {
                Toast.makeText(this, "Debes ingresar un correo", Toast.LENGTH_SHORT).show()
            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
                Toast.makeText(this, "Correo inválido", Toast.LENGTH_SHORT).show()
            } else {
                FirebaseUtils.enviarCorreoRecuperacion(correo, this)
            }
            dialog.dismiss()
        }

        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.dismiss()
        }

        builder.show()
    }
}
