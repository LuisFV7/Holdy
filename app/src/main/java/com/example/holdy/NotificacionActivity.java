package com.example.holdy;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class NotificacionActivity extends AppCompatActivity {

    private RecyclerView rvNotificacionesFull;
    private FirebaseFirestore db;
    private ListenerRegistration listenerNotifs;
    private ListenerRegistration listenerPrefs;
    private NotificacionAdapter notifAdapter;
    private static final int REQ_NOTIF = 1001;

    private final List<Notificacion> listaOriginal = new ArrayList<>();
    private final List<Notificacion> listaFiltrada = new ArrayList<>();

    private Button btnFiltroTodas, btnFiltroPaquetes, btnFiltroSeguridad;
    private EditText edtBuscarNotif;

    private String filtroActual = "todas";
    private String textoBusqueda = "";

    private static final String CHANNEL_ID = "holdy_notifs";
    private boolean primeraCarga = true; // para no notificar todo al entrar

    // ===== Preferencias cacheadas (default ON si no existen) =====
    private boolean prefMaster = true;
    private boolean prefEntregas = true;   // controla "paquete"
    private boolean prefApertura = true;   // por si lo usas luego
    private boolean prefSeguridad = true;  // controla "seguridad" / urgente

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notificacion);

        createNotifChannel();

        rvNotificacionesFull = findViewById(R.id.rvNotificacionesFull);
        rvNotificacionesFull.setLayoutManager(new LinearLayoutManager(this));
        notifAdapter = new NotificacionAdapter(listaFiltrada);
        rvNotificacionesFull.setAdapter(notifAdapter);

        ImageView imgBack = findViewById(R.id.imgBackNotif);
        btnFiltroTodas = findViewById(R.id.btnFiltroTodas);
        btnFiltroPaquetes = findViewById(R.id.btnFiltroPaquetes);
        btnFiltroSeguridad = findViewById(R.id.btnFiltroSeguridad);
        edtBuscarNotif = findViewById(R.id.edtBuscarNotif);

        imgBack.setOnClickListener(v -> finish());

        pedirPermisoNotificacionesSiHaceFalta();

        btnFiltroTodas.setOnClickListener(v -> {
            filtroActual = "todas";
            aplicarFiltro();
            actualizarEstilosBotones();
        });

        btnFiltroPaquetes.setOnClickListener(v -> {
            filtroActual = "paquetes";
            aplicarFiltro();
            actualizarEstilosBotones();
        });

        btnFiltroSeguridad.setOnClickListener(v -> {
            filtroActual = "seguridad";
            aplicarFiltro();
            actualizarEstilosBotones();
        });

        edtBuscarNotif.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                textoBusqueda = s.toString().toLowerCase().trim();
                aplicarFiltro();
            }
        });

        db = FirebaseFirestore.getInstance();

        // 1) Escuchar preferencias del usuario (master / entregas / seguridad…)
        escucharPreferenciasUsuario();

        // 2) Escuchar notificaciones en tiempo real
        escucharNotifsTiempoReal();

        actualizarEstilosBotones();
    }

    private void escucharPreferenciasUsuario() {
        String uid = (FirebaseAuth.getInstance().getCurrentUser() != null)
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        if (uid == null) return;

        if (listenerPrefs != null) listenerPrefs.remove();

        listenerPrefs = db.collection("user_preferences")
                .document(uid)
                .addSnapshotListener((doc, e) -> {
                    if (e != null || doc == null || !doc.exists()) return;

                    Boolean m = doc.getBoolean("notif_master");
                    Boolean en = doc.getBoolean("notif_entregas");
                    Boolean ap = doc.getBoolean("notif_apertura");
                    Boolean se = doc.getBoolean("notif_seguridad");

                    // defaults true si no está el campo
                    prefMaster = (m == null) || m;
                    prefEntregas = (en == null) || en;
                    prefApertura = (ap == null) || ap;
                    prefSeguridad = (se == null) || se;
                });
    }

    private void escucharNotifsTiempoReal() {
        if (listenerNotifs != null) listenerNotifs.remove();

        listenerNotifs = db.collection("notificaciones")
                .addSnapshotListener((snapshot, e) -> {

                    if (e != null) {
                        android.util.Log.e("NotifActivity", "ERROR Firestore", e);
                        return;
                    }
                    if (snapshot == null) {
                        android.util.Log.d("NotifActivity", "Snapshot NULL");
                        return;
                    }

                    // Notificar SOLO las nuevas (ADDED) y NO en la primera carga
                    if (!primeraCarga) {
                        for (DocumentChange dc : snapshot.getDocumentChanges()) {
                            if (dc.getType() == DocumentChange.Type.ADDED) {
                                Notificacion n = dc.getDocument().toObject(Notificacion.class);
                                if (n != null) maybeShowSystemNotification(n);
                            }
                        }
                    }

                    // Reconstruimos lista para Recycler (como ya hacías)
                    listaOriginal.clear();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : snapshot.getDocuments()) {
                        Notificacion n = doc.toObject(Notificacion.class);
                        if (n != null) listaOriginal.add(n);
                    }

                    aplicarFiltro();

                    if (primeraCarga) primeraCarga = false;
                });
    }

    private void createNotifChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Notificaciones Holdy",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Avisos de paquetes y seguridad");

            NotificationManager nm = getSystemService(NotificationManager.class);
            nm.createNotificationChannel(channel);
        }
    }

    //  Decide si se permite según PreferenciasActivity (Firestore)
    private void maybeShowSystemNotification(Notificacion n) {
        if (!prefMaster) return; // master OFF => nunca

        String tipo = (n.getTipo() != null) ? n.getTipo().toLowerCase() : "";
        String estado = (n.getEstado() != null) ? n.getEstado().toLowerCase() : "";

        // Paquetes: en tu app el tipo real suele ser "paquete"
        if ("paquete".equals(tipo)) {
            if (!prefEntregas) return;
        }

        // Seguridad: tipo "seguridad" o estado "urgente"
        if ("seguridad".equals(tipo) || "urgente".equals(estado)) {
            if (!prefSeguridad) return;
        }

        // Si algún día usas "apertura"
        if ("apertura".equals(tipo)) {
            if (!prefApertura) return;
        }

        showSystemNotification(n);
    }

    private void showSystemNotification(Notificacion n) {
        Intent intent = new Intent(this, NotificacionActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pi = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        int iconRes = R.drawable.paquete;
        if ("seguridad".equalsIgnoreCase(n.getTipo())) iconRes = R.drawable.urgente;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(iconRes)
                .setContentTitle(n.getTitulo() != null ? n.getTitulo() : "Holdy")
                .setContentText(n.getMensaje() != null ? n.getMensaje() : "")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pi)
                .setAutoCancel(true);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) {
                return; // permiso denegado → no notifico
            }
        }

        try {
            NotificationManagerCompat.from(this)
                    .notify((int) System.currentTimeMillis(), builder.build());
        } catch (SecurityException ignored) {}
    }



    private void aplicarFiltro() {
        listaFiltrada.clear();

        for (Notificacion n : listaOriginal) {
            String tipo = n.getTipo() != null ? n.getTipo().toLowerCase() : "";
            String estado = n.getEstado() != null ? n.getEstado().toLowerCase() : "";
            String titulo = n.getTitulo() != null ? n.getTitulo().toLowerCase() : "";
            String mensaje = n.getMensaje() != null ? n.getMensaje().toLowerCase() : "";

            boolean pasaFiltroTipo;

            switch (filtroActual) {
                case "paquetes":
                    pasaFiltroTipo = "paquete".equals(tipo);
                    break;
                case "seguridad":
                    pasaFiltroTipo = "seguridad".equals(tipo) || "urgente".equals(estado);
                    break;
                case "todas":
                default:
                    pasaFiltroTipo = true;
                    break;
            }

            if (!pasaFiltroTipo) continue;

            if (!textoBusqueda.isEmpty()) {
                boolean coincide = titulo.contains(textoBusqueda) || mensaje.contains(textoBusqueda);
                if (!coincide) continue;
            }

            listaFiltrada.add(n);
        }

        notifAdapter.notifyDataSetChanged();
    }

    private void actualizarEstilosBotones() {
        int colorActivoBg = Color.parseColor("#4739C6");
        int colorInactivoBg = Color.parseColor("#D6D6F8");
        int colorTextoActivo = Color.WHITE;
        int colorTextoInactivo = Color.parseColor("#23295C");

        if ("todas".equals(filtroActual)) activarBoton(btnFiltroTodas, colorActivoBg, colorTextoActivo);
        else desactivarBoton(btnFiltroTodas, colorInactivoBg, colorTextoInactivo);

        if ("paquetes".equals(filtroActual)) activarBoton(btnFiltroPaquetes, colorActivoBg, colorTextoActivo);
        else desactivarBoton(btnFiltroPaquetes, colorInactivoBg, colorTextoInactivo);

        if ("seguridad".equals(filtroActual)) activarBoton(btnFiltroSeguridad, colorActivoBg, colorTextoActivo);
        else desactivarBoton(btnFiltroSeguridad, colorInactivoBg, colorTextoInactivo);
    }

    private void activarBoton(Button btn, int colorBg, int colorTexto) {
        btn.setBackgroundTintList(ColorStateList.valueOf(colorBg));
        btn.setTextColor(colorTexto);
    }

    private void desactivarBoton(Button btn, int colorBg, int colorTexto) {
        btn.setBackgroundTintList(ColorStateList.valueOf(colorBg));
        btn.setTextColor(colorTexto);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listenerNotifs != null) {
            listenerNotifs.remove();
            listenerNotifs = null;
        }
        if (listenerPrefs != null) {
            listenerPrefs.remove();
            listenerPrefs = null;
        }
    }

    private void pedirPermisoNotificacionesSiHaceFalta() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQ_NOTIF
                );
            }
        }
    }
}
