package com.example.proyectoandroid;

import android.util.Log;

import com.example.proyectoandroid.data.model.Chat;
import com.example.proyectoandroid.data.model.Message;
import com.example.proyectoandroid.data.model.User;
import com.example.proyectoandroid.data.remote.FirebaseAuthDataSource;
import com.example.proyectoandroid.data.repository.AuthRepository;
import com.example.proyectoandroid.data.repository.ChatRepository;
import com.example.proyectoandroid.data.repository.MessageRepository;
import com.example.proyectoandroid.utils.Result;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

public class BackendTester {
    private static final String TAG = "BackendTester";

    private static BackendTester INSTANCE;

    private static final String TEST_USER_SEED = UUID.randomUUID().toString().substring(0, 8);
    private static final String TEST_EMAIL = "test" + TEST_USER_SEED + "@example.com";
    private static final String TEST_PASSWORD = "Test123!";
    private static final String TEST_DISPLAY_NAME = "Test User";

    private final FirebaseAuthDataSource authDataSource;
    private final AuthRepository authRepository;
    private final ChatRepository chatRepository;
    private final MessageRepository messageRepository;

    private String currentUserId = null;
    private String otherUserId = null;
    private String otherUserEmail = null;
    private String otherUserPassword = null;

    private List<ListenerRegistration> activeListeners = new ArrayList<>();

    public static synchronized BackendTester getInstance(
            AuthRepository authRepository,
            ChatRepository chatRepository,
            MessageRepository messageRepository) {
        if (INSTANCE == null) {
            INSTANCE = new BackendTester(authRepository, chatRepository, messageRepository);
        }
        return INSTANCE;
    }

    private BackendTester() {
        this.authDataSource = new FirebaseAuthDataSource();
        this.authRepository = null;
        this.chatRepository = null;
        this.messageRepository = null;
    }

    private BackendTester(AuthRepository authRepository) {
        this.authDataSource = new FirebaseAuthDataSource();
        this.authRepository = authRepository;
        this.chatRepository = null;
        this.messageRepository = null;
    }

    private BackendTester(AuthRepository authRepository, ChatRepository chatRepository, MessageRepository messageRepository) {
        this.authDataSource = new FirebaseAuthDataSource();
        this.authRepository = authRepository;
        this.chatRepository = chatRepository;
        this.messageRepository = messageRepository;
    }

