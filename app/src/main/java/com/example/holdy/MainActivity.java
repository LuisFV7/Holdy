package com.example.holdy;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MainActivity extends AppCompatActivity {

    private FloatingActionButton botonAcercaDe;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        botonAcercaDe = findViewById(R.id.botonAcercaDe);
        botonAcercaDe.setOnClickListener(v -> mensajeBotonAcercaDe());

    }

    private void mensajeBotonAcercaDe() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Acerca de Holdy")
                .setMessage("Buzón inteligente que recibe y protege tus paquetes, estés donde estés.")
                .setPositiveButton("Cerrar", (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }
}