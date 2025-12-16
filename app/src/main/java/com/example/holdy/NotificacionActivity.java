package com.example.holdy;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

// Pantalla que muestra TODAS las notificaciones:
// - Paquetes (dummy por ahora)
// - Eventos de seguridad (colección "eventos" en Firestore)
public class NotificacionActivity extends AppCompatActivity {

    // RecyclerView y adaptador
    private RecyclerView rvNotificacionesFull;
    private NotificacionAdapter notifAdapter;

    // Lista original con TODAS las notificaciones
    private final List<Notificacion> listaOriginal = new ArrayList<>();
    // Lista filtrada que realmente se muestra
    private final List<Notificacion> listaFiltrada = new ArrayList<>();

    // Botones de filtro
    private Button btnFiltroTodas, btnFiltroPaquetes, btnFiltroSeguridad;

    // Caja de búsqueda
    private EditText edtBuscarNotif;

    // Filtro actual (todas / paquetes / seguridad)
    private String filtroActual = "todas";

    // Texto de búsqueda actual
    private String textoBusqueda = "";

    // Firestore
    private FirebaseFirestore db;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notificacion);

        // ---- Firebase ----
        db = FirebaseFirestore.getInstance();

        // ---- Referencias a vistas ----
        rvNotificacionesFull = findViewById(R.id.rvNotificacionesFull);
        rvNotificacionesFull.setLayoutManager(new LinearLayoutManager(this));
        notifAdapter = new NotificacionAdapter(listaFiltrada);
        rvNotificacionesFull.setAdapter(notifAdapter);

        ImageView imgBack = findViewById(R.id.imgBackNotif);
        btnFiltroTodas = findViewById(R.id.btnFiltroTodas);
        btnFiltroPaquetes = findViewById(R.id.btnFiltroPaquetes);
        btnFiltroSeguridad = findViewById(R.id.btnFiltroSeguridad);
        edtBuscarNotif = findViewById(R.id.edtBuscarNotif);

        // Volver atrás
        imgBack.setOnClickListener(v -> finish());

        // ---- Filtros ----
        btnFiltroTodas.setOnClickListener(v -> {
            filtroActual = "todas";
            aplicarFiltro();
            actualizarEstilosBotones();
        });

        btnFiltroPaquetes.setOnClickListener(v -> {
            filtroActual = "paquetes";
            aplicarFiltro();
            actualizarEstilosBotones();
        });

        btnFiltroSeguridad.setOnClickListener(v -> {
            filtroActual = "seguridad";
            aplicarFiltro();
            actualizarEstilosBotones();
        });

        // ---- Buscador ----
        edtBuscarNotif.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                textoBusqueda = s.toString().toLowerCase().trim();
                aplicarFiltro();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // ---- Cargar datos desde Firestore ----
        escucharEventosYPaquetes();

        // Estilo inicial: filtro "Todas" activo
        actualizarEstilosBotones();
    }

    // ---------------------------------------------------
    // Escucha la colección "eventos" y añade también
    // 2 notificaciones de paquetes (dummy por ahora).
    // ---------------------------------------------------
    private void escucharEventosYPaquetes() {
        db.collection("eventos")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((QuerySnapshot value, com.google.firebase.firestore.FirebaseFirestoreException error) -> {
                    if (error != null) {
                        return;
                    }

                    // Limpiamos la lista original
                    listaOriginal.clear();

                    // --------- Paquetes dummy (ejemplo) ---------
                    listaOriginal.add(new Notificacion(
                            "Nuevo paquete entregado",
                            "Amazon - entregado hace 5 min",
                            "5 min",
                            "paquete",
                            "nuevo"
                    ));

                    listaOriginal.add(new Notificacion(
                            "Paquete en camino",
                            "Correos - llega hoy",
                            "2 h",
                            "paquete",
                            "leido"
                    ));

                    // --------- Eventos reales de seguridad ---------
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            String buzonId = doc.getString("buzonId");
                            String mensaje = doc.getString("mensaje");

                            // ⚠️ Usar la misma lectura segura que en InicioActivity
                            Timestamp ts = leerTimestampSeguro(doc, "timestamp");
                            String textoHora = formatearTiempo(ts);

                            String titulo = "Alerta en " + (buzonId != null ? buzonId : "buzón");

                            listaOriginal.add(new Notificacion(
                                    titulo,
                                    mensaje != null ? mensaje : "",
                                    textoHora,
                                    "seguridad",
                                    "urgente"
                            ));
                        }
                    }

                    // Aplicamos filtro actual y búsqueda
                    aplicarFiltro();
                });
    }

    // Lee un campo "timestamp" tolerando diferentes tipos (Timestamp, Long, Double...)
    private Timestamp leerTimestampSeguro(DocumentSnapshot doc, String campo) {
        Object tsObj = doc.get(campo);
        if (tsObj instanceof Timestamp) {
            return (Timestamp) tsObj;
        } else if (tsObj instanceof Long) {
            return new Timestamp(new Date((Long) tsObj));
        } else if (tsObj instanceof Double) {
            return new Timestamp(new Date(((Double) tsObj).longValue()));
        } else {
            return null;
        }
    }

    // ---------------------------------------------------
    // Aplica filtro (todas/paquetes/seguridad) + texto búsqueda
    // sobre listaOriginal → listaFiltrada
    // ---------------------------------------------------
    private void aplicarFiltro() {
        listaFiltrada.clear();

        for (Notificacion n : listaOriginal) {
            String tipo = n.getTipo() != null ? n.getTipo().toLowerCase() : "";
            String estado = n.getEstado() != null ? n.getEstado().toLowerCase() : "";
            String titulo = n.getTitulo() != null ? n.getTitulo().toLowerCase() : "";
            String mensaje = n.getMensaje() != null ? n.getMensaje().toLowerCase() : "";

            // 1) filtro por tipo
            boolean pasaFiltroTipo = false;

            switch (filtroActual) {
                case "todas":
                    pasaFiltroTipo = true;
                    break;
                case "paquetes":
                    pasaFiltroTipo = "paquete".equals(tipo);
                    break;
                case "seguridad":
                    pasaFiltroTipo = "seguridad".equals(tipo) || "urgente".equals(estado);
                    break;
            }

            if (!pasaFiltroTipo) continue;

            // 2) filtro por texto de búsqueda
            if (!textoBusqueda.isEmpty()) {
                boolean coincide =
                        titulo.contains(textoBusqueda) ||
                                mensaje.contains(textoBusqueda);

                if (!coincide) continue;
            }

            listaFiltrada.add(n);
        }

        notifAdapter.notifyDataSetChanged();
    }

    // ---------------------------------------------------
    // Cambia colores de los botones según el filtro activo
    // ---------------------------------------------------
    private void actualizarEstilosBotones() {
        int colorActivoBg = Color.parseColor("#4739C6");   // morado fuerte
        int colorInactivoBg = Color.parseColor("#D6D6F8"); // lila claro
        int colorTextoActivo = Color.WHITE;
        int colorTextoInactivo = Color.parseColor("#23295C");

        if ("todas".equals(filtroActual)) {
            activarBoton(btnFiltroTodas, colorActivoBg, colorTextoActivo);
        } else {
            desactivarBoton(btnFiltroTodas, colorInactivoBg, colorTextoInactivo);
        }

        if ("paquetes".equals(filtroActual)) {
            activarBoton(btnFiltroPaquetes, colorActivoBg, colorTextoActivo);
        } else {
            desactivarBoton(btnFiltroPaquetes, colorInactivoBg, colorTextoInactivo);
        }

        if ("seguridad".equals(filtroActual)) {
            activarBoton(btnFiltroSeguridad, colorActivoBg, colorTextoActivo);
        } else {
            desactivarBoton(btnFiltroSeguridad, colorInactivoBg, colorTextoInactivo);
        }
    }

    private void activarBoton(Button btn, int colorBg, int colorTexto) {
        btn.setBackgroundTintList(ColorStateList.valueOf(colorBg));
        btn.setTextColor(colorTexto);
    }

    private void desactivarBoton(Button btn, int colorBg, int colorTexto) {
        btn.setBackgroundTintList(ColorStateList.valueOf(colorBg));
        btn.setTextColor(colorTexto);
    }

    // ---------------------------------------------------
    // Formatea el Timestamp tipo "5 min", "2 h", "3 d"
    // ---------------------------------------------------
    private String formatearTiempo(Timestamp ts) {
        if (ts == null) return "";

        long ahora = System.currentTimeMillis();
        long evento = ts.toDate().getTime();
        long diff = ahora - evento;

        long minutos = diff / (60 * 1000);
        long horas = diff / (60 * 60 * 1000);
        long dias = diff / (24 * 60 * 60 * 1000);

        if (minutos < 1) return "Ahora";
        if (minutos < 60) return minutos + " min";
        if (horas < 24) return horas + " h";
        return dias + " d";
    }
}
