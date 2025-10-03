package com.example.proyectoandroid.data.repository.impl;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.example.proyectoandroid.data.model.Message;
import com.example.proyectoandroid.data.model.User;
import com.example.proyectoandroid.data.remote.FirebaseStorageDataSource;
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

    private static final String TAG = "MessageRepositoryImpl";
    private final FirestoreDataSource firestoreDataSource;
    private final FirebaseStorageDataSource storageDataSource;
    private final AuthRepository authRepository;

    public MessageRepositoryImpl(
            FirestoreDataSource firestoreDataSource,
            FirebaseStorageDataSource storageDataSource,
            AuthRepository authRepository) {
        this.firestoreDataSource = firestoreDataSource;
        this.storageDataSource = storageDataSource;
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
                currentUser.getEmail(),
                content,
                0,         // messageType: 0 para texto
                null       // imageUrl: null para texto
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
                currentUser.getEmail(),
                "",        // content vacío para imágenes
                1,         // messageType: 1 para imagen
                imageUrl   // imageUrl para imagen
        );

        return firestoreDataSource.sendMessage(message);
    }

    @Override
    public CompletableFuture<Result<Message>> uploadAndSendImageMessage(Context context, String chatId, Uri imageUri) {
        CompletableFuture<Result<Message>> resultFuture = new CompletableFuture<>();

        User currentUser = authRepository.getCurrentUser();
        if (currentUser == null) {
            resultFuture.complete(new Result.Error<>("User not logged in"));
            return resultFuture;
        }

        if (imageUri == null) {
            resultFuture.complete(new Result.Error<>("Invalid image URI"));
            return resultFuture;
        }

        storageDataSource.uploadImage(context, imageUri, chatId)
                .thenApply(result -> {
                    if (result instanceof Result.Success) {
                        String imageUrl = ((Result.Success<String>) result).getData();
                        Log.d(TAG, "Image uploaded successfully: " + imageUrl);

                        return sendImageMessage(chatId, imageUrl);
                    } else {
                        String errorMessage = ((Result.Error<String>) result).getErrorMessage();
                        Log.e(TAG, "Failed to upload image: " + errorMessage);
                        resultFuture.complete(new Result.Error<>(errorMessage));
                        return null;
                    }
                })
                .thenAccept(messageFuture -> {
                    if (messageFuture != null) {
                        messageFuture.thenAccept(resultFuture::complete);
                    }
                })
                .exceptionally(e -> {
                    Log.e(TAG, "Error uploading and sending image", e);
                    resultFuture.complete(new Result.Error<>(e.getMessage()));
                    return null;
                });

        return resultFuture;
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