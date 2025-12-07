package com.example.holdy;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

// Actividad para gestionar el perfil del usuario y su foto
public class PerfilActivity extends AppCompatActivity {

    // ---- Firebase ----
    private StorageReference storageRef;          // Raíz de Firebase Storage
    private FirebaseFirestore db;                 // Firestore para guardar la URL
    private FirebaseUser currentUser;             // Usuario activo

    // ---- Vistas ----
    private ImageView ivProfilePhoto;
    private ImageButton btnChangePhoto;

    private static final int RC_GALERIA = 1234;   // Código para abrir la galería

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.perfil);

        // Inicializar Firebase
        storageRef = FirebaseStorage.getInstance().getReference();
        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        // Referencias a las vistas
        ivProfilePhoto = findViewById(R.id.ivProfilePhoto);
        btnChangePhoto = findViewById(R.id.btnChangePhoto);

        // Botón para cambiar la foto → abrir galería
        btnChangePhoto.setOnClickListener(v -> abrirGaleria());

        // Cargar foto de perfil si existe en Firestore
        cargarFotoPerfil();
    }

    // ------------------------------------------------------------
    // ABRIR GALERÍA
    // ------------------------------------------------------------
    private void abrirGaleria() {
        Intent i = new Intent(Intent.ACTION_PICK);
        i.setType("image/*");
        startActivityForResult(i, RC_GALERIA);
    }

    // ------------------------------------------------------------
    // RECIBIR IMAGEN DE LA GALERÍA
    // ------------------------------------------------------------
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK && requestCode == RC_GALERIA) {

            Uri imagen = data.getData();

            if (imagen != null) {
                // Mostrar la foto seleccionada
                ivProfilePhoto.setImageURI(imagen);

                // Subir la foto a Firebase Storage
                subirFotoPerfil(imagen);
            }
        }
    }

    // ------------------------------------------------------------
    // SUBIR FOTO A FIREBASE STORAGE
    // ------------------------------------------------------------
    private void subirFotoPerfil(Uri fichero) {

        if (currentUser == null) {
            Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show();
            return;
        }

        // Nombre único: perfil/UID.jpg
        String ruta = "perfil/" + currentUser.getUid() + ".jpg";
        StorageReference refDestino = storageRef.child(ruta);

        UploadTask uploadTask = refDestino.putFile(fichero);

        uploadTask
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    return refDestino.getDownloadUrl();
                })
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {

                        Uri urlDescarga = task.getResult();

                        // Guardar URL en Firestore
                        db.collection("usuarios")
                                .document(currentUser.getUid())
                                .update("fotoPerfil", urlDescarga.toString())
                                .addOnSuccessListener(aVoid ->
                                        Toast.makeText(
                                                PerfilActivity.this,
                                                "Foto actualizada correctamente",
                                                Toast.LENGTH_SHORT
                                        ).show()
                                )
                                .addOnFailureListener(e ->
                                        Toast.makeText(
                                                PerfilActivity.this,
                                                "Error guardando URL en Firestore",
                                                Toast.LENGTH_SHORT
                                        ).show()
                                );

                    } else {
                        Toast.makeText(
                                PerfilActivity.this,
                                "Error subiendo la foto",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });
    }

    // ------------------------------------------------------------
    // CARGAR FOTO AL ABRIR EL PERFIL
    // ------------------------------------------------------------
    private void cargarFotoPerfil() {
        if (currentUser == null) return;

        db.collection("usuarios")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(doc -> {

                    if (doc.exists()) {
                        String urlFoto = doc.getString("fotoPerfil");

                        if (urlFoto != null && !urlFoto.isEmpty()) {

                            // Usamos Glide para mostrar la imagen
                            Glide.with(PerfilActivity.this)
                                    .load(urlFoto)
                                    .into(ivProfilePhoto);
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                PerfilActivity.this,
                                "Error cargando la foto",
                                Toast.LENGTH_SHORT
                        ).show()
                );
    }
}
