package com.example.trackearubicaciones

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.trackearubicaciones.utils.FirebaseUtils
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase

class UserManagementActivity : AppCompatActivity() {

    private lateinit var userList: MutableList<User>
    private lateinit var adapter: UsuarioAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_management)

        val recycler = findViewById<RecyclerView>(R.id.recyclerUsuarios)
        recycler.layoutManager = LinearLayoutManager(this)

        userList = mutableListOf()
        adapter = UsuarioAdapter(userList,
            onEditar = { usuario ->
                Toast.makeText(this, "Editar usuario: ${usuario.nombre}", Toast.LENGTH_SHORT).show()
            },
            onEliminar = { usuario ->
                Toast.makeText(this, "Eliminar usuario: ${usuario.nombre}", Toast.LENGTH_SHORT).show()
            }
        )
        recycler.adapter = adapter

        FirebaseUtils.usuariosRef.get().addOnSuccessListener { snapshot: DataSnapshot ->
            userList.clear()
            for (childSnapshot in snapshot.children) {
                val user = childSnapshot.getValue(User::class.java)
                if (user != null) {
                    userList.add(user)
                }
            }
            adapter.notifyDataSetChanged()
        }.addOnFailureListener { error ->
            Toast.makeText(this, "Error al cargar usuarios: ${error.message}", Toast.LENGTH_LONG).show()
        }
    }
}
