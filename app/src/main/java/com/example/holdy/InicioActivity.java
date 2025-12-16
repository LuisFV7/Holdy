package com.example.holdy;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

// Pantalla de inicio: tarjeta morada, últimas entregas (paquetes),
// popup de notificaciones y alerta grande con sonido para eventos de seguridad.
public class InicioActivity extends AppCompatActivity {

    // ------------------- FIREBASE -------------------
    private FirebaseFirestore db;

    // ------------------- ÚLTIMAS ENTREGAS (PAQUETES) -------------------
    private ImageView ivEntrega1, ivEntrega2, ivEntrega3;
    private TextView tvFecha1, tvFecha2, tvFecha3;
    private TextView tvTitulo1, tvTitulo2, tvTitulo3;
    private TextView tvEstado1, tvEstado2, tvEstado3;

    // ------------------- POPUP NOTIFICACIONES (EVENTOS) -------------------
    private RecyclerView rvNotificaciones;
    private NotificacionAdapter notifAdapter;
    private final List<Notificacion> notifList = new ArrayList<>();

    private CardView cardNotificaciones;
    private ImageView bell;
    private ImageView imgPointer;
    private LinearLayout rowVerTodos;

    // ------------------- ALERTA GRANDE + SONIDO -------------------
    // Guardamos el timestamp (en ms) del último evento procesado
    private long ultimoEventoMs = 0;
    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.inicio);

        // ----------- Firebase ----------
        db = FirebaseFirestore.getInstance();

        // ----------- NAV BAR INFERIOR ----------
        ImageView navRight = findViewById(R.id.navRight);
        navRight.setOnClickListener(v -> {
            Intent intent = new Intent(InicioActivity.this, PerfilActivity.class);
            startActivity(intent);
        });

        ImageView navHome = findViewById(R.id.navHome);
        navHome.setOnClickListener(v -> {
            // Ya estás en inicio, podrías refrescar si quisieras
        });

        ImageView navCamara = findViewById(R.id.navCamara);
        navCamara.setOnClickListener(v -> {
            Intent intent = new Intent(InicioActivity.this, CamaraActivity.class);
            startActivity(intent);
        });

        // Card de "Últimas entregas" → abre MisPaquetesActivity
        CardView cardEntregas = findViewById(R.id.cardEntregas);
        cardEntregas.setOnClickListener(v -> {
            Intent intent = new Intent(InicioActivity.this, MisPaquetesActivity.class);
            startActivity(intent);
        });

        // Botón "Ver detalles" de la tarjeta morada → también MisPaquetesActivity
        Button btnVerDetalles = findViewById(R.id.btnVerDetalles);
        btnVerDetalles.setOnClickListener(v -> {
            Intent intent = new Intent(InicioActivity.this, MisPaquetesActivity.class);
            startActivity(intent);
        });

        // ----------- REFERENCIAS ÚLTIMAS ENTREGAS ----------
        ivEntrega1 = findViewById(R.id.ivEntrega1);
        ivEntrega2 = findViewById(R.id.ivEntrega2);
        ivEntrega3 = findViewById(R.id.ivEntrega3);

        tvFecha1 = findViewById(R.id.tvFecha1);
        tvFecha2 = findViewById(R.id.tvFecha2);
        tvFecha3 = findViewById(R.id.tvFecha3);

        tvTitulo1 = findViewById(R.id.tvTitulo1);
        tvTitulo2 = findViewById(R.id.tvTitulo2);
        tvTitulo3 = findViewById(R.id.tvTitulo3);

        tvEstado1 = findViewById(R.id.tvEstado1);
        tvEstado2 = findViewById(R.id.tvEstado2);
        tvEstado3 = findViewById(R.id.tvEstado3);

        // Cargamos las últimas 3 entregas desde la colección "paquetes"
        cargarUltimasEntregas();

        // ----------- POPUP NOTIFICACIONES (EVENTOS) ----------
        rvNotificaciones = findViewById(R.id.rvNotificaciones);
        rvNotificaciones.setLayoutManager(new LinearLayoutManager(this));
        notifAdapter = new NotificacionAdapter(notifList);
        rvNotificaciones.setAdapter(notifAdapter);

        cardNotificaciones = findViewById(R.id.cardNotificaciones);
        bell = findViewById(R.id.imageView9);       // icono campana
        imgPointer = findViewById(R.id.imgNotifPointer);
        rowVerTodos = findViewById(R.id.rowVerTodos);

        // Click en la campana → mostrar / ocultar popup
        bell.setOnClickListener(v -> {
            if (cardNotificaciones.getVisibility() == View.VISIBLE) {
                cardNotificaciones.setVisibility(View.GONE);
                imgPointer.setVisibility(View.GONE);
            } else {
                cardNotificaciones.setVisibility(View.VISIBLE);
                imgPointer.setVisibility(View.VISIBLE);
            }
        });

        // "Ver todos" → ir a la pantalla completa de notificaciones
        rowVerTodos.setOnClickListener(v -> {
            Intent i = new Intent(InicioActivity.this, NotificacionActivity.class);
            startActivity(i);
        });

        // Escuchar en tiempo real la colección "eventos" (seguridad urgente)
        escucharEventosPopup();
    }

    // =========================================================
    //              ÚLTIMAS ENTREGAS (COLECCIÓN "paquetes")
    // =========================================================
    private void cargarUltimasEntregas() {
        db.collection("paquetes")
                // Idealmente: ordenar por un campo timestamp. De momento usamos "fecha".
                .orderBy("fecha", Query.Direction.DESCENDING)
                .limit(3)
                .get()
                .addOnSuccessListener(this::mostrarUltimasEntregas)
                .addOnFailureListener(e ->
                        Log.e("InicioActivity", "Error cargando últimas entregas", e)
                );
    }

    private void mostrarUltimasEntregas(QuerySnapshot snapshot) {
        List<DocumentSnapshot> docs = snapshot.getDocuments();

        // Paquete 1
        if (docs.size() > 0) {
            DocumentSnapshot d = docs.get(0);
            String titulo = d.getString("titulo");
            String fecha = d.getString("fecha");
            String estado = d.getString("estado");

            ivEntrega1.setImageResource(obtenerIconoPorTitulo(titulo));
            tvTitulo1.setText(titulo != null ? titulo : "");
            tvFecha1.setText(fecha != null ? fecha : "");
            tvEstado1.setText(estado != null ? estado : "");
        }

        // Paquete 2
        if (docs.size() > 1) {
            DocumentSnapshot d = docs.get(1);
            String titulo = d.getString("titulo");
            String fecha = d.getString("fecha");
            String estado = d.getString("estado");

            ivEntrega2.setImageResource(obtenerIconoPorTitulo(titulo));
            tvTitulo2.setText(titulo != null ? titulo : "");
            tvFecha2.setText(fecha != null ? fecha : "");
            tvEstado2.setText(estado != null ? estado : "");
        }

        // Paquete 3
        if (docs.size() > 2) {
            DocumentSnapshot d = docs.get(2);
            String titulo = d.getString("titulo");
            String fecha = d.getString("fecha");
            String estado = d.getString("estado");

            ivEntrega3.setImageResource(obtenerIconoPorTitulo(titulo));
            tvTitulo3.setText(titulo != null ? titulo : "");
            tvFecha3.setText(fecha != null ? fecha : "");
            tvEstado3.setText(estado != null ? estado : "");
        }
    }

    // Devuelve un icono según el título del paquete
    private int obtenerIconoPorTitulo(String titulo) {
        if (titulo == null) return R.drawable.ic_package_amazon;

        String t = titulo.toLowerCase();

        if (t.contains("amazon")) {
            return R.drawable.ic_package_amazon;
        } else if (t.contains("correos")) {
            return R.drawable.ic_package_correos;
        } else if (t.contains("carta")) {
            return R.drawable.ic_package_carta;
        } else if (t.contains("zara")) {
            return R.drawable.caja_zara;
        }

        return R.drawable.ic_package_amazon;
    }

    // =========================================================
    //              EVENTOS DE SEGURIDAD (POPUP CAMPANA)
    //              + disparar alerta grande con sonido
    // =========================================================
    private void escucharEventosPopup() {
        db.collection("eventos")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(3) // solo las 3 últimas alertas en el popup
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("InicioActivity", "Error escuchando eventos", error);
                        return;
                    }

                    notifList.clear();

                    long maxTimestampMs = ultimoEventoMs; // para detectar nuevos eventos

                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            String buzonId = doc.getString("buzonId");
                            String mensaje = doc.getString("mensaje");

                            // ⚠️ Leer timestamp de forma SEGURA (puede ser Timestamp o número)
                            Timestamp ts = leerTimestampSeguro(doc, "timestamp");

                            long tsMs = ts != null ? ts.toDate().getTime() : 0;
                            if (tsMs > maxTimestampMs) {
                                maxTimestampMs = tsMs;
                            }

                            String titulo = "Alerta en " + (buzonId != null ? buzonId : "buzón");
                            String textoHora = formatearTiempo(ts);

                            // Tipo "seguridad" + estado "urgente" → se pintará en rojo
                            notifList.add(new Notificacion(
                                    titulo,
                                    mensaje != null ? mensaje : "",
                                    textoHora,
                                    "seguridad",
                                    "urgente"
                            ));
                        }
                    }

                    notifAdapter.notifyDataSetChanged();

                    // Si hemos detectado un evento MÁS RECIENTE que el último visto,
                    // mostramos la alerta grande con sonido UNA VEZ.
                    if (maxTimestampMs > ultimoEventoMs && maxTimestampMs != 0) {
                        ultimoEventoMs = maxTimestampMs;

                        // Cogemos el mensaje del evento más reciente
                        String mensajeUltimo = "Alerta de seguridad en el buzón";
                        if (value != null && !value.isEmpty()) {

                            DocumentSnapshot docMasNuevo = value.getDocuments().get(0);
                            String m = docMasNuevo.getString("mensaje");
                            if (m != null && !m.isEmpty()) {
                                mensajeUltimo = m;
                            }
                        }

                        mostrarAlertaSeguridad(mensajeUltimo);
                    }
                });
    }

    // Lee un campo "timestamp" tolerando diferentes tipos (Timestamp, Long, Double...)
    private Timestamp leerTimestampSeguro(DocumentSnapshot doc, String campo) {
        Object tsObj = doc.get(campo);
        if (tsObj instanceof Timestamp) {
            return (Timestamp) tsObj;
        } else if (tsObj instanceof Long) {
            return new Timestamp(new Date((Long) tsObj));
        } else if (tsObj instanceof Double) {
            return new Timestamp(new Date(((Double) tsObj).longValue()));
        } else {
            // Si es String u otro tipo, podrías parsearlo aquí si quieres
            return null;
        }
    }

    // Mismo formato de tiempo que en NotificacionActivity
    private String formatearTiempo(Timestamp ts) {
        if (ts == null) return "";

        long ahora = System.currentTimeMillis();
        long evento = ts.toDate().getTime();
        long diff = ahora - evento;

        long minutos = diff / (60 * 1000);
        long horas = diff / (60 * 60 * 1000);
        long dias = diff / (24 * 60 * 60 * 1000);

        if (minutos < 1) return "Ahora";
        if (minutos < 60) return minutos + " min";
        if (horas < 24) return horas + " h";
        return dias + " d";
    }

    // -------------------------------------------------------
    // Muestra el diálogo grande de alerta y reproduce el sonido
    // -------------------------------------------------------
    private void mostrarAlertaSeguridad(String mensaje) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_alerta_seguridad);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        dialog.setCancelable(false); // el usuario debe pulsar Cerrar

        TextView tvMensaje = dialog.findViewById(R.id.tvMensajeAlerta);
        Button btnCerrar = dialog.findViewById(R.id.btnCerrarAlerta);

        tvMensaje.setText(mensaje);

        btnCerrar.setOnClickListener(v -> {
            detenerSonidoAlarma();
            dialog.dismiss();
        });

        dialog.show();
        reproducirSonidoAlarma();
    }

    // Reproduce el sonido de alarma en bucle
    private void reproducirSonidoAlarma() {
        detenerSonidoAlarma(); // por si hubiera uno sonando ya

        mediaPlayer = MediaPlayer.create(this, R.raw.alarma);
        if (mediaPlayer != null) {
            mediaPlayer.setLooping(true); // suena hasta que el usuario cierre
            mediaPlayer.start();
        }
    }

    // Detiene el sonido de alarma
    private void detenerSonidoAlarma() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
            } catch (IllegalStateException ignored) {}
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    // Por seguridad, paramos el sonido si la Activity se destruye
    @Override
    protected void onDestroy() {
        super.onDestroy();
        detenerSonidoAlarma();
    }
}
