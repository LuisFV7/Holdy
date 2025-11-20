package com.example.holdy;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class MisPaquetesActivity extends AppCompatActivity {

    private List<Paquete> listaOriginal = new ArrayList<>(); // lista completa desde Firebase
    private PaquetesAdapter adapter;                         // adapter del RecyclerView
    private FirebaseFirestore db;                            // Firestore

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mis_paquetes);

        // Toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }
        toolbar.setNavigationIcon(R.drawable.ic_back);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // Recycler
        RecyclerView rv = findViewById(R.id.recyclerPaquetes);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setHasFixedSize(true);

        // Adapter inicialmente vacío
        adapter = new PaquetesAdapter(new ArrayList<>());
        rv.setAdapter(adapter);

        // FAB nuevo paquete
        FloatingActionButton fabAdd = findViewById(R.id.fabAddPackage);
        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(MisPaquetesActivity.this, NuevoPaqueteActivity.class);
            startActivity(intent);
        });

        //  Inicializamos Firestore
        db = FirebaseFirestore.getInstance();

        // Carga inicial desde Firebase
        cargarPaquetesDesdeFirebase();
    }

    // ------------ DATOS DE PRUEBA (por si falla Firebase) ------------

    private void cargarPaquetesDePrueba() {
        listaOriginal.clear();
        listaOriginal.add(new Paquete("Amazon", "12/11/2025", "Entregado"));
        listaOriginal.add(new Paquete("Zara", "10/11/2025", "En camino"));
        listaOriginal.add(new Paquete("Correos carta", "09/11/2025", "En buzón"));

        adapter.actualizar(new ArrayList<>(listaOriginal));
    }

    // ------------ CARGAR DATOS DESDE FIREBASE ------------

    private void cargarPaquetesDesdeFirebase() {
        db.collection("paquetes")   // asegúrate de que la colección en Firebase se llama EXACTAMENTE así
                .get()
                .addOnSuccessListener(this::procesarResultado)
                .addOnFailureListener(e -> {
                    Log.e("MisPaquetes", "Error al obtener paquetes", e);
                    Toast.makeText(this, "Error al cargar paquetes", Toast.LENGTH_SHORT).show();

                    //  fallback: si falla Firebase, usamos datos de prueba
                    cargarPaquetesDePrueba();
                });
    }

    private void procesarResultado(QuerySnapshot querySnapshot) {
        listaOriginal.clear();

        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
            Paquete p = doc.toObject(Paquete.class);
            if (p != null) {
                listaOriginal.add(p);
            }
        }

        Log.d("MisPaquetes", "Paquetes Firebase: " + listaOriginal.size());
        Toast.makeText(this, "Paquetes Firebase: " + listaOriginal.size(), Toast.LENGTH_SHORT).show();

        if (listaOriginal.isEmpty()) {
            //  Si no hay nada en Firestore, metemos de prueba para que no se vea vacío
            cargarPaquetesDePrueba();
        } else {
            adapter.actualizar(new ArrayList<>(listaOriginal));
        }
    }

    // ---------------- CONSULTAS FIREBASE CON FILTROS ----------------

    private void cargarPaquetesFiltrado(String estado, Query.Direction ordenFecha) {

        Query query = db.collection("paquetes");

        boolean tieneEstado = (estado != null && !estado.isEmpty());
        boolean tieneOrden = (ordenFecha != null);

        // Filtro por estado
        if (tieneEstado) {
            query = query.whereEqualTo("estado", estado);
        }

        // Orden por fecha SOLO si no hay filtro de estado (para evitar errores de índice)
        if (tieneOrden && !tieneEstado) {
            query = query.orderBy("fecha", ordenFecha);
        }

        query.get()
                .addOnSuccessListener(snapshot -> {
                    listaOriginal.clear();

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Paquete p = doc.toObject(Paquete.class);
                        if (p != null) listaOriginal.add(p);
                    }

                    adapter.actualizar(new ArrayList<>(listaOriginal));
                })
                .addOnFailureListener(e -> {
                    Log.e("MisPaquetes", "Error al filtrar", e);
                    Toast.makeText(this, "Error al filtrar", Toast.LENGTH_SHORT).show();
                });
    }

    // ------------ MENÚ CON SEARCHVIEW Y FILTROS ------------

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        MenuItem item = menu.findItem(R.id.action_buscar);
        SearchView searchView = (SearchView) item.getActionView();
        searchView.setQueryHint("Buscar paquete...");

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filtrarLista(newText);
                return true;
            }
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.filtrar_en_camino) {
            // Ver solo "En camino" (sin ordenar)
            cargarPaquetesFiltrado("En camino", null);
            return true;

        } else if (id == R.id.filtrar_recibidos) {
            // Ver solo "Recibido" (sin ordenar)
            cargarPaquetesFiltrado("Recibido", null);
            return true;

        } else if (id == R.id.orden_mas_nuevos) {
            // Todos, ordenados por fecha descendente
            cargarPaquetesFiltrado(null, Query.Direction.DESCENDING);
            return true;

        } else if (id == R.id.orden_mas_antiguos) {
            // Todos, ordenados por fecha ascendente
            cargarPaquetesFiltrado(null, Query.Direction.ASCENDING);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // ------------ FILTRO LOCAL (BUSCADOR) ------------

    private void filtrarLista(String texto) {
        if (texto == null) texto = "";
        String query = texto.toLowerCase().trim();

        List<Paquete> filtrados = new ArrayList<>();

        if (query.isEmpty()) {
            filtrados.addAll(listaOriginal);  // sin texto, mostramos todo
        } else {
            for (Paquete p : listaOriginal) {
                if ((p.titulo != null && p.titulo.toLowerCase().contains(query)) ||
                        (p.estado != null && p.estado.toLowerCase().contains(query))) {
                    filtrados.add(p);
                }
            }
        }

        adapter.actualizar(filtrados);
    }

    // ------------ CLASE MODELO ------------

    static class Paquete {
        public String titulo;
        public String fecha;
        public String estado;
        public String fotoUrl;   // NUEVO campo para la imagen

        public Paquete() {
            // Constructor vacío necesario para Firestore
        }

        public Paquete(String titulo, String fecha, String estado) {
            this.titulo = titulo;
            this.fecha = fecha;
            this.estado = estado;
            this.fotoUrl = null;
        }

        public Paquete(String titulo, String fecha, String estado, String fotoUrl) {
            this.titulo = titulo;
            this.fecha = fecha;
            this.estado = estado;
            this.fotoUrl = fotoUrl;
        }
    }

    // ------------ ADAPTER ------------

    static class PaquetesAdapter extends RecyclerView.Adapter<PaquetesAdapter.VH> {

        private List<Paquete> data;

        PaquetesAdapter(List<Paquete> data) {
            this.data = data;
        }

        void actualizar(List<Paquete> nuevaLista) {
            this.data = nuevaLista;
            notifyDataSetChanged();
        }

        static class VH extends RecyclerView.ViewHolder {
            ImageView icono;
            TextView tvTitulo, tvFecha, tvEstado;

            VH(@NonNull View itemView) {
                super(itemView);
                icono    = itemView.findViewById(R.id.icono);
                tvTitulo = itemView.findViewById(R.id.tvTitulo);
                tvFecha  = itemView.findViewById(R.id.tvFecha);
                tvEstado = itemView.findViewById(R.id.tvEstado);
            }

            void bind(Paquete p) {
                int iconResPorDefecto = obtenerIconoPorTitulo(p.titulo);

                // Si hay fotoUrl, cargamos con Glide
                if (p.fotoUrl != null && !p.fotoUrl.isEmpty()) {
                    Glide.with(itemView.getContext())
                            .load(p.fotoUrl)
                            .placeholder(iconResPorDefecto)
                            .error(iconResPorDefecto)
                            .into(icono);
                } else {
                    // Si no hay fotoUrl, usamos el icono según el título
                    icono.setImageResource(iconResPorDefecto);
                }

                tvTitulo.setText(p.titulo != null ? p.titulo : "");
                tvFecha.setText("Fecha: " + (p.fecha != null ? p.fecha : ""));
                tvEstado.setText("Estado: " + (p.estado != null ? p.estado : ""));
            }
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_paquete, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            Paquete p = data.get(position);
            holder.bind(p);

            // Al pulsar cualquier card abrimos la pantalla de detalle (PaqueteAmazonActivity)
            holder.itemView.setOnClickListener(v -> {
                Context c = v.getContext();
                Intent i = new Intent(c, PaqueteAmazonActivity.class);
                c.startActivity(i);
            });
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        // Función auxiliar para elegir icono según título
        private static int obtenerIconoPorTitulo(String titulo) {
            if (titulo == null) return R.drawable.ic_package_amazon; // por defecto

            String t = titulo.toLowerCase();
            if (t.contains("amazon")) {
                return R.drawable.ic_package_amazon;
            } else if (t.contains("carta")) {
                return R.drawable.ic_package_carta;
            } else if (t.contains("correos")) {
                return R.drawable.ic_package_correos;
            } else if (t.contains("zara")) {
                return R.drawable.caja_zara;
            }
            // por defecto
            return R.drawable.ic_package_amazon;
        }
    }
}
