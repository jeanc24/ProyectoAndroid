package com.example.proyectoandroid.data.repository.impl;

import com.example.proyectoandroid.data.model.Message;
import com.example.proyectoandroid.data.model.User;
import com.example.proyectoandroid.data.remote.FirestoreDataSource;
import com.example.proyectoandroid.data.repository.AuthRepository;
import com.example.proyectoandroid.data.repository.MessageRepository;
import com.example.proyectoandroid.utils.Result;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class MessageRepositoryImpl implements MessageRepository {

    private final FirestoreDataSource firestoreDataSource;
    private final AuthRepository authRepository;

    public MessageRepositoryImpl(FirestoreDataSource firestoreDataSource, AuthRepository authRepository) {
        this.firestoreDataSource = firestoreDataSource;
        this.authRepository = authRepository;
    }

    @Override
    public CompletableFuture<Result<Message>> sendTextMessage(String chatId, String content) {
        User currentUser = authRepository.getCurrentUser();
        if (currentUser == null) {
            return CompletableFuture.completedFuture(new Result.Error<>("User not logged in"));
        }

        Message message = new Message(
                chatId,
                currentUser.getUid(),
                currentUser.getDisplayName(),
                content
        );

        return firestoreDataSource.sendMessage(message);
    }

    @Override
    public CompletableFuture<Result<Message>> sendImageMessage(String chatId, String imageUrl) {
        User currentUser = authRepository.getCurrentUser();
        if (currentUser == null) {
            return CompletableFuture.completedFuture(new Result.Error<>("User not logged in"));
        }

        Message message = new Message(
                chatId,
                currentUser.getUid(),
                currentUser.getDisplayName(),
                imageUrl,
                true
        );

        return firestoreDataSource.sendMessage(message);
    }

    @Override
    public CompletableFuture<Result<List<Message>>> getChatMessages(String chatId, int limit) {
        return firestoreDataSource.getChatMessages(chatId, limit);
    }

    @Override
    public CompletableFuture<Result<List<Message>>> getChatMessagesPaginated(String chatId, Date lastMessageTimestamp, int pageSize) {
        return firestoreDataSource.getChatMessagesPaginated(chatId, lastMessageTimestamp, pageSize);
    }

    @Override
    public CompletableFuture<Result<Void>> markMessagesAsRead(String chatId) {
        User currentUser = authRepository.getCurrentUser();
        if (currentUser == null) {
            return CompletableFuture.completedFuture(new Result.Error<>("User not logged in"));
        }

        return firestoreDataSource.markMessagesAsRead(chatId, currentUser.getUid());
    }

    @Override
    public ListenerRegistration addChatMessagesListener(
            String chatId,
            int limit,
            Consumer<List<Message>> onInitialMessages,
            Consumer<Message> onNewMessage,
            Consumer<Message> onModifiedMessage) {

        return firestoreDataSource.addChatMessagesListener(
                chatId,
                limit,
                onInitialMessages,
                onNewMessage,
                onModifiedMessage
        );
    }

    @Override
    public void removeChatMessagesListener(String chatId) {
        firestoreDataSource.removeChatMessagesListener(chatId);
    }

    @Override
    public void removeAllListeners() {
        firestoreDataSource.removeAllListeners();
    }
}
