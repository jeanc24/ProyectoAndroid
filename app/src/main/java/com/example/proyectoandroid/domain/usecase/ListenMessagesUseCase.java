package com.example.proyectoandroid.domain.usecase;

import com.example.proyectoandroid.data.model.Message;
import com.example.proyectoandroid.data.repository.MessageRepository;
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
        return messageRepository.sendTextMessage(chatId, content);
    }

    public CompletableFuture<Result<Message>> sendImageMessage(String chatId, String imageUrl) {
        return messageRepository.sendImageMessage(chatId, imageUrl);
    }

    public void cleanup() {
        for (ListenerRegistration registration : activeListeners.values()) {
            registration.remove();
        }
        activeListeners.clear();

        messageRepository.removeAllListeners();
    }
}
