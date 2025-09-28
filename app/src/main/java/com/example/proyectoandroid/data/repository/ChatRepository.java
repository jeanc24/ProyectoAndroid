package com.example.proyectoandroid.data.repository;

import com.example.proyectoandroid.data.model.Chat;
import com.example.proyectoandroid.data.model.User;
import com.example.proyectoandroid.utils.Result;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public interface ChatRepository {

    CompletableFuture<Result<Chat>> createChat(String otherUserId);

    CompletableFuture<Result<Chat>> createGroupChat(List<String> participantIds, String chatName);

    CompletableFuture<Result<Chat>> getChatById(String chatId);

    CompletableFuture<Result<List<Chat>>> getCurrentUserChats();

    CompletableFuture<Result<Chat>> findDirectChat(String otherUserId);

    ListenerRegistration addUserChatsListener(Consumer<List<Chat>> onChatsChanged);

    void removeUserChatsListener();

    ListenerRegistration addUserStatusListener(String userId, Consumer<User> onUserStatusChanged);

    void removeUserStatusListener(String userId);

    CompletableFuture<Result<Void>> updateCurrentUserOnlineStatus(boolean isOnline);

    void removeAllListeners();
}
