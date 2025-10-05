package com.example.proyectoandroid.domain.usecase;

import com.example.proyectoandroid.data.model.User;
import com.example.proyectoandroid.data.repository.AuthRepository;
import com.example.proyectoandroid.utils.Result;

import java.util.concurrent.CompletableFuture;

public class LoginUserUseCase {

    private final AuthRepository authRepository;

    public LoginUserUseCase(AuthRepository authRepository) {
        this.authRepository = authRepository;
    }

    public CompletableFuture<Result<User>> execute(String email, String password) {
        return authRepository.loginUser(email, password);
    }

    public CompletableFuture<Result<User>> registerUser(String email, String password, String displayName) {
        return authRepository.registerUser(email, password, displayName);
    }

    public void signOut() {
        authRepository.signOut();
    }
}
