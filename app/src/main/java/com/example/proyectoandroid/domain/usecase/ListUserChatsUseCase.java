package com.example.proyectoandroid.domain.usecase;

import com.example.proyectoandroid.data.model.Chat;
import com.example.proyectoandroid.data.repository.ChatRepository;
import com.example.proyectoandroid.utils.Result;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ListUserChatsUseCase {

    private final ChatRepository chatRepository;

    public ListUserChatsUseCase(ChatRepository chatRepository) {
        this.chatRepository = chatRepository;
    }

    public CompletableFuture<Result<List<Chat>>> execute() {
        return chatRepository.getCurrentUserChats();
    }
}
