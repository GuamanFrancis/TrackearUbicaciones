package com.example.trackearubicaciones

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class UsuarioAdapter(
    private val usuarios: List<User>,
    private val onEditar: (User) -> Unit,
    private val onEliminar: (User) -> Unit
) : RecyclerView.Adapter<UsuarioAdapter.UsuarioViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UsuarioViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_usuario, parent, false)
        return UsuarioViewHolder(view)
    }

    override fun onBindViewHolder(holder: UsuarioViewHolder, position: Int) {
        val usuario = usuarios[position]
        holder.tvNombre.text = usuario.nombre ?: "Sin nombre"
        holder.tvEmail.text = usuario.correo ?: "Sin correo"
        holder.tvRol.text = "Rol: ${usuario.rol ?: "Sin rol"}"

        holder.btnEditar.setOnClickListener { onEditar(usuario) }
        holder.btnEliminar.setOnClickListener { onEliminar(usuario) }
    }

    override fun getItemCount(): Int = usuarios.size

    class UsuarioViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNombre: TextView = itemView.findViewById(R.id.tvNombreUsuario)
        val tvEmail: TextView = itemView.findViewById(R.id.tvEmailUsuario)
        val tvRol: TextView = itemView.findViewById(R.id.tvRolUsuario)
        val btnEditar: Button = itemView.findViewById(R.id.btnEditar)
        val btnEliminar: Button = itemView.findViewById(R.id.btnEliminar)
    }
}
