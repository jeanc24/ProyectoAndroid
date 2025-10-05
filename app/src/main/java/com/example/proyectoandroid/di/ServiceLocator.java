package com.example.proyectoandroid.di;

import android.content.Context;

import com.example.proyectoandroid.data.local.SessionManager;
import com.example.proyectoandroid.data.remote.FirebaseAuthDataSource;
import com.example.proyectoandroid.data.remote.FirebaseStorageDataSource;
import com.example.proyectoandroid.data.remote.FirestoreDataSource;
import com.example.proyectoandroid.data.repository.AuthRepository;
import com.example.proyectoandroid.data.repository.ChatRepository;
import com.example.proyectoandroid.data.repository.MessageRepository;
import com.example.proyectoandroid.data.repository.impl.AuthRepositoryImpl;
import com.example.proyectoandroid.data.repository.impl.ChatRepositoryImpl;
import com.example.proyectoandroid.data.repository.impl.MessageRepositoryImpl;
import com.example.proyectoandroid.domain.usecase.CreateChatUseCase;
import com.example.proyectoandroid.domain.usecase.GetCurrentUserUseCase;
import com.example.proyectoandroid.domain.usecase.ListUserChatsUseCase;
import com.example.proyectoandroid.domain.usecase.ListenMessagesUseCase;
import com.example.proyectoandroid.domain.usecase.LoginUserUseCase;

public class ServiceLocator {
    private static volatile ServiceLocator INSTANCE = null;

    private Context applicationContext;

    private FirebaseAuthDataSource authDataSource;
    private FirestoreDataSource firestoreDataSource;
    private FirebaseStorageDataSource storageDataSource;
    private SessionManager sessionManager;

    private AuthRepository authRepository;
    private ChatRepository chatRepository;
    private MessageRepository messageRepository;

    private ServiceLocator(Context applicationContext) {
        this.applicationContext = applicationContext;
    }

    public static ServiceLocator getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (ServiceLocator.class) {
                if (INSTANCE == null) {
                    INSTANCE = new ServiceLocator(context.getApplicationContext());
                }
            }
        }
        return INSTANCE;
    }

    public static void resetInstance() {
        synchronized (ServiceLocator.class) {
            INSTANCE = null;
        }
    }

    public FirebaseAuthDataSource provideFirebaseAuthDataSource() {
        if (authDataSource == null) {
            authDataSource = new FirebaseAuthDataSource();
        }
        return authDataSource;
    }

    public FirestoreDataSource provideFirestoreDataSource() {
        if (firestoreDataSource == null) {
            firestoreDataSource = new FirestoreDataSource();
        }
        return firestoreDataSource;
    }

    public FirebaseStorageDataSource provideFirebaseStorageDataSource() {
        if (storageDataSource == null) {
            storageDataSource = new FirebaseStorageDataSource();
        }
        return storageDataSource;
    }

    public SessionManager provideSessionManager() {
        if (sessionManager == null) {
            sessionManager = new SessionManager(applicationContext);
        }
        return sessionManager;
    }

    public AuthRepository provideAuthRepository() {
        if (authRepository == null) {
            authRepository = new AuthRepositoryImpl(
                provideFirebaseAuthDataSource(),
                provideFirestoreDataSource(),
                provideSessionManager()
            );
        }
        return authRepository;
    }

    public ChatRepository provideChatRepository() {
        if (chatRepository == null) {
            chatRepository = new ChatRepositoryImpl(
                provideFirestoreDataSource(),
                provideAuthRepository()
            );
        }
        return chatRepository;
    }

    public MessageRepository provideMessageRepository() {
        if (messageRepository == null) {
            messageRepository = new MessageRepositoryImpl(
                provideFirestoreDataSource(),
                provideFirebaseStorageDataSource(),
                provideAuthRepository()
            );
        }
        return messageRepository;
    }

    public LoginUserUseCase provideLoginUserUseCase() {
        return new LoginUserUseCase(provideAuthRepository());
    }

    public GetCurrentUserUseCase provideGetCurrentUserUseCase() {
        return new GetCurrentUserUseCase(provideAuthRepository());
    }

    public ListUserChatsUseCase provideListUserChatsUseCase() {
        return new ListUserChatsUseCase(provideChatRepository());
    }

    public CreateChatUseCase provideCreateChatUseCase() {
        return new CreateChatUseCase(provideChatRepository());
    }

    public ListenMessagesUseCase provideListenMessagesUseCase() {
        return new ListenMessagesUseCase(provideMessageRepository());
    }
}
