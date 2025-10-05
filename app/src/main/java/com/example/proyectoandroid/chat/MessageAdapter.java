package com.example.proyectoandroid.chat;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.proyectoandroid.R;
import com.example.proyectoandroid.data.model.Message;
import com.example.proyectoandroid.utils.CryptoUtils;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<Message> messages;
    private final String currentUserId;

    public MessageAdapter(List<Message> messages, String currentUserId) {
        this.messages = messages != null ? messages : new ArrayList<>();
        this.currentUserId = currentUserId;
    }

    // Tipos de mensaje
    private static final int TYPE_TEXT_OUTGOING = 0;
    private static final int TYPE_TEXT_INCOMING = 1;
    private static final int TYPE_IMAGE_OUTGOING = 2;
    private static final int TYPE_IMAGE_INCOMING = 3;

    @Override
    public int getItemViewType(int position) {
        Message msg = messages.get(position);
        boolean isOutgoing = msg.getSenderId() != null && msg.getSenderId().equals(currentUserId);

        if (msg.getMessageType() == 1) {
            return isOutgoing ? TYPE_IMAGE_OUTGOING : TYPE_IMAGE_INCOMING;
        } else {
            return isOutgoing ? TYPE_TEXT_OUTGOING : TYPE_TEXT_INCOMING;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_TEXT_OUTGOING) {
            View v = inflater.inflate(R.layout.item_message_outgoing, parent, false);
            return new TextMessageViewHolder(v);
        } else if (viewType == TYPE_TEXT_INCOMING) {
            View v = inflater.inflate(R.layout.item_message_incoming, parent, false);
            return new TextMessageViewHolder(v);
        } else if (viewType == TYPE_IMAGE_OUTGOING) {
            View v = inflater.inflate(R.layout.item_message_outgoing_image, parent, false);
            return new ImageMessageViewHolder(v);
        } else {
            View v = inflater.inflate(R.layout.item_message_incoming_image, parent, false);
            return new ImageMessageViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message msg = messages.get(position);

        // Manejo del encabezado de fecha por día
        TextView tvDateHeader = holder.itemView.findViewById(R.id.tvDateHeader);
        if (tvDateHeader != null) {
            Date currentDate = msg.getTimestamp();
            Date prevDate = position > 0 ? messages.get(position - 1).getTimestamp() : null;

            if (shouldShowDateHeader(currentDate, prevDate)) {
                tvDateHeader.setVisibility(View.VISIBLE);
                tvDateHeader.setText(formatDayHeader(currentDate));
            } else {
                tvDateHeader.setVisibility(View.GONE);
                tvDateHeader.setText("");
            }
        }

        if (holder instanceof TextMessageViewHolder) {
            TextMessageViewHolder h = (TextMessageViewHolder) holder;
            // DESCIFRA EL TEXTO ANTES DE MOSTRAR
            String decryptedText = msg.getContent();
            if (decryptedText != null && !decryptedText.isEmpty()) {
                try {
                    decryptedText = CryptoUtils.decrypt(decryptedText);
                } catch (Exception e) {
                    decryptedText = "[Error al descifrar]";
                }
            }
            h.tvText.setText(decryptedText);

            // Mostrar nombre o email del remitente
            String senderDisplay = "";
            if (msg.getSenderName() != null && !msg.getSenderName().isEmpty()) {
                senderDisplay = msg.getSenderName();
            } else if (msg.getSenderEmail() != null && !msg.getSenderEmail().isEmpty()) {
                senderDisplay = msg.getSenderEmail();
            }
            h.tvSender.setText(senderDisplay);
            h.tvTimestamp.setText(formatTimestamp(msg.getTimestamp()));
        }

        if (holder instanceof ImageMessageViewHolder) {
            ImageMessageViewHolder h = (ImageMessageViewHolder) holder;

            // Mostrar nombre o email del remitente
            String senderDisplay = "";
            if (msg.getSenderName() != null && !msg.getSenderName().isEmpty()) {
                senderDisplay = msg.getSenderName();
            } else if (msg.getSenderEmail() != null && !msg.getSenderEmail().isEmpty()) {
                senderDisplay = msg.getSenderEmail();
            }
            h.tvSender.setText(senderDisplay);
            h.tvTimestamp.setText(formatTimestamp(msg.getTimestamp()));

            if (!TextUtils.isEmpty(msg.getImageUrl())) {
                Picasso.get().load(msg.getImageUrl()).into(h.ivImageMessage);

                final String imageUrl = msg.getImageUrl();
                final String sender = senderDisplay;
                h.ivImageMessage.setOnClickListener(view -> {
                    ImageViewerActivity.launch(view.getContext(), imageUrl, sender);
                });
            } else {
                h.ivImageMessage.setImageDrawable(null);
                h.ivImageMessage.setOnClickListener(null); // Quitar listener si no hay imagen
            }
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    // Métodos para actualizar la lista con prevención de duplicados
    public void setMessages(List<Message> msgs) {
        messages.clear();
        // Ordenar mensajes por timestamp (más antiguos primero) si vienen al revés
        List<Message> sortedMessages = new ArrayList<>(msgs);
        Collections.reverse(sortedMessages);
        messages.addAll(sortedMessages);
        notifyDataSetChanged();
    }

    public void addMessage(Message msg) {
        // Verificar si el mensaje ya existe
        boolean exists = false;
        for (Message existingMsg : messages) {
            if (existingMsg.getMessageId() != null &&
                    existingMsg.getMessageId().equals(msg.getMessageId())) {
                exists = true;
                break;
            }
        }

        // Solo agregar si no existe
        if (!exists) {
            messages.add(msg);
            int position = messages.size() - 1;
            notifyItemInserted(position);
        }
    }

    // ViewHolders
    static class TextMessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvText, tvSender, tvTimestamp;
        public TextMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvText = itemView.findViewById(R.id.tvText);
            tvSender = itemView.findViewById(R.id.tvSender);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
        }
    }

    static class ImageMessageViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImageMessage;
        TextView tvSender, tvTimestamp;
        public ImageMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImageMessage = itemView.findViewById(R.id.ivImageMessage);
            tvSender = itemView.findViewById(R.id.tvSender);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
        }
    }

    private String formatTimestamp(java.util.Date date) {
        if (date == null) return "";
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return sdf.format(date);
    }

    private boolean shouldShowDateHeader(Date current, Date previous) {
        if (current == null) return false; // Si aún no hay timestamp del servidor, no mostrar
        if (previous == null) return true;  // Primer mensaje siempre muestra fecha
        return !isSameDay(current, previous);
    }

    private boolean isSameDay(Date d1, Date d2) {
        if (d1 == null || d2 == null) return false;
        Calendar c1 = Calendar.getInstance();
        c1.setTime(d1);
        Calendar c2 = Calendar.getInstance();
        c2.setTime(d2);
        return c1.get(Calendar.ERA) == c2.get(Calendar.ERA)
                && c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR)
                && c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR);
    }

    private String formatDayHeader(Date date) {
        if (date == null) return "";
        Calendar today = Calendar.getInstance();
        Calendar target = Calendar.getInstance();
        target.setTime(date);

        // Normalizar horas para comparación
        zeroTime(today);
        zeroTime(target);

        Calendar yesterday = (Calendar) today.clone();
        yesterday.add(Calendar.DAY_OF_YEAR, -1);

        if (isSameDay(target.getTime(), today.getTime())) {
            return "Hoy";
        } else if (isSameDay(target.getTime(), yesterday.getTime())) {
            return "Ayer";
        } else {
            // Ej: lun, 7 oct 2025
            SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault());
            return capitalizeFirst(sdf.format(date));
        }
    }

    private void zeroTime(Calendar cal) {
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
    }

    private String capitalizeFirst(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase(Locale.getDefault()) + s.substring(1);
    }
}