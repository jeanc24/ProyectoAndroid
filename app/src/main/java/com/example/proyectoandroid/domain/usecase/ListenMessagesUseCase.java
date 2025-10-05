package com.example.proyectoandroid.domain.usecase;

import android.content.Context;
import android.net.Uri;

import com.example.proyectoandroid.data.model.Message;
import com.example.proyectoandroid.data.repository.MessageRepository;
import com.example.proyectoandroid.utils.CryptoUtils;
import com.example.proyectoandroid.utils.Result;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class ListenMessagesUseCase {

    private final MessageRepository messageRepository;
    private static final int DEFAULT_MESSAGE_LIMIT = 50;
    private final Map<String, ListenerRegistration> activeListeners = new HashMap<>();

    public ListenMessagesUseCase(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public CompletableFuture<Result<List<Message>>> execute(String chatId) {
        return execute(chatId, DEFAULT_MESSAGE_LIMIT);
    }

    public CompletableFuture<Result<List<Message>>> execute(String chatId, int limit) {
        CompletableFuture<Result<List<Message>>> resultFuture = messageRepository.getChatMessages(chatId, limit);

        messageRepository.markMessagesAsRead(chatId);

        return resultFuture;
    }

    public ListenerRegistration listenForMessages(
            String chatId,
            int limit,
            Consumer<List<Message>> onInitialMessages,
            Consumer<Message> onNewMessage,
            Consumer<Message> onMessageUpdated) {

        stopListeningForMessages(chatId);

        ListenerRegistration registration = messageRepository.addChatMessagesListener(
                chatId,
                limit,
                messages -> {
                    messageRepository.markMessagesAsRead(chatId);

                    if (onInitialMessages != null) {
                        onInitialMessages.accept(messages);
                    }
                },
                message -> {
                    messageRepository.markMessagesAsRead(chatId);

                    if (onNewMessage != null) {
                        onNewMessage.accept(message);
                    }
                },
                message -> {
                    if (onMessageUpdated != null) {
                        onMessageUpdated.accept(message);
                    }
                }
        );

        activeListeners.put(chatId, registration);

        return registration;
    }

    public com.google.firebase.firestore.ListenerRegistration listenForMessagesWithoutMarkingAsRead(
            String chatId,
            int limit,
            java.util.function.Consumer<java.util.List<com.example.proyectoandroid.data.model.Message>> onInitialMessages,
            java.util.function.Consumer<com.example.proyectoandroid.data.model.Message> onNewMessage,
            java.util.function.Consumer<com.example.proyectoandroid.data.model.Message> onMessageUpdated) {

        stopListeningForMessages(chatId);

        com.google.firebase.firestore.ListenerRegistration registration = messageRepository.addChatMessagesListener(
                chatId,
                limit,
                messages -> {
                    if (onInitialMessages != null) onInitialMessages.accept(messages);
                },
                message -> {
                    if (onNewMessage != null) onNewMessage.accept(message);
                },
                message -> {
                    if (onMessageUpdated != null) onMessageUpdated.accept(message);
                }
        );

        activeListeners.put(chatId, registration);
        return registration;
    }

    public void stopListeningForMessages(String chatId) {
        ListenerRegistration registration = activeListeners.get(chatId);
        if (registration != null) {
            registration.remove();
            activeListeners.remove(chatId);
        }
    }

    public CompletableFuture<Result<List<Message>>> loadMoreMessages(
            String chatId,
            Date lastMessageTimestamp,
            int pageSize) {

        return messageRepository.getChatMessagesPaginated(chatId, lastMessageTimestamp, pageSize);
    }


    public CompletableFuture<Result<Message>> sendTextMessage(String chatId, String content) {
        try {
            String encryptedContent = CryptoUtils.encrypt(content);
            return messageRepository.sendTextMessage(chatId, encryptedContent);
        } catch (Exception e) {
            CompletableFuture<Result<Message>> future = new CompletableFuture<>();
            future.complete(new Result.Error<>(e.getMessage()));
            return future;
        }
    }

    public CompletableFuture<Result<Message>> sendImageMessage(String chatId, String imageUrl) {
        return messageRepository.sendImageMessage(chatId, imageUrl);
    }

    public CompletableFuture<Result<Message>> uploadAndSendImageMessage(Context context, String chatId, Uri imageUri) {
        return messageRepository.uploadAndSendImageMessage(context, chatId, imageUri);
    }

    public CompletableFuture<Result<Void>> markChatAsRead(String chatId) {
        return messageRepository.markMessagesAsRead(chatId);
    }

    public void cleanup() {
        for (ListenerRegistration registration : activeListeners.values()) {
            registration.remove();
        }
        activeListeners.clear();

        messageRepository.removeAllListeners();
    }
}
