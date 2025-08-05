
package com.example.trackearubicaciones


data class Sesion(
    val id: String = "",
    val creadorId: String = "",
    val nombreCreador: String = "",
    val timestamp: Long = 0L,
    val estado: String = "activa"
)
