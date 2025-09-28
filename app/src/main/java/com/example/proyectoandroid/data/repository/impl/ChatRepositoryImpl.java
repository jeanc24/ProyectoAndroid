package com.example.proyectoandroid.data.repository.impl;

import com.example.proyectoandroid.data.model.Chat;
import com.example.proyectoandroid.data.model.User;
import com.example.proyectoandroid.data.remote.FirestoreDataSource;
import com.example.proyectoandroid.data.repository.AuthRepository;
import com.example.proyectoandroid.data.repository.ChatRepository;
import com.example.proyectoandroid.utils.Result;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class ChatRepositoryImpl implements ChatRepository {

    private final FirestoreDataSource firestoreDataSource;
    private final AuthRepository authRepository;
    private ListenerRegistration userChatsListener;

    public ChatRepositoryImpl(FirestoreDataSource firestoreDataSource, AuthRepository authRepository) {
        this.firestoreDataSource = firestoreDataSource;
        this.authRepository = authRepository;
    }

    @Override
    public CompletableFuture<Result<Chat>> createChat(String otherUserId) {
        CompletableFuture<Result<Chat>> resultFuture = new CompletableFuture<>();

        findDirectChat(otherUserId).thenAccept(result -> {
            if (result instanceof Result.Success) {
                resultFuture.complete(result);
            } else {
                User currentUser = authRepository.getCurrentUser();
                if (currentUser == null) {
                    resultFuture.complete(new Result.Error<>("User not logged in"));
                    return;
                }

                Chat newChat = new Chat(currentUser.getUid(), otherUserId);
                firestoreDataSource.createChat(newChat).thenAccept(resultFuture::complete);
            }
        });

        return resultFuture;
    }

    @Override
    public CompletableFuture<Result<Chat>> createGroupChat(List<String> participantIds, String chatName) {
        User currentUser = authRepository.getCurrentUser();
        if (currentUser == null) {
            return CompletableFuture.completedFuture(new Result.Error<>("User not logged in"));
        }

        if (!participantIds.contains(currentUser.getUid())) {
            participantIds.add(currentUser.getUid());
        }

        Chat groupChat = new Chat(participantIds, chatName);
        return firestoreDataSource.createChat(groupChat);
    }

    @Override
    public CompletableFuture<Result<Chat>> getChatById(String chatId) {
        return firestoreDataSource.getChatById(chatId);
    }

    @Override
    public CompletableFuture<Result<List<Chat>>> getCurrentUserChats() {
        User currentUser = authRepository.getCurrentUser();
        if (currentUser == null) {
            return CompletableFuture.completedFuture(new Result.Error<>("User not logged in"));
        }

        return firestoreDataSource.getUserChats(currentUser.getUid());
    }

    @Override
    public CompletableFuture<Result<Chat>> findDirectChat(String otherUserId) {
        CompletableFuture<Result<Chat>> resultFuture = new CompletableFuture<>();

        User currentUser = authRepository.getCurrentUser();
        if (currentUser == null) {
            resultFuture.complete(new Result.Error<>("User not logged in"));
            return resultFuture;
        }

        firestoreDataSource.getUserChats(currentUser.getUid())
            .thenAccept(result -> {
                if (result instanceof Result.Success) {
                    List<Chat> chats = ((Result.Success<List<Chat>>) result).getData();

                    for (Chat chat : chats) {
                        if (!chat.isGroupChat() && chat.getParticipantIds().size() == 2
                                && chat.getParticipantIds().contains(otherUserId)
                                && chat.getParticipantIds().contains(currentUser.getUid())) {
                            resultFuture.complete(new Result.Success<>(chat));
                            return;
                        }
                    }

                    resultFuture.complete(new Result.Error<>("Chat not found"));
                } else {
                    String errorMsg = ((Result.Error<List<Chat>>) result).getErrorMessage();
                    resultFuture.complete(new Result.Error<>(errorMsg));
                }
            });

        return resultFuture;
    }

    @Override
    public ListenerRegistration addUserChatsListener(Consumer<List<Chat>> onChatsChanged) {
        User currentUser = authRepository.getCurrentUser();
        if (currentUser == null) {
            return null;
        }

        removeUserChatsListener();

        userChatsListener = firestoreDataSource.addUserChatsListener(currentUser.getUid(), onChatsChanged);
        return userChatsListener;
    }

    @Override
    public void removeUserChatsListener() {
        if (userChatsListener != null) {
            userChatsListener.remove();
            userChatsListener = null;
        }
    }

    @Override
    public ListenerRegistration addUserStatusListener(String userId, Consumer<User> onUserStatusChanged) {
        return firestoreDataSource.addUserStatusListener(userId, onUserStatusChanged);
    }

    @Override
    public void removeUserStatusListener(String userId) {
        firestoreDataSource.removeUserStatusListener(userId);
    }

    @Override
    public CompletableFuture<Result<Void>> updateCurrentUserOnlineStatus(boolean isOnline) {
        User currentUser = authRepository.getCurrentUser();
        if (currentUser == null) {
            return CompletableFuture.completedFuture(new Result.Error<>("User not logged in"));
        }

        return firestoreDataSource.updateUserOnlineStatus(currentUser.getUid(), isOnline);
    }

    @Override
    public void removeAllListeners() {
        removeUserChatsListener();
        firestoreDataSource.removeAllListeners();
    }
}
