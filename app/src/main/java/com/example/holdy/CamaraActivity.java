package com.example.holdy;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;

import java.util.List;

public class CamaraActivity extends AppCompatActivity {

    private ImageView ivFotoCamara;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camara);

        // ---------------------------
        // Referencias
        // ---------------------------
        ivFotoCamara = findViewById(R.id.ivFotoCamara);

        // Botón MAPA (icono arriba)
        ImageButton btnMap = findViewById(R.id.btnMap);
        btnMap.setOnClickListener(v -> {
            Intent intent = new Intent(CamaraActivity.this, MapActivity.class);
            startActivity(intent);
        });

        // Botón "Abrir" -> lanza IA con la imagen actual del ImageView
        Button btnAbrir = findViewById(R.id.btnAbrir);
        btnAbrir.setOnClickListener(v -> ejecutarIAConImagenActual());

        // ---------------------------
        // Bottom navigation
        // ---------------------------
        ImageView navHome = findViewById(R.id.navHome);
        ImageView navCamara = findViewById(R.id.navCamara);
        ImageView navRight = findViewById(R.id.navRight);

        // Home → InicioActivity
        navHome.setOnClickListener(v -> {
            Intent i = new Intent(CamaraActivity.this, InicioActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(i);
            finish();
        });

        // Perfil → PerfilActivity
        navRight.setOnClickListener(v -> {
            Intent i = new Intent(CamaraActivity.this, PerfilActivity.class);
            startActivity(i);
        });

        // Cámara → ya estamos aquí
        navCamara.setOnClickListener(v -> {
            // Nothing
        });
    }

    private void ejecutarIAConImagenActual() {
        if (ivFotoCamara.getDrawable() == null) {
            Toast.makeText(this, "No hay imagen para analizar", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!(ivFotoCamara.getDrawable() instanceof BitmapDrawable)) {
            Toast.makeText(this, "Imagen no compatible para analizar", Toast.LENGTH_SHORT).show();
            return;
        }

        Bitmap bitmap = ((BitmapDrawable) ivFotoCamara.getDrawable()).getBitmap();
        if (bitmap == null) {
            Toast.makeText(this, "No se pudo obtener el bitmap", Toast.LENGTH_SHORT).show();
            return;
        }

        detectarPaqueteConMLKit(bitmap);
    }

    private void detectarPaqueteConMLKit(Bitmap bitmap) {

        InputImage image = InputImage.fromBitmap(bitmap, 0);

        ImageLabeler labeler = ImageLabeling.getClient(
                new ImageLabelerOptions.Builder()
                        .setConfidenceThreshold(0.60f)
                        .build()
        );

        labeler.process(image)
                .addOnSuccessListener(this::procesarLabels)
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error IA: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private void procesarLabels(List<ImageLabel> labels) {

        boolean hayPaquete = false;

        for (ImageLabel label : labels) {
            String texto = label.getText() != null ? label.getText().toLowerCase() : "";
            // labels típicos en inglés: box, carton, package
            if (texto.contains("box") || texto.contains("carton") || texto.contains("package")
                    || texto.contains("caja") || texto.contains("cartón") || texto.contains("carton")) {
                hayPaquete = true;
                break;
            }

        }

        if (hayPaquete) {
            Toast.makeText(this, " Posible paquete detectado por IA", Toast.LENGTH_LONG).show();
            // (Opcional) aquí podrías guardar un evento en Firestore
            // guardarEventoIA();
        } else {
            Toast.makeText(this, "No se detecta paquete en la imagen", Toast.LENGTH_SHORT).show();
        }
    }
}
