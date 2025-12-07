package com.example.holdy;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

// Actividad que muestra todas las notificaciones en una pantalla completa
public class NotificacionActivity extends AppCompatActivity {

    // RecyclerView y adaptador
    private RecyclerView rvNotificacionesFull;
    private NotificacionAdapter notifAdapter;

    // Lista original con TODAS las notificaciones
    private List<Notificacion> listaOriginal = new ArrayList<>();
    // Lista filtrada que realmente se muestra en pantalla
    private List<Notificacion> listaFiltrada = new ArrayList<>();

    // Botones de filtro
    private Button btnFiltroTodas, btnFiltroPaquetes, btnFiltroSeguridad;

    // Caja de búsqueda
    private EditText edtBuscarNotif;

    // Filtro actual (todas / paquetes / seguridad)
    private String filtroActual = "todas";

    // Texto de búsqueda actual
    private String textoBusqueda = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notificacion);

        // -------------------------------
        // Referencias a las vistas
        // -------------------------------
        rvNotificacionesFull = findViewById(R.id.rvNotificacionesFull);
        rvNotificacionesFull.setLayoutManager(new LinearLayoutManager(this));

        notifAdapter = new NotificacionAdapter(listaFiltrada);
        rvNotificacionesFull.setAdapter(notifAdapter);

        ImageView imgBack = findViewById(R.id.imgBackNotif);
        btnFiltroTodas = findViewById(R.id.btnFiltroTodas);
        btnFiltroPaquetes = findViewById(R.id.btnFiltroPaquetes);
        btnFiltroSeguridad = findViewById(R.id.btnFiltroSeguridad);
        edtBuscarNotif = findViewById(R.id.edtBuscarNotif);

        // Flecha atrás → volver a la pantalla anterior
        imgBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // -------------------------------
        // Listeners de los botones filtro
        // -------------------------------
        btnFiltroTodas.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filtroActual = "todas";
                aplicarFiltro();
                actualizarEstilosBotones();
            }
        });

        btnFiltroPaquetes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filtroActual = "paquetes";
                aplicarFiltro();
                actualizarEstilosBotones();
            }
        });

        btnFiltroSeguridad.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filtroActual = "seguridad";
                aplicarFiltro();
                actualizarEstilosBotones();
            }
        });

        // -------------------------------
        // Listener del buscador (texto)
        // -------------------------------
        edtBuscarNotif.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No necesitamos hacer nada aquí
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Guardamos el texto actual (en minúsculas para comparar)
                textoBusqueda = s.toString().toLowerCase().trim();
                aplicarFiltro();
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Tampoco necesitamos nada aquí
            }
        });

        // Cargamos datos de prueba (más adelante vendrán desde Firebase)
        cargarNotifsDummy();

        // Estilo inicial: "Todas" activa
        actualizarEstilosBotones();
    }

    // ---------------------------------------------------
    // Datos de prueba (se quitarán cuando usemos Firebase)
    // ---------------------------------------------------
    private void cargarNotifsDummy() {
        listaOriginal.clear();

        listaOriginal.add(new Notificacion(
                "Nuevo paquete entregado",
                "Amazon - entregado hace 5 min",
                "5 min",
                "paquete",
                "nuevo"
        ));

        listaOriginal.add(new Notificacion(
                "Paquete en camino",
                "Correos - llega hoy",
                "2 h",
                "paquete",
                "leido"
        ));

        listaOriginal.add(new Notificacion(
                "Alerta sin autorización",
                "Caja forzada a las 23:39",
                "2 h",
                "seguridad",
                "urgente"
        ));

        aplicarFiltro();
    }

    // ---------------------------------------------------
    // Aplica filtro (todas/paquetes/seguridad) + texto búsqueda
    // ---------------------------------------------------
    private void aplicarFiltro() {
        listaFiltrada.clear();

        for (Notificacion n : listaOriginal) {
            String tipo = n.getTipo() != null ? n.getTipo().toLowerCase() : "";
            String estado = n.getEstado() != null ? n.getEstado().toLowerCase() : "";
            String titulo = n.getTitulo() != null ? n.getTitulo().toLowerCase() : "";
            String mensaje = n.getMensaje() != null ? n.getMensaje().toLowerCase() : "";

            // 1️⃣ Primero aplicamos el filtro de tipo
            boolean pasaFiltroTipo = false;

            switch (filtroActual) {
                case "todas":
                    pasaFiltroTipo = true;
                    break;

                case "paquetes":
                    pasaFiltroTipo = "paquete".equals(tipo);
                    break;

                case "seguridad":
                    pasaFiltroTipo = "seguridad".equals(tipo) || "urgente".equals(estado);
                    break;
            }

            if (!pasaFiltroTipo) continue;

            // 2️⃣ Luego aplicamos el filtro de búsqueda
            if (!textoBusqueda.isEmpty()) {
                boolean coincide =
                        titulo.contains(textoBusqueda) ||
                                mensaje.contains(textoBusqueda);

                if (!coincide) continue;
            }

            // Si pasa ambos filtros, lo añadimos
            listaFiltrada.add(n);
        }

        notifAdapter.notifyDataSetChanged();
    }

    // ---------------------------------------------------
    // Cambia colores de los botones según el filtro activo
    // ---------------------------------------------------
    private void actualizarEstilosBotones() {
        int colorActivoBg = Color.parseColor("#4739C6");   // morado fuerte
        int colorInactivoBg = Color.parseColor("#D6D6F8"); // lila claro
        int colorTextoActivo = Color.WHITE;
        int colorTextoInactivo = Color.parseColor("#23295C");

        if ("todas".equals(filtroActual)) {
            activarBoton(btnFiltroTodas, colorActivoBg, colorTextoActivo);
        } else {
            desactivarBoton(btnFiltroTodas, colorInactivoBg, colorTextoInactivo);
        }

        if ("paquetes".equals(filtroActual)) {
            activarBoton(btnFiltroPaquetes, colorActivoBg, colorTextoActivo);
        } else {
            desactivarBoton(btnFiltroPaquetes, colorInactivoBg, colorTextoInactivo);
        }

        if ("seguridad".equals(filtroActual)) {
            activarBoton(btnFiltroSeguridad, colorActivoBg, colorTextoActivo);
        } else {
            desactivarBoton(btnFiltroSeguridad, colorInactivoBg, colorTextoInactivo);
        }
    }

    // Botón en estado ACTIVO
    private void activarBoton(Button btn, int colorBg, int colorTexto) {
        btn.setBackgroundTintList(ColorStateList.valueOf(colorBg));
        btn.setTextColor(colorTexto);
    }

    // Botón en estado INACTIVO
    private void desactivarBoton(Button btn, int colorBg, int colorTexto) {
        btn.setBackgroundTintList(ColorStateList.valueOf(colorBg));
        btn.setTextColor(colorTexto);
    }
}
