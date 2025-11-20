package com.example.holdy;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.List;

public class InicioActivity extends AppCompatActivity {

    private FirebaseFirestore db;

    // Vistas de últimas entregas
    private ImageView ivEntrega1, ivEntrega2, ivEntrega3;
    private TextView tvFecha1, tvFecha2, tvFecha3;
    private TextView tvTitulo1, tvTitulo2, tvTitulo3;
    private TextView tvEstado1, tvEstado2, tvEstado3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.inicio);

        db = FirebaseFirestore.getInstance();

        // Navegación barra inferior
        ImageView navRight = findViewById(R.id.navRight);
        navRight.setOnClickListener(v -> {
            Intent intent = new Intent(InicioActivity.this, PerfilActivity.class);
            startActivity(intent);
        });

        ImageView navHome = findViewById(R.id.navHome);
        navHome.setOnClickListener(v -> {
            // estás ya en inicio, pero por si luego quieres reiniciar o algo
        });

        ImageView navCamara = findViewById(R.id.navCamara);
        navCamara.setOnClickListener(v -> {
            // Aquí más adelante puedes abrir la cámara
        });

        // Card de últimas entregas → abre MisPaquetesActivity
        CardView cardEntregas = findViewById(R.id.cardEntregas);
        cardEntregas.setOnClickListener(v -> {
            Intent intent = new Intent(InicioActivity.this, MisPaquetesActivity.class);
            startActivity(intent);
        });

        // Botón "Ver detalles" (tarjeta morada) → también a MisPaquetesActivity
        Button btnVerDetalles = findViewById(R.id.btnVerDetalles);
        btnVerDetalles.setOnClickListener(v -> {
            Intent intent = new Intent(InicioActivity.this, MisPaquetesActivity.class);
            startActivity(intent);
        });



        // Referencias a las vistas de las 3 entregas
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

        // Cargar últimas entregas desde Firebase
        cargarUltimasEntregas();
    }

    private void cargarUltimasEntregas() {
        db.collection("paquetes")
                // Si pusieras un campo "timestamp" en Firestore sería mejor ordenar por eso.
                // De momento ordenamos por "fecha" descendente asumiendo formato coherente.
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
}
