package com.example.holdy;

import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.holdy.R;
import com.google.firebase.auth.FirebaseAuth;

public class RecuperarEmailActivity extends AppCompatActivity {

    private EditText etEmailReset;
    private Button btnSendLink, btnBackLogin;
    private ImageView btnClose;
    private FirebaseAuth mAuth;

    private ConstraintLayout layoutForm, layoutSent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recuperar_email);

        mAuth = FirebaseAuth.getInstance();

        etEmailReset = findViewById(R.id.etEmailReset);
        btnSendLink = findViewById(R.id.btnSendLink);
        btnClose = findViewById(R.id.btnClose);

        layoutForm = findViewById(R.id.layoutForm);
        layoutSent = findViewById(R.id.layoutSent);
        btnBackLogin = findViewById(R.id.btnBackLogin);

        btnClose.setOnClickListener(v -> finish());
        btnBackLogin.setOnClickListener(v -> finish());

        btnSendLink.setOnClickListener(v -> {
            String email = etEmailReset.getText().toString().trim();

            if (email.isEmpty()) {
                etEmailReset.setError("Introduce tu email");
                etEmailReset.requestFocus();
                return;
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmailReset.setError("Email no válido");
                etEmailReset.requestFocus();
                return;
            }

            btnSendLink.setEnabled(false);
            btnSendLink.setText("Enviando...");

            mAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                        btnSendLink.setEnabled(true);
                        btnSendLink.setText("Enviar enlace");

                        if (task.isSuccessful()) {
                            // Mostrar "pantalla enviado"
                            layoutForm.setVisibility(View.GONE);
                            layoutSent.setVisibility(View.VISIBLE);
                            etEmailReset.setText(""); // opcional
                        } else {
                            String msg = task.getException() != null
                                    ? task.getException().getMessage()
                                    : "Error al enviar el correo";
                            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                        }
                    });
        });
    }
}
