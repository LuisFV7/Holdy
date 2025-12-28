package com.example.holdy;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NuevoPaqueteActivity extends AppCompatActivity {

    private static final int RC_FOTO = 1001;

    private EditText etTitulo, etEmpresa, etEstado, etFecha, etFotoUrl;
    private Button btnGuardar;
    private ImageButton btnBack, btnSelectFoto;

    private FirebaseFirestore db;
    private StorageReference storageRef;

    private Uri uriFotoSeleccionada = null; // foto elegida de la galería

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nuevo_paquete);

        etTitulo  = findViewById(R.id.etTituloPaquete);
        etEmpresa = findViewById(R.id.etEmpresaPaquete);
        etEstado  = findViewById(R.id.etEstadoPaquete);
        etFecha   = findViewById(R.id.etFechaPaquete);
        etFotoUrl = findViewById(R.id.etFotoUrlPaquete);

        btnGuardar    = findViewById(R.id.btnGuardarPaquete);
        btnBack       = findViewById(R.id.btnBackNuevo);
        btnSelectFoto = findViewById(R.id.btnSelectFoto);

        db = FirebaseFirestore.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference();

        // Botón atrás
        btnBack.setOnClickListener(v -> finish());

        // Botón seleccionar foto de galería
        btnSelectFoto.setOnClickListener(v -> abrirGaleria());

        // Guardar paquete
        btnGuardar.setOnClickListener(v -> guardarPaquete());
    }

    // --------- ABRIR GALERÍA ---------

    private void abrirGaleria() {
        Intent i = new Intent(Intent.ACTION_PICK);
        i.setType("image/*");
        startActivityForResult(i, RC_FOTO);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_FOTO && resultCode == RESULT_OK && data != null) {
            uriFotoSeleccionada = data.getData();
            if (uriFotoSeleccionada != null) {
                Toast.makeText(this, "Foto seleccionada", Toast.LENGTH_SHORT).show();
                // Opcional: texto para que sepas que viene de la galería
                etFotoUrl.setText("Foto seleccionada desde galería");
            }
        }
    }

    // --------- GUARDAR PAQUETE (con o sin foto) ---------

    private void guardarPaquete() {

        String titulo  = etTitulo.getText().toString().trim();
        String empresa = etEmpresa.getText().toString().trim();
        String estado  = etEstado.getText().toString().trim();
        String fecha   = etFecha.getText().toString().trim();
        String fotoUrlEscrita = etFotoUrl.getText().toString().trim(); // por si pegas URL manual

        if (titulo.isEmpty()) {
            etTitulo.setError("Introduce un título");
            etTitulo.requestFocus();
            return;
        }

        // Si el usuario ha seleccionado una foto de la galería, primero la subimos
        if (uriFotoSeleccionada != null) {
            subirFotoYGuardarPaquete(titulo, empresa, estado, fecha, fotoUrlEscrita);
        } else {
            // Sin foto de galería → usamos la URL escrita (si la hay) o null
            guardarPaqueteEnFirestore(
                    titulo,
                    empresa,
                    estado,
                    fecha,
                    fotoUrlEscrita.isEmpty() ? null : fotoUrlEscrita
            );
        }
    }

    // --------- SUBIR FOTO A STORAGE ---------

    private void subirFotoYGuardarPaquete(String titulo,
                                          String empresa,
                                          String estado,
                                          String fecha,
                                          String fotoUrlEscrita) {

        String nombreFichero = UUID.randomUUID().toString();
        StorageReference ref = storageRef.child("paquetes_fotos/" + nombreFichero + ".jpg");

        UploadTask uploadTask = ref.putFile(uriFotoSeleccionada);

        uploadTask
                .continueWithTask((Continuation<UploadTask.TaskSnapshot, Task<Uri>>) task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    return ref.getDownloadUrl();
                })
                .addOnSuccessListener(downloadUri -> {
                    String urlSubida = downloadUri.toString();
                    guardarPaqueteEnFirestore(titulo, empresa, estado, fecha, urlSubida);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error al subir la foto", Toast.LENGTH_SHORT).show();
                    // Si falla la subida, como plan B intentamos guardar con URL escrita (si la hay)
                    if (!fotoUrlEscrita.isEmpty()) {
                        guardarPaqueteEnFirestore(titulo, empresa, estado, fecha, fotoUrlEscrita);
                    }
                });
    }

    // --------- GUARDAR DOCUMENTO EN FIRESTORE ---------

    private void guardarPaqueteEnFirestore(String titulo,
                                           String empresa,
                                           String estado,
                                           String fecha,
                                           String fotoUrlFinal) {

        Map<String, Object> paquete = new HashMap<>();
        paquete.put("titulo", titulo);
        paquete.put("empresa", empresa);
        paquete.put("estado", estado);
        paquete.put("fecha", fecha);
        paquete.put("fotoUrl", fotoUrlFinal);  // puede ser null
        paquete.put("manual", true);

        db.collection("paquetes")
                .add(paquete)
                .addOnSuccessListener(docRef -> {
                    Toast.makeText(this, "Paquete añadido", Toast.LENGTH_SHORT).show();

                    //  Crear notificación asociada en Firestore
                    crearNotificacionPaquete(titulo, empresa, estado);

                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error al guardar", Toast.LENGTH_SHORT).show();
                });

    }

    private void crearNotificacionPaquete(String tituloPaquete,
                                          String empresa,
                                          String estadoPaquete) {

        String hora = "Ahora"; // luego si quieres usas un timestamp real

        // tipo = "paquete" para tus filtros
        // estado notificación = "nuevo"
        Notificacion notif = new Notificacion(
                "Nuevo paquete: " + tituloPaquete,
                (empresa == null || empresa.isEmpty())
                        ? "Estado: " + estadoPaquete
                        : empresa + " · " + estadoPaquete,
                hora,
                "paquete",
                "nuevo"
        );

        FirebaseFirestore.getInstance()
                .collection("notificaciones")
                .add(notif);
    }

}
