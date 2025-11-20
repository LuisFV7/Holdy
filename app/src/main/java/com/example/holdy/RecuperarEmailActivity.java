package com.example.holdy;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;

public class RecuperarEmailActivity extends AppCompatActivity {

    private EditText etEmailReset;
    private Button btnSendLink;
    private ImageView btnClose;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recuperar_email);

        // Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Vistas
        etEmailReset = findViewById(R.id.etEmailReset);
        btnSendLink = findViewById(R.id.btnSendLink);
        btnClose = findViewById(R.id.btnClose);

        // Botón cerrar / atrás
        btnClose.setOnClickListener(v -> finish());

        // Botón enviar enlace
        btnSendLink.setOnClickListener(v -> {
            String email = etEmailReset.getText().toString().trim();

            // Validaciones básicas
            if (email.isEmpty()) {
                etEmailReset.setError("Introduce tu email");
                etEmailReset.requestFocus();
                Toast.makeText(RecuperarEmailActivity.this,
                        "El email es obligatorio", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmailReset.setError("Email no válido");
                etEmailReset.requestFocus();
                return;
            }

            // Enviar correo de restablecimiento
            mAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(RecuperarEmailActivity.this,
                                    "Te hemos enviado un correo para restablecer la contraseña",
                                    Toast.LENGTH_LONG).show();
                            finish(); // vuelve al login
                        } else {
                            String msg = task.getException() != null
                                    ? task.getException().getMessage()
                                    : "Error al enviar el correo";
                            Toast.makeText(RecuperarEmailActivity.this,
                                    msg, Toast.LENGTH_LONG).show();
                        }
                    });
        });
    }
}
