package com.example.holdy;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class EditarCuentaActivity extends AppCompatActivity {

    private EditText etNombre, etApellidos, etCorreo, etTelefono;
    private Button btnGuardar;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editar_cuenta);

        // Firebase
        auth = FirebaseAuth.getInstance();
        db   = FirebaseFirestore.getInstance();
        user = auth.getCurrentUser();

        if (user == null) {
            Toast.makeText(this, "No hay usuario conectado", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // UI referanses
        ImageButton btnBack = findViewById(R.id.btnBackEditarCuenta);
        etNombre    = findViewById(R.id.etNombre);
        etApellidos = findViewById(R.id.etApellidos);
        etCorreo    = findViewById(R.id.etCorreo);
        etTelefono  = findViewById(R.id.etTelefono);
        btnGuardar  = findViewById(R.id.btnGuardarCambios);

        // volver
        btnBack.setOnClickListener(v -> finish());

        cargarDatosUsuario();

        // guardar
        btnGuardar.setOnClickListener(v -> guardarCambios());
    }

    private void cargarDatosUsuario() {
        db.collection("usuarios")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(this::onDatosCargados)
                .addOnFailureListener(e ->
                        Toast.makeText(EditarCuentaActivity.this,
                                "Error al cargar datos: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }

    private void onDatosCargados(@NonNull DocumentSnapshot doc) {
        if (!doc.exists()) return;

        String nombre    = doc.getString("nombre");
        String apellidos = doc.getString("apellidos");
        String correo    = doc.getString("correo");
        String telefono  = doc.getString("telefono");

        // Si no hay correo en Firestore, tomarlo de Auth
        if (correo == null || correo.isEmpty()) {
            if (user.getEmail() != null) {
                correo = user.getEmail();
            }
        }

        if (nombre == null) nombre = "";
        if (apellidos == null) apellidos = "";
        if (correo == null) correo = "";
        if (telefono == null) telefono = "";

        etNombre.setText(nombre);
        etApellidos.setText(apellidos);
        etCorreo.setText(correo);
        etTelefono.setText(telefono);
    }

    private void guardarCambios() {
        String nombre    = etNombre.getText().toString().trim();
        String apellidos = etApellidos.getText().toString().trim();
        String correo    = etCorreo.getText().toString().trim();
        String telefono  = etTelefono.getText().toString().trim();

        if (nombre.isEmpty()) {
            etNombre.setError("El nombre es obligatorio");
            etNombre.requestFocus();
            return;
        }

        if (correo.isEmpty()) {
            etCorreo.setError("El correo es obligatorio");
            etCorreo.requestFocus();
            return;
        }

        // Campos que se escribirán en Firestore
        Map<String, Object> updates = new HashMap<>();
        updates.put("nombre", nombre);
        updates.put("apellidos", apellidos);
        updates.put("correo", correo);
        updates.put("telefono", telefono);

        db.collection("usuarios")
                .document(user.getUid())
                .update(updates)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(EditarCuentaActivity.this,
                            "Datos actualizados", Toast.LENGTH_SHORT).show();

                    finish();   // CuentaActivity'ye geri dön
                })
                .addOnFailureListener(e -> Toast.makeText(EditarCuentaActivity.this,
                        "Error al guardar: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }
}

