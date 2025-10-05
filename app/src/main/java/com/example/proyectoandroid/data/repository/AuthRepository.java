package com.example.proyectoandroid.data.repository;

import com.example.proyectoandroid.data.model.User;
import com.example.proyectoandroid.utils.Result;

import java.util.concurrent.CompletableFuture;

public interface AuthRepository {

    CompletableFuture<Result<User>> registerUser(String email, String password, String displayName);

    CompletableFuture<Result<User>> loginUser(String email, String password);

    User getCurrentUser();

    boolean isUserLoggedIn();

    void signOut();

    CompletableFuture<Result<Void>> updateUserOnlineStatus(boolean isOnline);
}
