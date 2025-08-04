
package com.example.trackearubicaciones

data class User(
    val uid: String = "",
    val nombre: String? = "",
    val correo: String? = "",
    val rol: String? = "",  // "topografo" o "administrador"
    val activo: Boolean = true
)