    public boolean testAuthentication() {
        Log.d(TAG, "Starting authentication tests...");
        Log.d(TAG, "Using test email: " + TEST_EMAIL);

        try {
            boolean registrationSuccess = testUserRegistration();
            if (!registrationSuccess) {
                Log.e(TAG, "User registration test failed");
                return false;
            }

            boolean loginSuccess = testUserLogin();
            if (!loginSuccess) {
                Log.e(TAG, "User login test failed");
                return false;
            }

            boolean sessionSuccess = testSessionPersistence();
            if (!sessionSuccess) {
                Log.e(TAG, "Session persistence test failed");
                return false;
            }

            Log.d(TAG, "Authentication tests passed successfully!");
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Error during authentication tests: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean testMessaging() {
        Log.d(TAG, "Starting messaging tests...");

        if (chatRepository == null || messageRepository == null) {
            Log.e(TAG, "Chat or Message repository is null. Cannot run messaging tests.");
            return false;
        }

        try {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser == null) {
                Log.d(TAG, "Not logged in yet, logging in first...");
                Result<User> result = authDataSource.loginUser(TEST_EMAIL, TEST_PASSWORD).get(30, TimeUnit.SECONDS);
                if (result instanceof Result.Error) {
                    Log.e(TAG, "Login failed: " + ((Result.Error<User>) result).getErrorMessage());
                    return false;
                }

                Thread.sleep(1000);

                if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                    Log.e(TAG, "Login successful but Firebase user is null");
                    return false;
                }
            }

            String chatId = testCreateChat();
            if (chatId == null) {
                Log.e(TAG, "Chat creation test failed");
                return false;
            }

            boolean messagingSuccess = testSendReceiveMessages(chatId);
            if (!messagingSuccess) {
                Log.e(TAG, "Send/receive messages test failed");
                return false;
            }

            boolean listenersSuccess = testMessageListeners(chatId);
            if (!listenersSuccess) {
                Log.e(TAG, "Message listeners test failed");
                return false;
            }

            boolean paginationSuccess = testMessagePagination(chatId);
            if (!paginationSuccess) {
                Log.e(TAG, "Message pagination test failed");
                return false;
            }

            Log.d(TAG, "Messaging tests passed successfully!");
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Error during messaging tests: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean testRealtimeFeatures() {
        Log.d(TAG, "Starting real-time features tests...");

        if (chatRepository == null) {
            Log.e(TAG, "Chat repository is null. Cannot run real-time tests.");
            return false;
        }

        try {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser == null) {
                Log.d(TAG, "Not logged in yet, logging in first...");
                Result<User> result = authDataSource.loginUser(TEST_EMAIL, TEST_PASSWORD).get(30, TimeUnit.SECONDS);
                if (result instanceof Result.Error) {
                    Log.e(TAG, "Login failed: " + ((Result.Error<User>) result).getErrorMessage());
                    return false;
                }

                Thread.sleep(1000);

                if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                    Log.e(TAG, "Login successful but Firebase user is null");
                    return false;
                }

                createOrUpdateUserDocument();
            }

            boolean onlineStatusSuccess = testOnlineOfflineStatus();
            if (!onlineStatusSuccess) {
                Log.e(TAG, "Online/offline status test failed");
                return false;
            }

            boolean chatListenersSuccess = testChatListeners();
            if (!chatListenersSuccess) {
                Log.e(TAG, "Chat listeners test failed");
                return false;
            }

            Log.d(TAG, "Real-time features tests passed successfully!");
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Error during real-time features tests: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private void createOrUpdateUserDocument() {
        try {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) {
                Log.e(TAG, "Cannot create user document: Not authenticated");
                return;
            }

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            DocumentReference userRef = db.collection("users").document(user.getUid());

            Map<String, Object> userData = new HashMap<>();
            userData.put("uid", user.getUid());
            userData.put("email", user.getEmail());
            userData.put("displayName", user.getDisplayName() != null ? user.getDisplayName() : "Test User");
            userData.put("online", true);
            userData.put("lastOnline", System.currentTimeMillis());

            com.google.android.gms.tasks.Tasks.await(userRef.set(userData), 10, TimeUnit.SECONDS);
            Log.d(TAG, "User document created/updated successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error creating/updating user document: " + e.getMessage());
        }
    }

    private boolean testUserRegistration() throws ExecutionException, InterruptedException, TimeoutException {
        Log.d(TAG, "Testing user registration...");

        try {
            Result<User> loginResult = authDataSource.loginUser(TEST_EMAIL, TEST_PASSWORD).get(30, TimeUnit.SECONDS);
            if (loginResult instanceof Result.Success) {
                User user = ((Result.Success<User>) loginResult).getData();
                currentUserId = user.getUid();
                Log.d(TAG, "User already exists, using existing user: " + currentUserId);
                return true;
            }
        } catch (Exception e) {
            Log.d(TAG, "Login attempt failed, will try registration: " + e.getMessage());
        }

        CompletableFuture<Result<User>> resultFuture = authDataSource.registerUser(
                TEST_EMAIL, TEST_PASSWORD, TEST_DISPLAY_NAME);

        Result<User> result = resultFuture.get(30, TimeUnit.SECONDS);

        if (result instanceof Result.Success) {
            User user = ((Result.Success<User>) result).getData();
            currentUserId = user.getUid();
            Log.d(TAG, "User registration successful! User ID: " + currentUserId);

            createOrUpdateUserDocument();
            return true;
        } else {
            String errorMessage = ((Result.Error<User>) result).getErrorMessage();
            Log.e(TAG, "User registration failed: " + errorMessage);
            return false;
        }
    }

    private boolean testUserLogin() throws ExecutionException, InterruptedException, TimeoutException {
        Log.d(TAG, "Testing user login...");

        authDataSource.signOut();

        Thread.sleep(1000);

        CompletableFuture<Result<User>> resultFuture = authDataSource.loginUser(
                TEST_EMAIL, TEST_PASSWORD);

        Result<User> result = resultFuture.get(30, TimeUnit.SECONDS);

        if (result instanceof Result.Success) {
            User user = ((Result.Success<User>) result).getData();
            Log.d(TAG, "User login successful! User ID: " + user.getUid());

            if (currentUserId != null && currentUserId.equals(user.getUid())) {
                Log.d(TAG, "User ID verification passed");
                return true;
            } else {
                Log.e(TAG, "User ID verification failed. Expected: " + currentUserId + ", Got: " + user.getUid());
                return false;
            }
        } else {
            String errorMessage = ((Result.Error<User>) result).getErrorMessage();
            Log.e(TAG, "User login failed: " + errorMessage);
            return false;
        }
    }

    private boolean testSessionPersistence() throws ExecutionException, InterruptedException, TimeoutException {
        Log.d(TAG, "Testing session persistence...");

        if (authRepository == null) {
            Log.e(TAG, "Auth repository is null. Cannot test session persistence.");
            return false;
        }

        if (authRepository.getCurrentUser() == null) {
            CompletableFuture<Result<User>> resultFuture = authDataSource.loginUser(
                    TEST_EMAIL, TEST_PASSWORD);
            Result<User> result = resultFuture.get(30, TimeUnit.SECONDS);
            if (result instanceof Result.Error) {
                Log.e(TAG, "Login failed: " + ((Result.Error<User>) result).getErrorMessage());
                return false;
            }

            Thread.sleep(1000);
        }

        User originalUser = authRepository.getCurrentUser();
        if (originalUser == null) {
            Log.e(TAG, "Failed to get current user");
            return false;
        }

        authDataSource.signOut();

        Thread.sleep(1000);

        User sessionUser = authRepository.getCurrentUser();
        if (sessionUser == null) {
            Log.e(TAG, "Session persistence failed - no user found after signout");
            return false;
        }

        boolean success = sessionUser.getUid().equals(originalUser.getUid());
        if (success) {
            Log.d(TAG, "Session persistence test passed");
        } else {
            Log.e(TAG, "Session persistence failed - UIDs don't match");
        }

        return success;
    }

    private String testCreateChat() throws ExecutionException, InterruptedException, TimeoutException {
        Log.d(TAG, "Testing chat creation...");

        if (otherUserId != null) {
            Log.d(TAG, "Using existing test user: " + otherUserId);
        } else {
            otherUserEmail = "other" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
            otherUserPassword = "Other123!";
            String otherDisplayName = "Other Test User";

            Result<User> registerResult = authDataSource.registerUser(
                    otherUserEmail, otherUserPassword, otherDisplayName).get(30, TimeUnit.SECONDS);

            if (registerResult instanceof Result.Error) {
                Log.e(TAG, "Failed to create other test user: "
                        + ((Result.Error<User>) registerResult).getErrorMessage());
                return null;
            }

            User otherUser = ((Result.Success<User>) registerResult).getData();
            otherUserId = otherUser.getUid();

            try {
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                DocumentReference userRef = db.collection("users").document(otherUserId);

                Map<String, Object> userData = new HashMap<>();
                userData.put("uid", otherUserId);
                userData.put("email", otherUserEmail);
                userData.put("displayName", otherDisplayName);
                userData.put("online", false);
                userData.put("lastOnline", System.currentTimeMillis());

                com.google.android.gms.tasks.Tasks.await(userRef.set(userData), 10, TimeUnit.SECONDS);
                Log.d(TAG, "Other user document created successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error creating other user document: " + e.getMessage());
            }
        }

        try {
            FirebaseUser currentAuthUser = FirebaseAuth.getInstance().getCurrentUser();

            if (currentAuthUser != null && currentAuthUser.getEmail() != null &&
                currentAuthUser.getEmail().equals(TEST_EMAIL)) {
                Log.d(TAG, "Already logged in as test user: " + currentAuthUser.getUid());
            } else {
                if (currentAuthUser != null) {
                    Log.d(TAG, "Currently logged in as: " + currentAuthUser.getEmail() +
                          ", logging out first");
                    authDataSource.signOut();
                }

                Thread.sleep(1000);

                Log.d(TAG, "Logging in as test user " + TEST_EMAIL + " with password " + TEST_PASSWORD);

                Result<User> loginResult = authDataSource.loginUser(
                        TEST_EMAIL, TEST_PASSWORD).get(30, TimeUnit.SECONDS);

                if (loginResult instanceof Result.Error) {
                    Log.e(TAG, "Failed to sign back in as test user: "
                            + ((Result.Error<User>) loginResult).getErrorMessage());
                    return null;
                }

                Thread.sleep(1000);
            }

            currentAuthUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentAuthUser == null) {
                Log.e(TAG, "Failed to authenticate as test user");
                return null;
            }

            Log.d(TAG, "Current Firebase user for chat creation: " + currentAuthUser.getUid());

            createOrUpdateUserDocument();

            Log.d(TAG, "Creating chat with other user: " + otherUserId);
            Result<Chat> chatResult = chatRepository.createChat(otherUserId).get(30, TimeUnit.SECONDS);

            if (chatResult instanceof Result.Error) {
                Log.e(TAG, "Failed to create chat: "
                        + ((Result.Error<Chat>) chatResult).getErrorMessage());
                return null;
            }

            Chat chat = ((Result.Success<Chat>) chatResult).getData();
            Log.d(TAG, "Chat created successfully with ID: " + chat.getChatId());

            List<String> participants = chat.getParticipantIds();
            if (!participants.contains(currentAuthUser.getUid()) || !participants.contains(otherUserId)) {
                Log.e(TAG, "Chat does not contain both participants!");
                return null;
            }

            return chat.getChatId();
        } catch (Exception e) {
            Log.e(TAG, "Error in chat creation process: " + e.getMessage(), e);
            return null;
        }
    }

