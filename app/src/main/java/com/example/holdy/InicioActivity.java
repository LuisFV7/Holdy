package com.example.holdy;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

// Pantalla de inicio: muestra la tarjeta morada, últimas entregas
// y el popup de notificaciones en la campana.
public class InicioActivity extends AppCompatActivity {

    // ------------------- FIREBASE (paquetes) -------------------
    private FirebaseFirestore db;

    // Vistas de últimas entregas (3 elementos)
    private ImageView ivEntrega1, ivEntrega2, ivEntrega3;
    private TextView tvFecha1, tvFecha2, tvFecha3;
    private TextView tvTitulo1, tvTitulo2, tvTitulo3;
    private TextView tvEstado1, tvEstado2, tvEstado3;

    // ------------------- POPUP NOTIFICACIONES -------------------
    private RecyclerView rvNotificaciones;
    private NotificacionAdapter notifAdapter;
    private final List<Notificacion> notifList = new ArrayList<>();

    private CardView cardNotificaciones;
    private ImageView bell;
    private ImageView imgPointer;
    private LinearLayout rowVerTodos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.inicio);

        // Inicializamos Firestore
        db = FirebaseFirestore.getInstance();

        // ------------------- NAV BAR INFERIOR -------------------
        ImageView navRight = findViewById(R.id.navRight);
        navRight.setOnClickListener(v -> {
            Intent intent = new Intent(InicioActivity.this, PerfilActivity.class);
            startActivity(intent);
        });

        ImageView navHome = findViewById(R.id.navHome);
        navHome.setOnClickListener(v -> {
            // Ya estás en inicio, pero podrías refrescar si quisieras
        });

        ImageView navCamara = findViewById(R.id.navCamara);
        navCamara.setOnClickListener(v -> {
            // Más adelante puedes abrir la cámara aquí
        });

        // Card de "Últimas entregas" → abre MisPaquetesActivity
        CardView cardEntregas = findViewById(R.id.cardEntregas);
        cardEntregas.setOnClickListener(v -> {
            Intent intent = new Intent(InicioActivity.this, MisPaquetesActivity.class);
            startActivity(intent);
        });

        // Botón "Ver detalles" de la tarjeta morada → también MisPaquetesActivity
        Button btnVerDetalles = findViewById(R.id.btnVerDetalles);
        btnVerDetalles.setOnClickListener(v -> {
            Intent intent = new Intent(InicioActivity.this, MisPaquetesActivity.class);
            startActivity(intent);
        });

        // ------------------- REFERENCIAS ÚLTIMAS ENTREGAS -------------------
        ivEntrega1 = findViewById(R.id.ivEntrega1);
        ivEntrega2 = findViewById(R.id.ivEntrega2);
        ivEntrega3 = findViewById(R.id.ivEntrega3);

        tvFecha1 = findViewById(R.id.tvFecha1);
        tvFecha2 = findViewById(R.id.tvFecha2);
        tvFecha3 = findViewById(R.id.tvFecha3);

        tvTitulo1 = findViewById(R.id.tvTitulo1);
        tvTitulo2 = findViewById(R.id.tvTitulo2);
        tvTitulo3 = findViewById(R.id.tvTitulo3);

        tvEstado1 = findViewById(R.id.tvEstado1);
        tvEstado2 = findViewById(R.id.tvEstado2);
        tvEstado3 = findViewById(R.id.tvEstado3);

        // Cargamos las últimas entregas desde Firebase
        cargarUltimasEntregas();

        // ------------------- POPUP NOTIFICACIONES (CAMPANA) -------------------
        rvNotificaciones = findViewById(R.id.rvNotificaciones);
        rvNotificaciones.setLayoutManager(new LinearLayoutManager(this));
        notifAdapter = new NotificacionAdapter(notifList);
        rvNotificaciones.setAdapter(notifAdapter);

        cardNotificaciones = findViewById(R.id.cardNotificaciones);
        bell = findViewById(R.id.imageView9);       // icono de campana
        imgPointer = findViewById(R.id.imgNotifPointer);
        rowVerTodos = findViewById(R.id.rowVerTodos);

        // Click en la campana → mostrar/ocultar popup
        bell.setOnClickListener(v -> {
            if (cardNotificaciones.getVisibility() == View.VISIBLE) {
                cardNotificaciones.setVisibility(View.GONE);
                imgPointer.setVisibility(View.GONE);
            } else {
                cardNotificaciones.setVisibility(View.VISIBLE);
                imgPointer.setVisibility(View.VISIBLE);
            }
        });

        // Click en "Ver todos" → abre la pantalla completa de notificaciones
        rowVerTodos.setOnClickListener(v -> {
            Intent i = new Intent(InicioActivity.this, NotificacionActivity.class);
            startActivity(i);
        });

        // De momento, notificaciones de ejemplo (luego vendrán de Firebase)
        cargarNotifsDummy();
    }

    // =========================================================
    //              ÚLTIMAS ENTREGAS (FIREBASE)
    // =========================================================
    private void cargarUltimasEntregas() {
        db.collection("paquetes")
                // Idealmente ordenar por un campo "timestamp"
                .orderBy("fecha", Query.Direction.DESCENDING)
                .limit(3)
                .get()
                .addOnSuccessListener(this::mostrarUltimasEntregas)
                .addOnFailureListener(e ->
                        Log.e("InicioActivity", "Error cargando últimas entregas", e)
                );
    }

    private void mostrarUltimasEntregas(QuerySnapshot snapshot) {
        List<DocumentSnapshot> docs = snapshot.getDocuments();

        // Paquete 1
        if (docs.size() > 0) {
            DocumentSnapshot d = docs.get(0);
            String titulo = d.getString("titulo");
            String fecha = d.getString("fecha");
            String estado = d.getString("estado");

            ivEntrega1.setImageResource(obtenerIconoPorTitulo(titulo));
            tvTitulo1.setText(titulo != null ? titulo : "");
            tvFecha1.setText(fecha != null ? fecha : "");
            tvEstado1.setText(estado != null ? estado : "");
        }

        // Paquete 2
        if (docs.size() > 1) {
            DocumentSnapshot d = docs.get(1);
            String titulo = d.getString("titulo");
            String fecha = d.getString("fecha");
            String estado = d.getString("estado");

            ivEntrega2.setImageResource(obtenerIconoPorTitulo(titulo));
            tvTitulo2.setText(titulo != null ? titulo : "");
            tvFecha2.setText(fecha != null ? fecha : "");
            tvEstado2.setText(estado != null ? estado : "");
        }

        // Paquete 3
        if (docs.size() > 2) {
            DocumentSnapshot d = docs.get(2);
            String titulo = d.getString("titulo");
            String fecha = d.getString("fecha");
            String estado = d.getString("estado");

            ivEntrega3.setImageResource(obtenerIconoPorTitulo(titulo));
            tvTitulo3.setText(titulo != null ? titulo : "");
            tvFecha3.setText(fecha != null ? fecha : "");
            tvEstado3.setText(estado != null ? estado : "");
        }
    }

    // Devuelve icono según el texto del título
    private int obtenerIconoPorTitulo(String titulo) {
        if (titulo == null) return R.drawable.ic_package_amazon;

        String t = titulo.toLowerCase();

        if (t.contains("amazon")) {
            return R.drawable.ic_package_amazon;
        } else if (t.contains("correos")) {
            return R.drawable.ic_package_correos;
        } else if (t.contains("carta")) {
            return R.drawable.ic_package_carta;
        } else if (t.contains("zara")) {
            return R.drawable.caja_zara;
        }

        return R.drawable.ic_package_amazon;
    }

    // =========================================================
    //              NOTIFICACIONES (POPUP CAMPANA)
    // =========================================================
    // Método temporal para rellenar la lista con datos falsos
    private void cargarNotifsDummy() {
        notifList.clear();

        // Notificación de paquete NUEVO
        notifList.add(new Notificacion(
                "Nuevo paquete entregado",
                "Amazon - entregado hace 5 min",
                "5 min",
                "paquete",
                "nuevo"));

        // Notificación de paquete LEÍDO
        notifList.add(new Notificacion(
                "Paquete en camino",
                "Correos - llega hoy",
                "2 h",
                "paquete",
                "leido"));

        // Notificación de seguridad URGENTE
        notifList.add(new Notificacion(
                "Alerta sin autorización",
                "Caja forzada a las 23:39",
                "2 h",
                "seguridad",
                "urgente"));

        notifAdapter.notifyDataSetChanged();
    }
}
