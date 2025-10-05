package com.example.proyectoandroid.domain.usecase;

import com.example.proyectoandroid.data.model.Chat;
import com.example.proyectoandroid.data.model.User;
import com.example.proyectoandroid.data.repository.ChatRepository;
import com.example.proyectoandroid.utils.Result;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class ListenUserChatsUseCase {

    private final ChatRepository chatRepository;
    private ListenerRegistration chatsListener;
    private final Map<String, ListenerRegistration> userStatusListeners = new HashMap<>();

    public ListenUserChatsUseCase(ChatRepository chatRepository) {
        this.chatRepository = chatRepository;
    }

    public ListenerRegistration listenForUserChats(Consumer<List<Chat>> onChatsChanged) {
        stopListeningForUserChats();

        chatsListener = chatRepository.addUserChatsListener(onChatsChanged);
        return chatsListener;
    }

    public void stopListeningForUserChats() {
        if (chatsListener != null) {
            chatRepository.removeUserChatsListener();
            chatsListener = null;
        }
    }

    public ListenerRegistration listenForUserStatus(String userId, Consumer<User> onUserStatusChanged) {
        stopListeningForUserStatus(userId);

        ListenerRegistration registration = chatRepository.addUserStatusListener(userId, onUserStatusChanged);
        userStatusListeners.put(userId, registration);
        return registration;
    }

    public void stopListeningForUserStatus(String userId) {
        ListenerRegistration registration = userStatusListeners.get(userId);
        if (registration != null) {
            chatRepository.removeUserStatusListener(userId);
            userStatusListeners.remove(userId);
        }
    }

    public CompletableFuture<Result<Void>> updateUserOnlineStatus(boolean isOnline) {
        return chatRepository.updateCurrentUserOnlineStatus(isOnline);
    }

    public void cleanup() {
        stopListeningForUserChats();

        for (String userId : new HashMap<>(userStatusListeners).keySet()) {
            stopListeningForUserStatus(userId);
        }
    }
}
