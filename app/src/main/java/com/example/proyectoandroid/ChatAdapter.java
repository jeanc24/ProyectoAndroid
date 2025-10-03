package com.example.proyectoandroid;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.proyectoandroid.data.model.Chat;
import com.example.proyectoandroid.data.model.User;

import java.util.List;
import java.util.Map;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private List<Chat> chatList;
    private Map<String, User> userMap;
    private String currentUserId;
    private OnChatClickListener listener;

    // Nuevo constructor que recibe el mapa de usuarios y el id del usuario actual
    public ChatAdapter(List<Chat> chatList, Map<String, User> userMap, String currentUserId, OnChatClickListener listener) {
        this.chatList = chatList;
        this.userMap = userMap;
        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat, parent, false);
        return new ChatViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        Chat chat = chatList.get(position);

        String displayName;

        // Si es chat individual, muestra el nombre del otro usuario
        if (!chat.isGroupChat() && chat.getParticipantIds().size() == 2 && currentUserId != null) {
            String otherUserId = chat.getParticipantIds().get(0).equals(currentUserId)
                    ? chat.getParticipantIds().get(1)
                    : chat.getParticipantIds().get(0);

            User otherUser = userMap.get(otherUserId);
            displayName = (otherUser != null && otherUser.getDisplayName() != null && !otherUser.getDisplayName().isEmpty())
                    ? otherUser.getDisplayName()
                    : otherUserId;
        } else {
            // Grupo o sin datos suficientes
            displayName = chat.getChatName();
        }

        holder.chatTitle.setText(displayName);
        holder.chatLastMessage.setText(chat.getLastMessageContent());

        holder.itemView.setOnClickListener(view -> {
            if (listener != null) listener.onChatClick(chat);
        });
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    public static class ChatViewHolder extends RecyclerView.ViewHolder {
        ImageView chatAvatar;
        TextView chatTitle, chatLastMessage;
        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            chatAvatar = itemView.findViewById(R.id.chatAvatar);
            chatTitle = itemView.findViewById(R.id.chatTitle);
            chatLastMessage = itemView.findViewById(R.id.chatLastMessage);
        }
    }

    public interface OnChatClickListener {
        void onChatClick(Chat chat);
    }
}