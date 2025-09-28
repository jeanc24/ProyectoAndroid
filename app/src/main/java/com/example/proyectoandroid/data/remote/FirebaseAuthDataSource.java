package com.example.proyectoandroid.data.remote;

import com.example.proyectoandroid.data.model.User;
import com.example.proyectoandroid.utils.Result;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

import java.util.concurrent.CompletableFuture;

public class FirebaseAuthDataSource {
    private final FirebaseAuth firebaseAuth;

    public FirebaseAuthDataSource() {
        this.firebaseAuth = FirebaseAuth.getInstance();
    }

    public CompletableFuture<Result<User>> registerUser(String email, String password, String displayName) {
        CompletableFuture<Result<User>> resultFuture = new CompletableFuture<>();

        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                    if (firebaseUser != null) {
                        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                .setDisplayName(displayName)
                                .build();

                        firebaseUser.updateProfile(profileUpdates)
                            .addOnCompleteListener(profileTask -> {
                                if (profileTask.isSuccessful()) {
                                    User user = new User(
                                            firebaseUser.getUid(),
                                            firebaseUser.getEmail(),
                                            displayName
                                    );
                                    resultFuture.complete(new Result.Success<>(user));
                                } else {
                                    resultFuture.complete(new Result.Error<>(
                                            profileTask.getException() != null ?
                                            profileTask.getException().getMessage() :
                                            "Failed to update profile"
                                    ));
                                }
                            });
                    } else {
                        resultFuture.complete(new Result.Error<>("User registration failed"));
                    }
                } else {
                    String errorMessage = task.getException() != null ?
                            task.getException().getMessage() :
                            "Registration failed";
                    resultFuture.complete(new Result.Error<>(errorMessage));
                }
            });

        return resultFuture;
    }

    public CompletableFuture<Result<User>> loginUser(String email, String password) {
        CompletableFuture<Result<User>> resultFuture = new CompletableFuture<>();

        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                    if (firebaseUser != null) {
                        User user = new User(
                                firebaseUser.getUid(),
                                firebaseUser.getEmail(),
                                firebaseUser.getDisplayName() != null ?
                                        firebaseUser.getDisplayName() :
                                        "User"
                        );
                        resultFuture.complete(new Result.Success<>(user));
                    } else {
                        resultFuture.complete(new Result.Error<>("Login failed"));
                    }
                } else {
                    String errorMessage = task.getException() != null ?
                            task.getException().getMessage() :
                            "Login failed";
                    resultFuture.complete(new Result.Error<>(errorMessage));
                }
            });

        return resultFuture;
    }

    public FirebaseUser getCurrentUser() {
        return firebaseAuth.getCurrentUser();
    }

    public boolean isUserLoggedIn() {
        return firebaseAuth.getCurrentUser() != null;
    }

    public void signOut() {
        firebaseAuth.signOut();
    }
}