    private boolean testSendReceiveMessages(String chatId) throws ExecutionException, InterruptedException, TimeoutException {
        Log.d(TAG, "Testing send/receive messages in chat " + chatId);

        String messageText = "Test message " + System.currentTimeMillis();
        Result<Message> sendResult = messageRepository.sendTextMessage(
                chatId, messageText).get(30, TimeUnit.SECONDS);

        if (sendResult instanceof Result.Error) {
            Log.e(TAG, "Failed to send message: "
                    + ((Result.Error<Message>) sendResult).getErrorMessage());
            return false;
        }

        Message sentMessage = ((Result.Success<Message>) sendResult).getData();
        Log.d(TAG, "Message sent successfully with ID: " + sentMessage.getMessageId());

        Result<List<Message>> messagesResult = messageRepository.getChatMessages(
                chatId, 10).get(30, TimeUnit.SECONDS);

        if (messagesResult instanceof Result.Error) {
            Log.e(TAG, "Failed to retrieve messages: "
                    + ((Result.Error<List<Message>>) messagesResult).getErrorMessage());
            return false;
        }

        List<Message> messages = ((Result.Success<List<Message>>) messagesResult).getData();

        boolean found = false;
        for (Message message : messages) {
            if (message.getMessageId().equals(sentMessage.getMessageId())) {
                found = true;
                break;
            }
        }

        if (!found) {
            Log.e(TAG, "Sent message not found in retrieved messages!");
            return false;
        }

        Log.d(TAG, "Send/receive message test passed!");
        return true;
    }

