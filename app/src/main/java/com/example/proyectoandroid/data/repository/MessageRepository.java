package com.example.proyectoandroid.data.repository;

import com.example.proyectoandroid.data.model.Message;
import com.example.proyectoandroid.utils.Result;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public interface MessageRepository {
    
    CompletableFuture<Result<Message>> sendTextMessage(String chatId, String content);
    
    CompletableFuture<Result<Message>> sendImageMessage(String chatId, String imageUrl);

    CompletableFuture<Result<List<Message>>> getChatMessages(String chatId, int limit);

    CompletableFuture<Result<List<Message>>> getChatMessagesPaginated(String chatId, Date lastMessageTimestamp, int pageSize);

    CompletableFuture<Result<Void>> markMessagesAsRead(String chatId);

    ListenerRegistration addChatMessagesListener(
            String chatId,
            int limit,
            Consumer<List<Message>> onInitialMessages,
            Consumer<Message> onNewMessage,
            Consumer<Message> onModifiedMessage);

    void removeChatMessagesListener(String chatId);

    void removeAllListeners();
}
