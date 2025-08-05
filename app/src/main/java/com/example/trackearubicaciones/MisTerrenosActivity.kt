package com.example.trackearubicaciones

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import org.osmdroid.util.GeoPoint
import java.text.SimpleDateFormat
import java.util.*

data class Terreno(
    val id: String = "",
    val nombre: String = "",
    val descripcion: String? = null,
    val latitud: Double? = null,
    val longitud: Double? = null,
    val area: Double? = null,
    val puntos: Map<String, Map<String, Double>>? = null,
    val creadorUid: String = "",
    val nombreCreador: String? = null,
    val timestamp: Long? = null,
    val esCompartido: Boolean = false,
    val sessionId: String? = null,
    val codigo: String? = null
)

class TerrenoAdapter(
    private val terrenos: List<Terreno>,
    private val onItemClick: (Terreno) -> Unit
) : RecyclerView.Adapter<TerrenoAdapter.TerrenoViewHolder>() {

    class TerrenoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNombre: TextView = itemView.findViewById(R.id.tvNombreTerreno)
        val tvArea: TextView = itemView.findViewById(R.id.tvArea)
        val tvFecha: TextView = itemView.findViewById(R.id.tvFecha)
        val tvDescripcion: TextView = itemView.findViewById(R.id.tvDescripcion)
        val tvUbicacion: TextView = itemView.findViewById(R.id.tvUbicacion)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TerrenoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_terreno, parent, false)
        return TerrenoViewHolder(view)
    }

    override fun onBindViewHolder(holder: TerrenoViewHolder, position: Int) {
        val terreno = terrenos[position]
        holder.tvNombre.text = terreno.nombre
        holder.tvArea.text = terreno.area?.let { "Área: ${"%.2f".format(it)} m²" } ?: ""
        holder.tvFecha.text = terreno.timestamp?.let {
            "Fecha: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(it))}"
        } ?: ""
        holder.tvDescripcion.text = terreno.descripcion?.takeIf { it.isNotEmpty() }?.let { "Descripción: $it" } ?: ""
        holder.tvUbicacion.text = if (terreno.latitud != null && terreno.longitud != null) {
            "Ubicación: ${"%.6f".format(terreno.latitud)}, ${"%.6f".format(terreno.longitud)}"
        } else ""
        holder.itemView.setOnClickListener {
            Log.d("TerrenoAdapter", "Clic en terreno: ${terreno.id}")
            onItemClick(terreno)
        }
    }

    override fun getItemCount(): Int = terrenos.size
}

class MisTerrenosActivity : AppCompatActivity() {

