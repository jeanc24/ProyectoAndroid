package com.example.proyectoandroid.data.repository.impl;

import android.content.Context;
import android.util.Log;
import com.example.proyectoandroid.data.local.SessionManager;
import com.example.proyectoandroid.data.model.User;
import com.example.proyectoandroid.data.remote.FirebaseAuthDataSource;
import com.example.proyectoandroid.data.remote.FirestoreDataSource;
import com.example.proyectoandroid.data.repository.AuthRepository;
import com.example.proyectoandroid.notifications.NotificationManager;
import com.example.proyectoandroid.utils.Result;
import java.util.concurrent.CompletableFuture;

public class AuthRepositoryImpl implements AuthRepository {

    private static final String TAG = "AuthRepositoryImpl";
    private final FirebaseAuthDataSource authDataSource;
    private final FirestoreDataSource firestoreDataSource;
    private final SessionManager sessionManager;
    private Context context;

    public AuthRepositoryImpl(FirebaseAuthDataSource authDataSource,
                              FirestoreDataSource firestoreDataSource,
                              SessionManager sessionManager) {
        this.authDataSource = authDataSource;
        this.firestoreDataSource = firestoreDataSource;
        this.sessionManager = sessionManager;
        this.context = sessionManager.getContext();
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
                                    if (!(firestoreResult instanceof Result.Success)) {
                                        String errorMessage = ((Result.Error<?>) firestoreResult).getErrorMessage();
                                        Log.w(TAG, "No se pudo guardar en Firestore: " + errorMessage);
                                    }
                                })
                                .exceptionally(throwable -> {
                                    Log.w(TAG, "Error Firestore: " + throwable.getMessage());
                                    return null;
                                });

                        resultFuture.complete(result);
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

                        sessionManager.saveUserSession(user);

                        NotificationManager notificationManager = new NotificationManager(context);
                        notificationManager.registerUserForNotifications(user.getUid());

                        firestoreDataSource.updateUserOnlineStatus(user.getUid(), true)
                                .thenRun(() -> {
                                    user.setOnline(true);
                                    sessionManager.saveUserSession(user);
                                })
                                .exceptionally(throwable -> {
                                    Log.w(TAG, "No se pudo actualizar estado online: " + throwable.getMessage());
                                    return null;
                                });

                        resultFuture.complete(result);
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
            return user;
        }

        return null;
    }

    public void logoutUser() {
        User currentUser = getCurrentUser();
        if (currentUser != null) {
            NotificationManager notificationManager = new NotificationManager(context);
            notificationManager.unregisterUserFromNotifications(currentUser.getUid());

            firestoreDataSource.updateUserOnlineStatus(currentUser.getUid(), false)
                    .exceptionally(throwable -> {
                        Log.w(TAG, "Error al actualizar estado offline: " + throwable.getMessage());
                        return null;
                    });
        }

        sessionManager.clearUserSession();
        authDataSource.signOut();
    }

    @Override
    public boolean isUserLoggedIn() {
        return authDataSource.isUserLoggedIn() && getCurrentUser() != null;
    }

    public CompletableFuture<Result<Void>> resetPassword(String email) {
        return authDataSource.resetPassword(email);
    }

    @Override
    public CompletableFuture<Result<Void>> updateUserOnlineStatus(boolean isOnline) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return CompletableFuture.completedFuture(
                new Result.Error<>("No user logged in")
            );
        }

        return firestoreDataSource.updateUserOnlineStatus(currentUser.getUid(), isOnline);
    }

    @Override
    public void signOut() {
        logoutUser();
    }
}