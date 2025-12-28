package com.example.holdy;


import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class CambiarContrasenaPerfilActivity extends AppCompatActivity {

    private EditText etActual, etNueva, etConfirmar;
    private Button btnCambiar;
    private ImageView btnClose;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cambiar_contrasena_perfil);

        etActual    = findViewById(R.id.currentPassword);
        etNueva     = findViewById(R.id.passwordReset);
        etConfirmar = findViewById(R.id.confirmPassword);
        btnCambiar  = findViewById(R.id.btnSendReset);
        btnClose    = findViewById(R.id.btnClose);

        mAuth = FirebaseAuth.getInstance();

        btnClose.setOnClickListener(v -> finish());
        btnCambiar.setOnClickListener(v -> actualizarContrasena());
    }

    private void actualizarContrasena() {

        String actual    = etActual.getText().toString().trim();
        String nueva     = etNueva.getText().toString().trim();
        String confirmar = etConfirmar.getText().toString().trim();

        if (actual.isEmpty() || nueva.isEmpty() || confirmar.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show();
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

        String email = user.getEmail();
        if (email == null || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Tu cuenta no usa contraseña (probablemente Google).", Toast.LENGTH_LONG).show();
            return;
        }

        btnCambiar.setEnabled(false);

        AuthCredential credential = EmailAuthProvider.getCredential(email, actual);

        user.reauthenticate(credential)
                .addOnSuccessListener(unused -> user.updatePassword(nueva)
                        .addOnSuccessListener(u -> {
                            btnCambiar.setEnabled(true);
                            Toast.makeText(this, "Contraseña actualizada correctamente", Toast.LENGTH_LONG).show();
                            finish();
                        })
                        .addOnFailureListener(e -> {
                            btnCambiar.setEnabled(true);
                            Toast.makeText(this, "Error al cambiar contraseña: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        })
                )
                .addOnFailureListener(e -> {
                    btnCambiar.setEnabled(true);
                    Toast.makeText(this, "Contraseña actual incorrecta", Toast.LENGTH_LONG).show();
                });
    }
}