    private lateinit var rvMisTerrenos: RecyclerView
    private lateinit var rvTerrenosCompartidos: RecyclerView
    private val misTerrenosList = mutableListOf<Terreno>()
    private val terrenosCompartidosList = mutableListOf<Terreno>()
    private lateinit var misTerrenosAdapter: TerrenoAdapter
    private lateinit var terrenosCompartidosAdapter: TerrenoAdapter
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mis_terrenos)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        rvMisTerrenos = findViewById(R.id.rvMisTerrenos)
        rvTerrenosCompartidos = findViewById(R.id.rvTerrenosCompartidos)

        misTerrenosAdapter = TerrenoAdapter(misTerrenosList) { terreno ->
            Log.d("MisTerrenosActivity", "Iniciando RegistrarTerrenoActivity con terrenoId: ${terreno.id}")
            val intent = Intent(this, RegistrarTerrenoActivity::class.java)
            intent.putExtra("terrenoId", terreno.id)
            startActivity(intent)
        }
        rvMisTerrenos.adapter = misTerrenosAdapter

        terrenosCompartidosAdapter = TerrenoAdapter(terrenosCompartidosList) { terreno ->
            terreno.sessionId?.let {
                Log.d("MisTerrenosActivity", "Iniciando MapaColaborativoActivity con sessionId: $it")
                val intent = Intent(this, MapaColaborativoActivity::class.java)
                intent.putExtra("sesionId", it)
                startActivity(intent)
            }
        }
        rvTerrenosCompartidos.adapter = terrenosCompartidosAdapter

        cargarTerrenos()
    }

    private fun cargarTerrenos() {
        if (userId == null) {
            Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        // Cargar Mis Terrenos
        val terrenosRef = FirebaseDatabase.getInstance().getReference("terrenos")
        terrenosRef.orderByChild("creadorUid").equalTo(userId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    misTerrenosList.clear()
                    for (terrenoSnapshot in snapshot.children) {
                        val terreno = terrenoSnapshot.getValue(Terreno::class.java)?.copy(id = terrenoSnapshot.key ?: "")
                        if (terreno != null && !terreno.esCompartido) {
                            misTerrenosList.add(terreno)
                        }
                    }
                    misTerrenosAdapter.notifyDataSetChanged()
                    if (misTerrenosList.isEmpty()) {
                        Toast.makeText(this@MisTerrenosActivity, "No hay terrenos propios", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("MisTerrenosActivity", "Error al cargar terrenos propios: ${error.message}")
                    Toast.makeText(this@MisTerrenosActivity, "Error al cargar terrenos", Toast.LENGTH_SHORT).show()
                }
            })

        // Cargar Terrenos Compartidos
        val sesionesRef = FirebaseDatabase.getInstance().getReference("sesiones")
        sesionesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                terrenosCompartidosList.clear()
                for (sesionSnapshot in snapshot.children) {
                    val sessionId = sesionSnapshot.key ?: continue
                    val usuariosConectados = sesionSnapshot.child("usuariosConectados").children.mapNotNull { it.key }
                    if (userId in usuariosConectados) {
                        val nombre = sesionSnapshot.child("nombre").getValue(String::class.java) ?: "Sin nombre"
                        val nombreCreador = sesionSnapshot.child("nombreCreador").getValue(String::class.java) ?: "Desconocido"
                        val codigo = sesionSnapshot.child("codigo").getValue(String::class.java) ?: "Sin código"
                        val timestamp = sesionSnapshot.child("timestamp").getValue(Long::class.java) ?: 0L
                        val puntosData = sesionSnapshot.child("puntos").children.mapNotNull { puntoSnapshot ->
                            val lat = puntoSnapshot.child("latitud").getValue(Double::class.java)
                            val lon = puntoSnapshot.child("longitud").getValue(Double::class.java)
                            val key = puntoSnapshot.key ?: return@mapNotNull null // Filtra claves nulas
                            if (lat != null && lon != null) {
                                key to mapOf("latitud" to lat, "longitud" to lon)
                            } else null
                        }.toMap()
                        val area = calcularAreaPoligono(puntosData.values.map { GeoPoint(it["latitud"]!!, it["longitud"]!!) })
                        terrenosCompartidosList.add(
                            Terreno(
                                id = sessionId,
                                nombre = nombre,
                                area = area,
                                puntos = puntosData,
                                creadorUid = sesionSnapshot.child("creadorUid").getValue(String::class.java) ?: "",
                                nombreCreador = nombreCreador,
                                timestamp = timestamp,
                                esCompartido = true,
                                sessionId = sessionId,
                                codigo = codigo
                            )
                        )
                    }
                }
                terrenosCompartidosAdapter.notifyDataSetChanged()
                if (terrenosCompartidosList.isEmpty()) {
                    Toast.makeText(this@MisTerrenosActivity, "No hay terrenos compartidos", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("MisTerrenosActivity", "Error al cargar terrenos compartidos: ${error.message}")
                Toast.makeText(this@MisTerrenosActivity, "Error al cargar terrenos compartidos", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun calcularAreaPoligono(points: List<GeoPoint>): Double {
        if (points.size < 3) return 0.0

        var area = 0.0
        val n = points.size
        val latPromedio = points.map { it.latitude }.average() * Math.PI / 180
        val escalaLongitud = 111_139.0 * Math.cos(latPromedio)
        val escalaLatitud = 111_139.0

        for (i in 0 until n) {
            val j = (i + 1) % n
            area += points[i].longitude * points[j].latitude
            area -= points[j].longitude * points[i].latitude
        }
        area = kotlin.math.abs(area) / 2.0
        return area * escalaLongitud * escalaLatitud
    }
}