package com.example.holdy;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class NuevoPaqueteActivity extends AppCompatActivity {

    private EditText etTitulo, etEmpresa, etEstado, etFecha, etFotoUrl;;
    private Button btnGuardar;
    private ImageButton btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nuevo_paquete);

        etTitulo = findViewById(R.id.etTituloPaquete);
        etEmpresa = findViewById(R.id.etEmpresaPaquete);
        etEstado = findViewById(R.id.etEstadoPaquete);
        etFecha = findViewById(R.id.etFechaPaquete);
        etFotoUrl = findViewById(R.id.etFotoUrlPaquete);
        btnGuardar = findViewById(R.id.btnGuardarPaquete);
        btnBack = findViewById(R.id.btnBackNuevo);

        // Botón atrás
        btnBack.setOnClickListener(v -> finish());

        // Guardar paquete en Firestore
        btnGuardar.setOnClickListener(v -> guardarPaquete());
    }

    private void guardarPaquete() {

        String titulo = etTitulo.getText().toString().trim();
        String empresa = etEmpresa.getText().toString().trim();
        String estado = etEstado.getText().toString().trim();
        String fecha = etFecha.getText().toString().trim();
        String fotoUrl = etFotoUrl.getText().toString().trim();

        if (titulo.isEmpty()) {
            etTitulo.setError("Introduce un título");
            etTitulo.requestFocus();
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> paquete = new HashMap<>();
        paquete.put("titulo", titulo);
        paquete.put("empresa", empresa);
        paquete.put("estado", estado);
        paquete.put("fecha", fecha);
        paquete.put("fotoUrl", fotoUrl);
        paquete.put("manual", true);

        db.collection("paquetes")
                .add(paquete)
                .addOnSuccessListener(docRef -> {
                    Toast.makeText(this, "Paquete añadido", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error al guardar", Toast.LENGTH_SHORT).show();
                });
    }
}

