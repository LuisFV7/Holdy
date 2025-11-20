package com.example.holdy;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class CambiarContrasenaPerfilActivity extends AppCompatActivity {

    private EditText etNueva, etConfirmar;
    private Button btnCambiar;
    private ImageView btnClose;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cambiar_contrasena_perfil);

        // IDs que SÍ existen en tu XML
        etNueva     = findViewById(R.id.passwordReset);
        etConfirmar = findViewById(R.id.confirmPassword);
        btnCambiar  = findViewById(R.id.btnSendReset);
        btnClose    = findViewById(R.id.btnClose);

        mAuth = FirebaseAuth.getInstance();

        btnClose.setOnClickListener(v -> finish());

        btnCambiar.setOnClickListener(v -> actualizarContrasena());
    }

    private void actualizarContrasena() {

        String nueva     = etNueva.getText().toString().trim();
        String confirmar = etConfirmar.getText().toString().trim();

        if (nueva.isEmpty() || confirmar.isEmpty()) {
            Toast.makeText(this, "Completa ambos campos", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!nueva.equals(confirmar)) {
            etConfirmar.setError("Las contraseñas no coinciden");
            return;
        }

        if (nueva.length() < 6) {
            etNueva.setError("Mínimo 6 caracteres");
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "No hay usuario logueado", Toast.LENGTH_SHORT).show();
            return;
        }

        user.updatePassword(nueva)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this,
                            "Contraseña actualizada correctamente",
                            Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Error al cambiar contraseña: " + e.getMessage(),
                                Toast.LENGTH_LONG).show()
                );
    }
}
