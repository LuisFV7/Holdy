package com.example.holdy;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
//import com.google.firebase.auth.FirebaseAuth;
//import com.google.firebase.auth.FirebaseUser;
//import com.google.firebase.firestore.DocumentReference;
//import com.google.firebase.firestore.FirebaseFirestore;
//import com.google.firebase.storage.FirebaseStorage;
//import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;

public class PerfilActivity extends AppCompatActivity {

    private static final int PERMISO_CAMARA = 1;

    private ImageView foto;
    private ImageButton btnChangePhoto;
    private TextView tvUserName;
    private Button btnCerrarSesion;

    private LinearLayout btnCuentaLayout;
    private TextView tvCuenta;
    private ImageView ivCuentaArrow;

    private ActivityResultLauncher<Intent> galeriaLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;

    private Uri uriUltimaFoto;
    private String uriFotoPerfil;
    private boolean tieneFoto = false;

    // ----------------------------
    //FIREBASE
    // ----------------------------
    /*
    private FirebaseUser currentUser;
    private FirebaseFirestore db;
    private DocumentReference userDoc;
    private StorageReference photoRef;
    */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.perfil);

        foto            = findViewById(R.id.ivProfilePhoto);
        btnChangePhoto  = findViewById(R.id.btnChangePhoto);
        tvUserName      = findViewById(R.id.tvUserName);
        btnCerrarSesion = findViewById(R.id.btnCerrarSesion);

        tvCuenta        = findViewById(R.id.tvCuenta);
        ivCuentaArrow   = findViewById(R.id.ivCuentaArrow);
        btnCuentaLayout = findViewById(R.id.btnCuenta);

        btnChangePhoto.setImageResource(R.drawable.gridicons_add);

        /*
        // --------- FIREBASE (COMENTADO) -------------
        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            userDoc = db.collection("usuarios").document(currentUser.getUid());
            photoRef = FirebaseStorage.getInstance()
                    .getReference("profile_photos")
                    .child(currentUser.getUid() + ".jpg");

            cargarPerfilDeFirebase();
        } else {
            tvUserName.setText("Usuario");
        }
        */

        // ========= CLICK EN CUENTA =========
        View.OnClickListener irCuentaListener = v -> {
            Intent intent = new Intent(PerfilActivity.this, CuentaActivity.class);
            startActivity(intent);
        };

        btnCuentaLayout.setOnClickListener(irCuentaListener);
        tvCuenta.setOnClickListener(irCuentaListener);
        ivCuentaArrow.setOnClickListener(irCuentaListener);

        // ========= AJUSTES =========
        ImageButton btnAjustes = findViewById(R.id.btnAjustes);
        btnAjustes.setOnClickListener(v -> {
            Intent i = new Intent(PerfilActivity.this, CambiarContrasenaPerfilActivity.class);
            startActivity(i);
        });

        // ===== GALERÍA =====
        galeriaLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK
                            && result.getData() != null
                            && result.getData().getData() != null) {

                        Uri uri = result.getData().getData();

                        try {
                            getContentResolver().takePersistableUriPermission(
                                    uri,
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                            );
                        } catch (SecurityException ignored) {}

                        uriFotoPerfil = uri.toString();

                        //mostrar dentro del círculo
                        ponerFoto(foto, uriFotoPerfil);

                        tieneFoto = true;
                        btnChangePhoto.setImageResource(R.drawable.edit);

                        //Firebase comentado
                        //uploadFotoAFirebase(uri);

                    } else {
                        Toast.makeText(this,
                                "Foto no cargada",
                                Toast.LENGTH_LONG).show();
                    }
                });

        // ===== CÁMARA =====
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        if (uriUltimaFoto != null) {

                            uriFotoPerfil = uriUltimaFoto.toString();

                            //mostrar dentro del círculo
                            ponerFoto(foto, uriFotoPerfil);

                            tieneFoto = true;
                            btnChangePhoto.setImageResource(R.drawable.edit);

                            //Firebase comentado
                            //uploadFotoAFirebase(uriUltimaFoto);
                        }
                    } else {
                        Toast.makeText(this,
                                "Foto no tomada",
                                Toast.LENGTH_SHORT).show();
                    }
                });

        // ===== Cambiar foto =====
        btnChangePhoto.setOnClickListener(v -> {
            if (tieneFoto) {
                mostrarBottomSheetFotoExistente();
            } else {
                mostrarBottomSheetElegirOrigen();
            }
        });

        // ===== Cerrar sesión =====
        btnCerrarSesion.setOnClickListener(v -> mostrarDialogoCerrarSesion());
    }

    /*
    @Override
    protected void onResume() {
        super.onResume();
        if (currentUser != null) cargarPerfilDeFirebase();
    }
    */

    // ----------- BOTTON SHEETS -------------

    private void mostrarBottomSheetElegirOrigen() {
        com.google.android.material.bottomsheet.BottomSheetDialog dialog =
                new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_origen_foto, null);
        dialog.setContentView(view);

        TextView opcionGaleria = view.findViewById(R.id.opcionGaleria);
        TextView opcionCamara  = view.findViewById(R.id.opcionCamara);

        opcionGaleria.setOnClickListener(v -> {
            dialog.dismiss();
            abrirGaleria();
        });

        opcionCamara.setOnClickListener(v -> {
            dialog.dismiss();
            tomarFoto();
        });

        dialog.show();
    }

    private void mostrarBottomSheetFotoExistente() {
        com.google.android.material.bottomsheet.BottomSheetDialog dialog =
                new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_foto_existente, null);
        dialog.setContentView(view);

        TextView opcionCambiar  = view.findViewById(R.id.opcionCambiarFoto);
        TextView opcionEliminar = view.findViewById(R.id.opcionEliminarFoto);

        opcionCambiar.setOnClickListener(v -> {
            dialog.dismiss();
            mostrarBottomSheetElegirOrigen();
        });

        opcionEliminar.setOnClickListener(v -> {
            dialog.dismiss();
            eliminarFoto();
        });

        dialog.show();
    }

    private void abrirGaleria() {
        Intent intent = new Intent(
                Intent.ACTION_OPEN_DOCUMENT,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        );
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        galeriaLauncher.launch(intent);
    }

    private void tomarFoto() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                ContextCompat.checkSelfPermission(this,
                        android.Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{android.Manifest.permission.CAMERA},
                    PERMISO_CAMARA
            );
            return;
        }

        try {
            File fotoFile = File.createTempFile(
                    "img_" + (System.currentTimeMillis() / 1000),
                    ".jpg",
                    getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            );

            uriUltimaFoto = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    fotoFile
            );

            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uriUltimaFoto);
            cameraLauncher.launch(intent);

        } catch (IOException e) {
            Toast.makeText(this,
                    "Error al crear fichero de imagen",
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISO_CAMARA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                tomarFoto();
            } else {
                Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void eliminarFoto() {
        uriFotoPerfil = null;
        tieneFoto = false;

        foto.setImageResource(R.drawable.ic_perfil);
        btnChangePhoto.setImageResource(R.drawable.gridicons_add);

        /*
        // 🔵 Firebase comentado
        if (userDoc != null) {
            userDoc.update("fotoPerfil", null);
        }
        if (photoRef != null) {
            photoRef.delete();
        }
        */
    }

    // 🔥 Mostrar foto en el círculo
    private void ponerFoto(ImageView imageView, String uri) {
        if (uri != null) {
            Glide.with(this)
                    .load(Uri.parse(uri))
                    .circleCrop()
                    .into(imageView);
        } else {
            imageView.setImageResource(R.drawable.ic_perfil);
        }
    }

    /*
    //---------------------- SUBIR FOTO A FIREBASE (COMENTADO) ----------------------
    private void uploadFotoAFirebase(Uri localUri) { ... }
    */

    private void mostrarDialogoCerrarSesion() {

        // Firebase no se usa ya, pero dejo el código comentado
        /*
        FirebaseAuth.getInstance().signOut();
        */

        new AlertDialog.Builder(this)
                .setTitle("Cerrar sesión")
                .setMessage("¿Estás seguro de que quieres cerrar sesión?")
                .setPositiveButton("Cerrar sesión", (dialog, which) -> {

                    Intent intent = new Intent(PerfilActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                            | Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    public void irAInicio(View view) {
        startActivity(new Intent(this, InicioActivity.class));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}
