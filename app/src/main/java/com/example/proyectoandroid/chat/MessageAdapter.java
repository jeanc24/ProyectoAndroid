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
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
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

        if (holder instanceof TextMessageViewHolder) {
            TextMessageViewHolder h = (TextMessageViewHolder) holder;
            h.tvText.setText(msg.getContent());
            h.tvSender.setText(msg.getSenderName());
            h.tvTimestamp.setText(formatTimestamp(msg.getTimestamp()));
        }

        if (holder instanceof ImageMessageViewHolder) {
            ImageMessageViewHolder h = (ImageMessageViewHolder) holder;
            h.tvSender.setText(msg.getSenderName());
            h.tvTimestamp.setText(formatTimestamp(msg.getTimestamp()));
            if (!TextUtils.isEmpty(msg.getImageUrl())) {
                Picasso.get().load(msg.getImageUrl()).into(h.ivImageMessage);
            }
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    // Métodos para actualizar la lista
    public void setMessages(List<Message> msgs) {
        messages.clear();
        // Ordenar mensajes por timestamp (más antiguos primero)
        List<Message> sortedMessages = new ArrayList<>(msgs);
        Collections.reverse(sortedMessages);
        messages.addAll(sortedMessages);
        notifyDataSetChanged();
    }

    public void addMessage(Message msg) {
        messages.add(msg);
        int position = messages.size() - 1;
        notifyItemInserted(position);

        // Animación para el nuevo mensaje
        animateNewMessage(position);
    }

    private void animateNewMessage(int position) {
        // Esta animación se aplicará automáticamente cuando se haga scroll
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
}