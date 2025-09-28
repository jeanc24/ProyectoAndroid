package com.example.proyectoandroid.domain.usecase;

import com.example.proyectoandroid.data.model.User;
import com.example.proyectoandroid.data.repository.AuthRepository;

public class GetCurrentUserUseCase {

    private final AuthRepository authRepository;

    public GetCurrentUserUseCase(AuthRepository authRepository) {
        this.authRepository = authRepository;
    }

    public User execute() {
        return authRepository.getCurrentUser();
    }

    public boolean isUserLoggedIn() {
        return authRepository.isUserLoggedIn();
    }
}