    private boolean testMessageListeners(String chatId) throws ExecutionException, InterruptedException, TimeoutException {
        Log.d(TAG, "Testing message listeners for chat " + chatId);

        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicBoolean listenerTriggered = new AtomicBoolean(false);

        ListenerRegistration listener = messageRepository.addChatMessagesListener(
                chatId,
                10,
                messages -> {
                    Log.d(TAG, "Message listener triggered with " + messages.size() + " messages");
                    listenerTriggered.set(true);
                    latch.countDown();
                },
                newMessage -> {
                    Log.d(TAG, "New message received: " + newMessage.getMessageId());
                },
                modifiedMessage -> {
                    Log.d(TAG, "Message modified: " + modifiedMessage.getMessageId());
                });

        activeListeners.add(listener);

        String messageText = "Listener test message " + System.currentTimeMillis();
        Result<Message> sendResult = messageRepository.sendTextMessage(
                chatId, messageText).get(30, TimeUnit.SECONDS);

        if (sendResult instanceof Result.Error) {
            Log.e(TAG, "Failed to send test message: "
                    + ((Result.Error<Message>) sendResult).getErrorMessage());
            return false;
        }

        boolean triggered = latch.await(10, TimeUnit.SECONDS);

        if (!triggered || !listenerTriggered.get()) {
            Log.e(TAG, "Message listener was not triggered within timeout!");
            return false;
        }

        Log.d(TAG, "Message listener test passed!");
        return true;
    }

