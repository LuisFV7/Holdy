package com.example.holdy;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class PreferenciasActivity extends AppCompatActivity {

    // ------------------ Notificaciones ------------------
    private Switch swNotifMaster, swNotifEntregas, swNotifApertura, swNotifSeguridad;

    // ------------------ Caja (Nombre) ------------------
    private LinearLayout rowBoxView, rowBoxEdit;
    private TextView tvBoxName;
    private EditText etBoxName;
    private ImageButton btnEditBox, btnSaveBox, btnCancelBox;

    // ------------------ Emergencia ------------------
    private LinearLayout rowEmView, rowEmEdit;
    private TextView tvEmName, tvEmPhone;
    private EditText etEmName, etEmPhone;
    private ImageButton btnEditEm, btnSaveEm, btnCancelEm;

    // ------------------ Updated (global) ------------------
    private TextView tvUpdatedGlobal;

    // ------------------ Firebase ------------------
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    // Cache (cancel için)
    private String cachedBoxName = "";
    private String cachedEmName = "";
    private String cachedEmPhone = "";

    private boolean isLoading = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferencias);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        bindViews();
        setupNotificationBehavior();
        setupBoxEditFlow();
        setupEmergencyEditFlow();

        loadAllFromFirestore();
    }

    private void bindViews() {
        // Notificaciones
        swNotifMaster = findViewById(R.id.swNotifMaster);
        swNotifEntregas = findViewById(R.id.swNotifEntregas);
        swNotifApertura = findViewById(R.id.swNotifApertura);
        swNotifSeguridad = findViewById(R.id.swNotifSeguridad);

        // Caja
        rowBoxView = findViewById(R.id.rowBoxView);
        rowBoxEdit = findViewById(R.id.rowBoxEdit);
        tvBoxName = findViewById(R.id.tvBoxName);
        etBoxName = findViewById(R.id.etBoxName);
        btnEditBox = findViewById(R.id.btnEditBox);
        btnSaveBox = findViewById(R.id.btnSaveBox);
        btnCancelBox = findViewById(R.id.btnCancelBox);

        // Emergencia
        rowEmView = findViewById(R.id.rowEmView);
        rowEmEdit = findViewById(R.id.rowEmEdit);
        tvEmName = findViewById(R.id.tvEmName);
        tvEmPhone = findViewById(R.id.tvEmPhone);
        etEmName = findViewById(R.id.etEmName);
        etEmPhone = findViewById(R.id.etEmPhone);
        btnEditEm = findViewById(R.id.btnEditEm);
        btnSaveEm = findViewById(R.id.btnSaveEm);
        btnCancelEm = findViewById(R.id.btnCancelEm);

        // Updated global (sayfanın en altı)
        tvUpdatedGlobal = findViewById(R.id.tvUpdatedGlobal);
    }

    private String uidOrNull() {
        return (auth.getCurrentUser() != null) ? auth.getCurrentUser().getUid() : null;
    }

    private void docRefOrToast(CallbackWithUid callback) {
        String uid = uidOrNull();
        if (uid == null) {
            toast("Usuario no autenticado");
            return;
        }
        callback.run(uid);
    }

    // ================== NOTIFICACIONES ==================

    private void setupNotificationBehavior() {

        swNotifMaster.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isLoading) return;

            setNotificationChildrenEnabled(isChecked);
            saveNotifToFirestore();
        });

        swNotifEntregas.setOnCheckedChangeListener((b, v) -> {
            if (isLoading) return;
            saveNotifToFirestore();
        });

        swNotifApertura.setOnCheckedChangeListener((b, v) -> {
            if (isLoading) return;
            saveNotifToFirestore();
        });

        swNotifSeguridad.setOnCheckedChangeListener((b, v) -> {
            if (isLoading) return;
            saveNotifToFirestore();
        });
    }

    private void setNotificationChildrenEnabled(boolean enabled) {
        swNotifEntregas.setEnabled(enabled);
        swNotifApertura.setEnabled(enabled);
        swNotifSeguridad.setEnabled(enabled);
    }

    private void saveNotifToFirestore() {
        docRefOrToast(uid -> {
            Map<String, Object> data = new HashMap<>();
            data.put("notif_master", swNotifMaster.isChecked());
            data.put("notif_entregas", swNotifEntregas.isChecked());
            data.put("notif_apertura", swNotifApertura.isChecked());
            data.put("notif_seguridad", swNotifSeguridad.isChecked());
            data.put("updatedAt", FieldValue.serverTimestamp());

            db.collection("user_preferences")
                    .document(uid)
                    .set(data, SetOptions.merge());
        });
    }

    // ================== CAJA ==================

    private void setupBoxEditFlow() {
        btnEditBox.setOnClickListener(v -> {
            etBoxName.setText(safeText(tvBoxName));
            showBoxEdit(true);
        });

        btnCancelBox.setOnClickListener(v -> {
            etBoxName.setText(cachedBoxName);
            showBoxEdit(false);
        });

        btnSaveBox.setOnClickListener(v -> {
            docRefOrToast(uid -> {
                String newName = etBoxName.getText().toString().trim();
                if (newName.isEmpty()) {
                    etBoxName.setError("Introduce un nombre");
                    return;
                }

                Map<String, Object> data = new HashMap<>();
                data.put("boxName", newName);
                data.put("updatedAt", FieldValue.serverTimestamp());

                db.collection("user_preferences")
                        .document(uid)
                        .set(data, SetOptions.merge())
                        .addOnSuccessListener(unused -> {
                            cachedBoxName = newName;
                            tvBoxName.setText(newName);
                            showBoxEdit(false);
                            toast("Guardado");
                        })
                        .addOnFailureListener(e -> toast("Error: " + e.getMessage()));
            });
        });
    }

    private void showBoxEdit(boolean editing) {
        rowBoxView.setVisibility(editing ? View.GONE : View.VISIBLE);
        rowBoxEdit.setVisibility(editing ? View.VISIBLE : View.GONE);
    }

    // ================== EMERGENCIA ==================

    private void setupEmergencyEditFlow() {
        btnEditEm.setOnClickListener(v -> {
            etEmName.setText(safeText(tvEmName));
            etEmPhone.setText(safeText(tvEmPhone));
            showEmEdit(true);
        });

        btnCancelEm.setOnClickListener(v -> {
            etEmName.setText(cachedEmName);
            etEmPhone.setText(cachedEmPhone);
            showEmEdit(false);
        });

        btnSaveEm.setOnClickListener(v -> {
            docRefOrToast(uid -> {
                String name = etEmName.getText().toString().trim();
                String phone = etEmPhone.getText().toString().trim();

                if (name.isEmpty()) {
                    etEmName.setError("Nombre requerido");
                    return;
                }
                if (phone.isEmpty()) {
                    etEmPhone.setError("Teléfono requerido");
                    return;
                }

                Map<String, Object> data = new HashMap<>();
                data.put("emergency_name", name);
                data.put("emergency_phone", phone);
                data.put("updatedAt", FieldValue.serverTimestamp());

                db.collection("user_preferences")
                        .document(uid)
                        .set(data, SetOptions.merge())
                        .addOnSuccessListener(unused -> {
                            cachedEmName = name;
                            cachedEmPhone = phone;
                            tvEmName.setText(name);
                            tvEmPhone.setText(phone);
                            showEmEdit(false);
                            toast("Guardado");
                        })
                        .addOnFailureListener(e -> toast("Error: " + e.getMessage()));
            });
        });
    }

    private void showEmEdit(boolean editing) {
        rowEmView.setVisibility(editing ? View.GONE : View.VISIBLE);
        rowEmEdit.setVisibility(editing ? View.VISIBLE : View.GONE);
    }

    // ================== LOAD ==================

    private void loadAllFromFirestore() {
        String uid = uidOrNull();
        if (uid == null) {
            setNotificationChildrenEnabled(swNotifMaster.isChecked());
            tvBoxName.setText("—");
            tvEmName.setText("—");
            tvEmPhone.setText("—");
            tvUpdatedGlobal.setText("Actualizado: —");
            return;
        }

        isLoading = true;

        db.collection("user_preferences").document(uid).get()
                .addOnSuccessListener(doc -> {
                    Boolean master = doc.getBoolean("notif_master");
                    Boolean entregas = doc.getBoolean("notif_entregas");
                    Boolean apertura = doc.getBoolean("notif_apertura");
                    Boolean seguridad = doc.getBoolean("notif_seguridad");

                    swNotifMaster.setChecked(master != null && master);
                    swNotifEntregas.setChecked(entregas != null && entregas);
                    swNotifApertura.setChecked(apertura != null && apertura);
                    swNotifSeguridad.setChecked(seguridad != null && seguridad);

                    setNotificationChildrenEnabled(swNotifMaster.isChecked());

                    cachedBoxName = doc.getString("boxName") != null ? doc.getString("boxName") : "";
                    tvBoxName.setText(cachedBoxName.isEmpty() ? "—" : cachedBoxName);

                    cachedEmName = doc.getString("emergency_name") != null ? doc.getString("emergency_name") : "";
                    cachedEmPhone = doc.getString("emergency_phone") != null ? doc.getString("emergency_phone") : "";

                    tvEmName.setText(cachedEmName.isEmpty() ? "—" : cachedEmName);
                    tvEmPhone.setText(cachedEmPhone.isEmpty() ? "—" : cachedEmPhone);

                    // ✅ updatedAt (Firestore) -> global text
                    Timestamp updated = doc.getTimestamp("updatedAt");
                    tvUpdatedGlobal.setText(formatUpdatedAt(updated));

                    isLoading = false;
                })
                .addOnFailureListener(e -> {
                    isLoading = false;
                    toast("Error: " + e.getMessage());
                });
    }

    // ================== Helpers ==================

    private String safeText(TextView tv) {
        if (tv == null || tv.getText() == null) return "";
        String s = tv.getText().toString();
        return s.equals("—") ? "" : s;
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    // ✅ Firestore console ile aynı timezone + düzgün tarih-saat
    private String formatUpdatedAt(Timestamp ts) {
        if (ts == null) return "Actualizado: —";

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("Europe/Madrid"));

        return "Actualizado: " + sdf.format(ts.toDate());
    }

    private interface CallbackWithUid {
        void run(String uid);
    }
}
