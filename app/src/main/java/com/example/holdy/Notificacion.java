package com.example.holdy;

// Clase modelo para representar una notificación individual
public class Notificacion {

    // Título principal (ej: "Nuevo paquete entregado")
    private String titulo;

    // Mensaje secundario (ej: "Amazon - entregado hace 5 min")
    private String mensaje;

    // Hora o tiempo relativo (ej: "5 min", "2h")
    private String hora;

    // Tipo de notificación: "paquete" o "seguridad"
    private String tipo;

    // Estado: "nuevo", "leido", "urgente"
    private String estado;

    // Constructor vacío requerido por Firebase
    public Notificacion() {
    }

    // Constructor con todos los campos
    public Notificacion(String titulo, String mensaje, String hora,
                        String tipo, String estado) {
        this.titulo = titulo;
        this.mensaje = mensaje;
        this.hora = hora;
        this.tipo = tipo;
        this.estado = estado;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getMensaje() {
        return mensaje;
    }

    public String getHora() {
        return hora;
    }

    public String getTipo() {
        return tipo;
    }

    public String getEstado() {
        return estado;
    }
}
