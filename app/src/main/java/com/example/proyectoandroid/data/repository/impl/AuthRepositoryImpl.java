package com.example.proyectoandroid.data.repository.impl;

import android.content.Context;

import com.example.proyectoandroid.data.local.SessionManager;
import com.example.proyectoandroid.data.model.User;
import com.example.proyectoandroid.data.remote.FirebaseAuthDataSource;
import com.example.proyectoandroid.data.remote.FirestoreDataSource;
import com.example.proyectoandroid.data.repository.AuthRepository;
import com.example.proyectoandroid.utils.Result;

import java.util.concurrent.CompletableFuture;

public class AuthRepositoryImpl implements AuthRepository {

    private final FirebaseAuthDataSource authDataSource;
    private final FirestoreDataSource firestoreDataSource;
    private final SessionManager sessionManager;

    public AuthRepositoryImpl(FirebaseAuthDataSource authDataSource,
                             FirestoreDataSource firestoreDataSource,
                             SessionManager sessionManager) {
        this.authDataSource = authDataSource;
        this.firestoreDataSource = firestoreDataSource;
        this.sessionManager = sessionManager;
    }

    @Override
    public CompletableFuture<Result<User>> registerUser(String email, String password, String displayName) {
        CompletableFuture<Result<User>> resultFuture = new CompletableFuture<>();

        authDataSource.registerUser(email, password, displayName)
            .thenAccept(result -> {
                if (result instanceof Result.Success) {
                    User user = ((Result.Success<User>) result).getData();

                    firestoreDataSource.createOrUpdateUser(user)
                        .thenAccept(firestoreResult -> {
                            if (firestoreResult instanceof Result.Success) {
                                sessionManager.saveUserSession(user);
                                resultFuture.complete(result);
                            } else {
                                String errorMessage = ((Result.Error<User>) firestoreResult).getErrorMessage();
                                resultFuture.complete(new Result.Error<>(errorMessage));
                            }
                        });
                } else {
                    resultFuture.complete(result);
                }
            });

        return resultFuture;
    }

    @Override
    public CompletableFuture<Result<User>> loginUser(String email, String password) {
        CompletableFuture<Result<User>> resultFuture = new CompletableFuture<>();

        authDataSource.loginUser(email, password)
            .thenAccept(result -> {
                if (result instanceof Result.Success) {
                    User user = ((Result.Success<User>) result).getData();

                    firestoreDataSource.updateUserOnlineStatus(user.getUid(), true)
                        .thenRun(() -> {
                            user.setOnline(true);
                            sessionManager.saveUserSession(user);
                            resultFuture.complete(result);
                        });
                } else {
                    resultFuture.complete(result);
                }
            });

        return resultFuture;
    }

    @Override
    public User getCurrentUser() {
        User sessionUser = sessionManager.getUserData();
        if (sessionUser != null) {
            return sessionUser;
        }

        if (authDataSource.getCurrentUser() != null) {
            User user = new User(
                authDataSource.getCurrentUser().getUid(),
                authDataSource.getCurrentUser().getEmail(),
                authDataSource.getCurrentUser().getDisplayName()
            );
            sessionManager.saveUserSession(user);
            return user;
        }

        return null;
    }

    @Override
    public boolean isUserLoggedIn() {
        return sessionManager.isLoggedIn() || authDataSource.isUserLoggedIn();
    }

    @Override
    public void signOut() {
        User currentUser = getCurrentUser();
        if (currentUser != null) {
            firestoreDataSource.updateUserOnlineStatus(currentUser.getUid(), false);
        }

        authDataSource.signOut();

        sessionManager.clearSession();
    }

    @Override
    public CompletableFuture<Result<Void>> updateUserOnlineStatus(boolean isOnline) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return CompletableFuture.completedFuture(new Result.Error<>("No user logged in"));
        }

        return firestoreDataSource.updateUserOnlineStatus(currentUser.getUid(), isOnline);
    }
}