    private boolean testMessagePagination(String chatId) throws ExecutionException, InterruptedException, TimeoutException {
        Log.d(TAG, "Testing message pagination for chat " + chatId);

        int testMessageCount = 5;
        for (int i = 0; i < testMessageCount; i++) {
            String messageText = "Pagination test message " + i;
            Result<Message> sendResult = messageRepository.sendTextMessage(
                    chatId, messageText).get(30, TimeUnit.SECONDS);

            if (sendResult instanceof Result.Error) {
                Log.e(TAG, "Failed to send test message " + i + ": "
                        + ((Result.Error<Message>) sendResult).getErrorMessage());
                return false;
            }

            Thread.sleep(100);
        }

        Result<List<Message>> page1Result = messageRepository.getChatMessages(
                chatId, 2).get(30, TimeUnit.SECONDS);

        if (page1Result instanceof Result.Error) {
            Log.e(TAG, "Failed to get first page of messages: "
                    + ((Result.Error<List<Message>>) page1Result).getErrorMessage());
            return false;
        }

        List<Message> page1 = ((Result.Success<List<Message>>) page1Result).getData();

        if (page1.size() != 2) {
            Log.e(TAG, "Expected 2 messages in first page, but got " + page1.size());
            return false;
        }

        Message lastMessage = page1.get(page1.size() - 1);

        Result<List<Message>> page2Result = messageRepository.getChatMessagesPaginated(
                chatId, lastMessage.getTimestamp(), 2).get(30, TimeUnit.SECONDS);

        if (page2Result instanceof Result.Error) {
            Log.e(TAG, "Failed to get second page of messages: "
                    + ((Result.Error<List<Message>>) page2Result).getErrorMessage());
            return false;
        }

        List<Message> page2 = ((Result.Success<List<Message>>) page2Result).getData();

        if (page2.isEmpty()) {
            Log.e(TAG, "Second page is empty!");
            return false;
        }

        boolean hasDuplicates = false;
        for (Message msg1 : page1) {
            for (Message msg2 : page2) {
                if (msg1.getMessageId().equals(msg2.getMessageId())) {
                    hasDuplicates = true;
                    break;
                }
            }
        }

        if (hasDuplicates) {
            Log.e(TAG, "Found duplicate messages across pages!");
            return false;
        }

        Log.d(TAG, "Message pagination test passed!");
        return true;
    }

    private boolean testOnlineOfflineStatus() throws ExecutionException, InterruptedException, TimeoutException {
        Log.d(TAG, "Testing online/offline status");

        FirebaseUser currentAuthUser = FirebaseAuth.getInstance().getCurrentUser();
        Log.d(TAG, "Current Firebase user before online status test: " +
              (currentAuthUser != null ? currentAuthUser.getUid() : "null"));

        if (currentAuthUser == null) {
            Log.e(TAG, "Cannot test online status: No authenticated user");
            return false;
        }

        Result<Void> onlineResult = chatRepository.updateCurrentUserOnlineStatus(true)
                .get(30, TimeUnit.SECONDS);

        if (onlineResult instanceof Result.Error) {
            Log.e(TAG, "Failed to set user online: "
                    + ((Result.Error<Void>) onlineResult).getErrorMessage());
            return false;
        }

        User currentUser = authRepository.getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "Failed to get current user");
            return false;
        }

        Result<Void> offlineResult = chatRepository.updateCurrentUserOnlineStatus(false)
                .get(30, TimeUnit.SECONDS);

        if (offlineResult instanceof Result.Error) {
            Log.e(TAG, "Failed to set user offline: "
                    + ((Result.Error<Void>) offlineResult).getErrorMessage());
            return false;
        }

        Log.d(TAG, "Online/offline status test passed!");
        return true;
    }

    private boolean testChatListeners() throws InterruptedException {
        Log.d(TAG, "Testing chat listeners");

        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicBoolean listenerTriggered = new AtomicBoolean(false);

        ListenerRegistration listener = chatRepository.addUserChatsListener(chats -> {
            Log.d(TAG, "Chat listener triggered with " + chats.size() + " chats");
            listenerTriggered.set(true);
            latch.countDown();
        });

        activeListeners.add(listener);

        boolean triggered = latch.await(10, TimeUnit.SECONDS);

        if (!triggered || !listenerTriggered.get()) {
            Log.e(TAG, "Chat listener was not triggered within timeout!");
            return false;
        }

        Log.d(TAG, "Chat listener test passed!");
        return true;
    }

    public void cleanUp() {
        Log.d(TAG, "Cleaning up test resources...");

        for (ListenerRegistration listener : activeListeners) {
            if (listener != null) {
                listener.remove();
            }
        }
        activeListeners.clear();

        if (chatRepository != null) {
            chatRepository.removeAllListeners();
        }
    }
}
