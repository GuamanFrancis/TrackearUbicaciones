package com.example.trackearubicaciones

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.trackearubicaciones.utils.FirebaseUtils
import com.google.firebase.database.*

class GestionarUsuariosActivity : AppCompatActivity() {

    private lateinit var recyclerUsuarios: RecyclerView
    private lateinit var btnAgregarUsuario: Button
    private lateinit var databaseReference: DatabaseReference
    private val listaUsuarios = mutableListOf<User>()
    private lateinit var adapter: UsuarioAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gestionar_usuarios)

        recyclerUsuarios = findViewById(R.id.recyclerUsuarios)
        btnAgregarUsuario = findViewById(R.id.btnAgregarUsuario)
        databaseReference = FirebaseDatabase.getInstance().getReference("usuarios")

        adapter = UsuarioAdapter(listaUsuarios,
            onEditar = { usuario -> mostrarDialogoEditar(usuario) },
            onEliminar = { usuario -> eliminarUsuario(usuario) }
        )

        recyclerUsuarios.layoutManager = LinearLayoutManager(this)
        recyclerUsuarios.adapter = adapter

        btnAgregarUsuario.setOnClickListener {
            mostrarDialogoAgregar()
        }

        obtenerUsuarios()
    }

    private fun obtenerUsuarios() {
        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                listaUsuarios.clear()
                for (dato in snapshot.children) {
                    val usuario = dato.getValue(User::class.java)
                    if (usuario != null) {
                        listaUsuarios.add(usuario)
                    }
                }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@GestionarUsuariosActivity, "Error al obtener usuarios", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun mostrarDialogoAgregar() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_usuario, null)
        val etNombre = dialogView.findViewById<EditText>(R.id.etNombre)
        val etCorreo = dialogView.findViewById<EditText>(R.id.etCorreo)
        val spinnerRol = dialogView.findViewById<Spinner>(R.id.spinnerRol)

        val roles = listOf("Administrador", "Topógrafo")
        val adapterSpinner = ArrayAdapter(this, android.R.layout.simple_spinner_item, roles)
        adapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerRol.adapter = adapterSpinner

        val dialog = AlertDialog.Builder(this)
            .setTitle("Agregar Usuario")
            .setView(dialogView)
            .setPositiveButton("Guardar", null)
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.setOnShowListener {
            val botonGuardar = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            botonGuardar.setOnClickListener {
                val nombre = etNombre.text.toString().trim()
                val correo = etCorreo.text.toString().trim()
                val rol = spinnerRol.selectedItem?.toString()

                if (nombre.isNotEmpty() && correo.isNotEmpty() && rol != null) {
                    // Usa la función de FirebaseUtils para crear usuario con contraseña temporal + email verification
                    FirebaseUtils.registrarUsuarioDesdeAdmin(nombre, correo, rol, this) { success, message ->
                        runOnUiThread {
                            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                            if (success) {
                                dialog.dismiss()
                            }
                        }
                    }
                } else {
                    Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                }
            }
        }

        dialog.show()
    }

    private fun mostrarDialogoEditar(usuario: User) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_usuario, null)
        val etNombre = dialogView.findViewById<EditText>(R.id.etNombre)
        val etCorreo = dialogView.findViewById<EditText>(R.id.etCorreo)
        val spinnerRol = dialogView.findViewById<Spinner>(R.id.spinnerRol)

        val roles = listOf("Administrador", "Topógrafo")
        val adapterSpinner = ArrayAdapter(this, android.R.layout.simple_spinner_item, roles)
        adapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerRol.adapter = adapterSpinner

        etNombre.setText(usuario.nombre)
        etCorreo.setText(usuario.correo)
        val rolIndex = roles.indexOf(usuario.rol)
        if (rolIndex >= 0) spinnerRol.setSelection(rolIndex)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Editar Usuario")
            .setView(dialogView)
            .setPositiveButton("Actualizar", null)
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.setOnShowListener {
            val botonActualizar = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            botonActualizar.setOnClickListener {
                val nuevoNombre = etNombre.text.toString().trim()
                val nuevoCorreo = etCorreo.text.toString().trim()
                val nuevoRol = spinnerRol.selectedItem?.toString()

                if (nuevoNombre.isNotEmpty() && nuevoCorreo.isNotEmpty() && nuevoRol != null) {
                    val usuarioActualizado = User(usuario.uid, nuevoNombre, nuevoCorreo, nuevoRol)
                    databaseReference.child(usuario.uid).setValue(usuarioActualizado)
                    dialog.dismiss()
                } else {
                    Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                }
            }
        }

        dialog.show()
    }

    private fun eliminarUsuario(usuario: User) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar Usuario")
            .setMessage("¿Estás seguro de eliminar este usuario?")
            .setPositiveButton("Eliminar") { _, _ ->
                databaseReference.child(usuario.uid).removeValue()
                Toast.makeText(this, "Usuario eliminado", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

}
