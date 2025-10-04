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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private List<Chat> chatList;
    private Map<String, User> userMap;
    private String currentUserId;
    private OnChatClickListener listener;

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

        if (!chat.isGroupChat() && chat.getParticipantIds().size() == 2 && currentUserId != null) {
            String otherUserId = chat.getParticipantIds().get(0).equals(currentUserId)
                    ? chat.getParticipantIds().get(1)
                    : chat.getParticipantIds().get(0);

            User otherUser = userMap.get(otherUserId);
            displayName = (otherUser != null && otherUser.getDisplayName() != null && !otherUser.getDisplayName().isEmpty())
                    ? otherUser.getDisplayName()
                    : otherUserId;
        } else {
            displayName = chat.getChatName();
        }

        holder.chatTitle.setText(displayName);

        String lastMsg = chat.getLastMessageContent();
        if (lastMsg == null) lastMsg = "";
        holder.chatLastMessage.setText(lastMsg);

        holder.chatTimestamp.setText(formatTimestamp(chat.getLastMessageTimestamp()));

        // Mostrar remitente ("Tú:" si es el actual)
        if (chat.getLastMessageSenderId() != null && chat.getLastMessageSenderId().equals(currentUserId)) {
            holder.chatLastSender.setText("Tú:");
        } else {
            String sender = chat.getLastMessageSenderName();
            if (sender == null || sender.isEmpty()) sender = chat.getLastMessageSenderEmail();
            holder.chatLastSender.setText(sender != null ? sender + ":" : "");
        }

        // Mostrar puntito de no leído si el último mensaje es de otro usuario y no está leído
        boolean showUnreadDot = !chat.isLastMessageRead() && chat.getLastMessageSenderId() != null && !chat.getLastMessageSenderId().equals(currentUserId);
        holder.unreadDot.setVisibility(showUnreadDot ? View.VISIBLE : View.GONE);

        holder.itemView.setOnClickListener(view -> {
            if (listener != null) listener.onChatClick(chat);
        });
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    private String formatTimestamp(Date date) {
        if (date == null) return "";
        Calendar today = Calendar.getInstance();
        Calendar msgDate = Calendar.getInstance();
        msgDate.setTime(date);

        boolean isToday = today.get(Calendar.YEAR) == msgDate.get(Calendar.YEAR)
                && today.get(Calendar.DAY_OF_YEAR) == msgDate.get(Calendar.DAY_OF_YEAR);

        if (isToday) {
            return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(date);
        } else {
            return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date);
        }
    }

    public static class ChatViewHolder extends RecyclerView.ViewHolder {
        ImageView chatAvatar;
        TextView chatTitle, chatLastMessage, chatTimestamp, chatLastSender;
        View unreadDot;
        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            chatAvatar = itemView.findViewById(R.id.chatAvatar);
            chatTitle = itemView.findViewById(R.id.chatTitle);
            chatLastMessage = itemView.findViewById(R.id.chatLastMessage);
            chatTimestamp = itemView.findViewById(R.id.chatTimestamp);
            chatLastSender = itemView.findViewById(R.id.chatLastSender);
            unreadDot = itemView.findViewById(R.id.unreadDot);
        }
    }

    public interface OnChatClickListener {
        void onChatClick(Chat chat);
    }
}