package com.example.holdy;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

// Adaptador para mostrar la lista de notificaciones en el RecyclerView
public class NotificacionAdapter extends RecyclerView.Adapter<NotificacionAdapter.NotifViewHolder> {

    // Lista de objetos Notificacion que se mostrarán
    private List<Notificacion> lista;

    // Recibe la lista de notificaciones a mostrar
    public NotificacionAdapter(List<Notificacion> lista) {
        this.lista = lista;
    }

    @NonNull
    @Override
    public NotifViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflar el layout de cada ítem de la lista
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notificacion, parent, false);
        return new NotifViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull NotifViewHolder holder, int position) {
        // Obtener la notificación actual
        Notificacion n = lista.get(position);

        // Rellenar textos básicos
        holder.txtTitulo.setText(n.getTitulo());
        holder.txtMensaje.setText(n.getMensaje());
        holder.txtHora.setText(n.getHora());

        // -------------------------
        // Icono según el tipo
        // -------------------------
        if ("paquete".equalsIgnoreCase(n.getTipo())) {
            // paquete.png
            holder.imgIcono.setImageResource(R.drawable.paquete);
        } else if ("seguridad".equalsIgnoreCase(n.getTipo())) {
            // urgente.png (alerta de seguridad)
            holder.imgIcono.setImageResource(R.drawable.urgente);
        } else {
            // icono por defecto si hiciera falta
            holder.imgIcono.setImageResource(R.drawable.paquete);
        }

        // -------------------------
        // Estado: nuevo / leído / urgente
        // -------------------------
        String estado = n.getEstado();
        if ("nuevo".equalsIgnoreCase(estado)) {
            holder.txtEstado.setText("Nuevo");
            holder.txtEstado.setTextColor(Color.WHITE);
            holder.txtEstado.setBackgroundResource(R.drawable.badge_nuevo);
        } else if ("leido".equalsIgnoreCase(estado)) {
            holder.txtEstado.setText("Leído");
            holder.txtEstado.setTextColor(Color.parseColor("#23295C"));
            holder.txtEstado.setBackgroundResource(R.drawable.badge_leido);
        } else if ("urgente".equalsIgnoreCase(estado)) {
            holder.txtEstado.setText("Urgente");
            holder.txtEstado.setTextColor(Color.WHITE);
            holder.txtEstado.setBackgroundResource(R.drawable.badge_urgente);
        } else {
            // Por si llega un estado desconocido
            holder.txtEstado.setText("");
            holder.txtEstado.setBackground(null);
        }
    }

    @Override
    public int getItemCount() {
        return lista.size();
    }

    // ViewHolder interno que guarda las referencias a las vistas de cada ítem
    public static class NotifViewHolder extends RecyclerView.ViewHolder {

        ImageView imgIcono;
        TextView txtTitulo, txtMensaje, txtHora, txtEstado;

        public NotifViewHolder(@NonNull View itemView) {
            super(itemView);
            imgIcono = itemView.findViewById(R.id.imgIconoNotif);
            txtTitulo = itemView.findViewById(R.id.txtTituloNotif);
            txtMensaje = itemView.findViewById(R.id.txtMensajeNotif);
            txtHora = itemView.findViewById(R.id.txtHoraNotif);
            txtEstado = itemView.findViewById(R.id.txtEstadoNotif);
        }
    }
}