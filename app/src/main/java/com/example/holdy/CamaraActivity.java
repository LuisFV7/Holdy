package com.example.holdy;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class CamaraActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camara);

        // Bottom navigation
        ImageView navHome = findViewById(R.id.navHome);
        ImageView navCamara = findViewById(R.id.navCamara);
        ImageView navRight = findViewById(R.id.navRight);

        // Home → InicioActivity
        navHome.setOnClickListener(v -> {
            Intent i = new Intent(CamaraActivity.this, InicioActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(i);
            finish();
        });

        // Perfil → PerfilActivity
        navRight.setOnClickListener(v -> {
            Intent i = new Intent(CamaraActivity.this, PerfilActivity.class);
            startActivity(i);
        });

        // Cámara → ya estamos aquí (no hace nada)
        navCamara.setOnClickListener(v -> {
            // Nothing
        });
    }
}
