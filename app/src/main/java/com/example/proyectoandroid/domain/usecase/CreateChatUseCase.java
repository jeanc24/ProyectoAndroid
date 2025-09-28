package com.example.proyectoandroid.domain.usecase;

import com.example.proyectoandroid.data.model.Chat;
import com.example.proyectoandroid.data.repository.ChatRepository;
import com.example.proyectoandroid.utils.Result;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CreateChatUseCase {

    private final ChatRepository chatRepository;

    public CreateChatUseCase(ChatRepository chatRepository) {
        this.chatRepository = chatRepository;
    }

    public CompletableFuture<Result<Chat>> execute(String otherUserId) {
        return chatRepository.createChat(otherUserId);
    }

    public CompletableFuture<Result<Chat>> createGroupChat(List<String> participantIds, String chatName) {
        return chatRepository.createGroupChat(participantIds, chatName);
    }
}
