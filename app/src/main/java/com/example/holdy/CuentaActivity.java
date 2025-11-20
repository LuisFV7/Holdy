package com.example.holdy;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CuentaActivity extends AppCompatActivity {

    // Firebase
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    // UI
    private TextView tvNombre;
    private TextView tvApellidos;
    private TextView tvCorreo;
    private TextView tvTelefono;
    private TextView tvFechaRegistro;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cuenta);

        // ---------- Firebase ----------
        auth = FirebaseAuth.getInstance();
        db   = FirebaseFirestore.getInstance();

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            irAMain();
            return;
        }

        // ---------- View  ----------
        ImageButton btnMore = findViewById(R.id.btnMoreCuenta);
        ImageButton btnBack = findViewById(R.id.btnBackCuenta);

        tvNombre        = findViewById(R.id.tvNombreUsuario);
        tvApellidos     = findViewById(R.id.tvApellidosUsuario);
        tvCorreo        = findViewById(R.id.tvCorreoUsuario);
        tvTelefono      = findViewById(R.id.tvTelefonoUsuario);
        tvFechaRegistro = findViewById(R.id.tvFechaRegistro);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> onBackPressed());
        }

        if (btnMore != null) {
            btnMore.setOnClickListener(v -> {
                PopupMenu popupMenu = new PopupMenu(CuentaActivity.this, v);
                popupMenu.getMenuInflater().inflate(R.menu.menu_cuenta, popupMenu.getMenu());

                popupMenu.setOnMenuItemClickListener(item -> {
                    int id = item.getItemId();

                    if (id == R.id.action_editar) {
                        Intent intent = new Intent(CuentaActivity.this, EditarCuentaActivity.class);
                        startActivity(intent);
                        return true;

                    } else if (id == R.id.action_eliminar) {
                        new AlertDialog.Builder(CuentaActivity.this)
                                .setTitle("Eliminar cuenta")
                                .setMessage("¿Estás seguro de que quieres eliminar tu cuenta? Esta acción no se puede deshacer.")
                                .setPositiveButton("Eliminar", (dialog, which) -> eliminarCuenta())
                                .setNegativeButton("Cancelar", null)
                                .show();
                        return true;
                    }
                    return false;
                });

                popupMenu.show();
            });
        }


        // ---------- cargar datos ---------
        cargarDatosUsuario(user);
    }

    private void cargarDatosUsuario(FirebaseUser user) {
        db.collection("usuarios")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(this::onDatosCargados)
                .addOnFailureListener(e ->
                        Toast.makeText(CuentaActivity.this,
                                "Error al cargar datos: " + e.getLocalizedMessage(),
                                Toast.LENGTH_LONG).show()
                );
    }

    private void onDatosCargados(DocumentSnapshot doc) {

        if (!doc.exists()) {
            Toast.makeText(this,
                    "No se encontraron datos del usuario",
                    Toast.LENGTH_LONG).show();
            return;
        }

        String nombre    = doc.getString("nombre");
        String apellidos = doc.getString("apellidos");
        String correo    = doc.getString("correo");
        String telefono  = doc.getString("telefono");

        Long creadoEn    = doc.getLong("creadoEn");
        if (creadoEn == null) {
            creadoEn = doc.getLong("inicioSesion");
        }

        String fechaFormateada = "";
        if (creadoEn != null) {
            Date date = new Date(creadoEn);
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            fechaFormateada = sdf.format(date);
        }

        if (nombre == null) nombre = "";
        if (apellidos == null) apellidos = "";
        if (telefono == null) telefono = "";

        if (correo == null || correo.isEmpty()) {
            FirebaseUser user = auth.getCurrentUser();
            if (user != null && user.getEmail() != null) {
                correo = user.getEmail();
            } else {
                correo = "";
            }
        }

        tvNombre.setText(nombre);
        tvApellidos.setText(apellidos);
        tvCorreo.setText(correo);
        tvTelefono.setText(telefono);
        tvFechaRegistro.setText(fechaFormateada);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (auth != null && auth.getCurrentUser() != null) {
            cargarDatosUsuario(auth.getCurrentUser());
        }
    }

    // ---------- eliminar cuenta  ----------
    private void eliminarCuenta() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            irAMain();
            return;
        }

        String uid = user.getUid();


        db.collection("usuarios")
                .document(uid)
                .delete()
                .addOnSuccessListener(unused -> {
                    // Sonra FirebaseAuth kullanıcısını sil
                    user.delete()
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Toast.makeText(CuentaActivity.this,
                                            "Cuenta eliminada correctamente", Toast.LENGTH_SHORT).show();

                                    auth.signOut();
                                    irAMain();
                                } else {
                                    Toast.makeText(CuentaActivity.this,
                                            "No se pudo eliminar la cuenta (vuelve a iniciar sesión).",
                                            Toast.LENGTH_LONG).show();
                                }
                            });
                })
                .addOnFailureListener(e ->
                        Toast.makeText(CuentaActivity.this,
                                "Error al eliminar datos: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }

    private void irAMain() {

        Intent i = new Intent(CuentaActivity.this, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
        finish();
    }
}