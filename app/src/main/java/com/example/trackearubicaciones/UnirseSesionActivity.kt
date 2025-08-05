package com.example.trackearubicaciones

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class SesionAdapter(
    private val sesiones: List<SesionVisible>,
    private val onItemClick: (SesionVisible) -> Unit
) : RecyclerView.Adapter<SesionAdapter.SesionViewHolder>() {

    class SesionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNombre: TextView = itemView.findViewById(R.id.tvNombreSesion)
        val tvCreador: TextView = itemView.findViewById(R.id.tvCreador)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SesionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_sesion, parent, false)
        return SesionViewHolder(view)
    }

    override fun onBindViewHolder(holder: SesionViewHolder, position: Int) {
        val sesion = sesiones[position]
        holder.tvNombre.text = sesion.nombreSesion
        holder.tvCreador.text = "Creador: ${sesion.nombreCreador}"
        holder.itemView.setOnClickListener {
            Log.d("SesionAdapter", "Clic en sesión: ${sesion.id}")
            onItemClick(sesion)
        }
    }

    override fun getItemCount(): Int = sesiones.size
}

class UnirseSesionActivity : AppCompatActivity() {

    private lateinit var rvSesiones: RecyclerView
    private lateinit var adapter: SesionAdapter
    private lateinit var edtBuscarSesion: EditText
    private lateinit var edtCodigo: EditText
    private lateinit var btnUnirse: Button
    private val sesionesList = mutableListOf<SesionVisible>()
    private var sesionSeleccionada: SesionVisible? = null
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_unirse_sesion)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        rvSesiones = findViewById(R.id.recyclerViewSesiones)
        edtBuscarSesion = findViewById(R.id.edtBuscarSesion)
        edtCodigo = findViewById(R.id.edtCodigo)
        btnUnirse = findViewById(R.id.btnUnirse)

        adapter = SesionAdapter(sesionesList) { sesion ->
            sesionSeleccionada = sesion
            // Si es el creador, ocultar el campo de código
            if (currentUserId == sesion.creadorId) {
                edtCodigo.visibility = View.GONE
            } else {
                edtCodigo.visibility = View.VISIBLE
                edtCodigo.setText("")
            }
            Toast.makeText(this, "Sesión seleccionada: ${sesion.nombreSesion}", Toast.LENGTH_SHORT).show()
        }
        rvSesiones.adapter = adapter

        cargarSesiones()

        edtBuscarSesion.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s:CharSequence?, start: Int, before: Int, count: Int) {
                filtrarSesiones(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnUnirse.setOnClickListener {
            unirseASesion()
        }
    }

    private fun cargarSesiones() {
        val sesionesRef = FirebaseDatabase.getInstance().getReference("sesiones")
        sesionesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                sesionesList.clear()
                for (sesionSnapshot in snapshot.children) {
                    val id = sesionSnapshot.key ?: continue
                    val creadorId = sesionSnapshot.child("creadorUid").getValue(String::class.java) ?: "Desconocido"
                    val nombreSesion = sesionSnapshot.child("nombre").getValue(String::class.java) ?: "Sin nombre"
                    val nombreCreador = sesionSnapshot.child("nombreCreador").getValue(String::class.java) ?: "Sin nombre"
                    val codigo = sesionSnapshot.child("codigo").getValue(String::class.java) ?: ""
                    val timestamp = sesionSnapshot.child("timestamp").getValue(Long::class.java) ?: 0L
                    sesionesList.add(SesionVisible(id, creadorId, nombreCreador, timestamp, nombreSesion, codigo))
                }
                adapter.notifyDataSetChanged()
                if (sesionesList.isEmpty()) {
                    Toast.makeText(this@UnirseSesionActivity, "No hay sesiones disponibles", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("UnirseSesionActivity", "Error al cargar sesiones: ${error.message}")
                Toast.makeText(this@UnirseSesionActivity, "Error al cargar sesiones: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun filtrarSesiones(query: String) {
        val filteredList = if (query.isEmpty()) {
            sesionesList
        } else {
            sesionesList.filter { it.nombreSesion.contains(query, ignoreCase = true) }
        }
        adapter = SesionAdapter(filteredList) { sesion ->
            sesionSeleccionada = sesion
            if (currentUserId == sesion.creadorId) {
                edtCodigo.visibility = View.GONE
            } else {
                edtCodigo.visibility = View.VISIBLE
                edtCodigo.setText("")
            }
            Toast.makeText(this, "Sesión seleccionada: ${sesion.nombreSesion}", Toast.LENGTH_SHORT).show()
        }
        rvSesiones.adapter = adapter
    }

    private fun unirseASesion() {
        val userId = currentUserId
        if (userId == null) {
            Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        val sesion = sesionSeleccionada ?: run {
            Toast.makeText(this, "Selecciona una sesión", Toast.LENGTH_SHORT).show()
            return
        }

        // Verificar si es el creador (no necesita código)
        if (userId == sesion.creadorId) {
            unirseASesionDirectamente(sesion.id)
            return
        }

        // Para otros usuarios, verificar código
        val codigo = edtCodigo.text.toString().trim()
        if (codigo.isEmpty()) {
            Toast.makeText(this, "Ingresa el código de la sesión", Toast.LENGTH_SHORT).show()
            return
        }

        if (codigo == sesion.codigo) {
            unirseASesionDirectamente(sesion.id)
        } else {
            Toast.makeText(this, "Código incorrecto", Toast.LENGTH_SHORT).show()
        }
    }

    private fun unirseASesionDirectamente(sesionId: String) {
        val userId = currentUserId ?: return

        val sesionesRef = FirebaseDatabase.getInstance().getReference("sesiones").child(sesionId)
        sesionesRef.child("usuariosConectados").child(userId).setValue(true)
            .addOnSuccessListener {
                Log.d("UnirseSesionActivity", "Unido a sesión: $sesionId")
                val intent = Intent(this, MapaColaborativoActivity::class.java)
                intent.putExtra("sesionId", sesionId)
                startActivity(intent)
                finish()
            }
            .addOnFailureListener {
                Log.e("UnirseSesionActivity", "Error al unirse a sesión: ${it.message}")
                Toast.makeText(this, "Error al unirse a la sesión", Toast.LENGTH_SHORT).show()
            }
    }
}

data class SesionVisible(
    val id: String,
    val creadorId: String,
    val nombreCreador: String,
    val timestamp: Long,
    val nombreSesion: String = "Sin nombre",
    val codigo: String = ""
)