package com.example.trackearubicaciones.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.trackearubicaciones.LoginActivity
import com.example.trackearubicaciones.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

object FirebaseUtils {
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()
    val usuariosRef = database.getReference("usuarios")

    fun registrarUsuario(
        nombre: String,
        correo: String,
        contrasena: String,
        rol: String,
        context: Context
    ) {
        auth.createUserWithEmailAndPassword(correo, contrasena).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val uid = auth.currentUser?.uid ?: ""
                val user = User(uid, nombre, correo, rol)
                usuariosRef.child(uid).setValue(user).addOnSuccessListener {
                    auth.currentUser?.sendEmailVerification()
                    Toast.makeText(
                        context,
                        "Registro exitoso. Verifica tu correo para activar la cuenta.",
                        Toast.LENGTH_LONG
                    ).show()
                    context.startActivity(Intent(context, LoginActivity::class.java))
                }.addOnFailureListener {
                    Toast.makeText(context, "Error al guardar usuario: ${it.message}", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(context, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun registrarUsuarioDesdeAdmin(
        nombre: String,
        correo: String,
        rol: String,
        context: Context,
        onResult: (Boolean, String) -> Unit
    ) {
        val contrasenaTemporal = "123456"
        auth.createUserWithEmailAndPassword(correo, contrasenaTemporal).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val firebaseUser = auth.currentUser
                firebaseUser?.sendEmailVerification()
                val uid = firebaseUser?.uid ?: ""
                if (uid.isNotEmpty()) {
                    val user = User(uid, nombre, correo, rol)
                    usuariosRef.child(uid).setValue(user).addOnSuccessListener {
                        onResult(true, "Usuario creado y correo de verificación enviado")
                    }.addOnFailureListener { e ->
                        onResult(false, "Error al guardar usuario en DB: ${e.message}")
                    }
                } else {
                    onResult(false, "Error: UID vacío")
                }
            } else {
                onResult(false, "Error al crear usuario: ${task.exception?.message}")
            }
        }
    }

    fun loginUsuario(
        correo: String,
        contrasena: String,
        context: Context,
        onResult: (Boolean, String?) -> Unit
    ) {
        auth.signInWithEmailAndPassword(correo, contrasena).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val user = auth.currentUser
                if (user != null) {
                    if (user.isEmailVerified) {
                        usuariosRef.child(user.uid).get().addOnSuccessListener { snapshot ->
                            val usuario = snapshot.getValue(User::class.java)
                            (context as? Activity)?.runOnUiThread {
                                onResult(true, usuario?.rol)
                            }
                        }.addOnFailureListener {
                            (context as? Activity)?.runOnUiThread {
                                Toast.makeText(context, "Error al obtener datos de usuario", Toast.LENGTH_LONG).show()
                                onResult(false, null)
                            }
                        }
                    } else {
                        (context as? Activity)?.runOnUiThread {
                            Toast.makeText(context, "Por favor verifica tu correo antes de ingresar.", Toast.LENGTH_LONG).show()
                            onResult(false, null)
                        }
                    }
                } else {
                    (context as? Activity)?.runOnUiThread {
                        Toast.makeText(context, "Usuario no encontrado.", Toast.LENGTH_LONG).show()
                        onResult(false, null)
                    }
                }
            } else {
                (context as? Activity)?.runOnUiThread {
                    Toast.makeText(context, "Error en login: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    onResult(false, null)
                }
            }
        }
    }

    fun cerrarSesion() {
        auth.signOut()
    }

    fun enviarCorreoRecuperacion(correo: String, context: Context) {
        auth.sendPasswordResetEmail(correo)
            .addOnSuccessListener {
                Toast.makeText(context, "Correo de recuperación enviado.", Toast.LENGTH_LONG).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error al enviar correo: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

}
