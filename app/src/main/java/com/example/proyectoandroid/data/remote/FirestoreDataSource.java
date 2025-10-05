package com.example.proyectoandroid.data.remote;

import com.example.proyectoandroid.data.model.Chat;
import com.example.proyectoandroid.data.model.Message;
import com.example.proyectoandroid.data.model.User;
import com.example.proyectoandroid.utils.FirebaseCollections;
import com.example.proyectoandroid.utils.Result;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class FirestoreDataSource {

    public final FirebaseFirestore firestore;
    private final Map<String, ListenerRegistration> messageListeners = new HashMap<>();
    private final Map<String, ListenerRegistration> chatListeners = new HashMap<>();
    private final Map<String, ListenerRegistration> userStatusListeners = new HashMap<>();

    public FirestoreDataSource() {
        this.firestore = FirebaseFirestore.getInstance();
    }

    public CompletableFuture<Result<User>> createOrUpdateUser(User user) {
        CompletableFuture<Result<User>> resultFuture = new CompletableFuture<>();

        DocumentReference userRef = firestore.collection(FirebaseCollections.USERS).document(user.getUid());

        userRef.set(user)
            .addOnSuccessListener(aVoid -> resultFuture.complete(new Result.Success<>(user)))
            .addOnFailureListener(e -> resultFuture.complete(new Result.Error<>(e.getMessage())));

        return resultFuture;
    }

    public CompletableFuture<Result<User>> getUserById(String userId) {
        CompletableFuture<Result<User>> resultFuture = new CompletableFuture<>();

        DocumentReference userRef = firestore.collection(FirebaseCollections.USERS).document(userId);

        userRef.get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    User user = documentSnapshot.toObject(User.class);
                    resultFuture.complete(new Result.Success<>(user));
                } else {
                    resultFuture.complete(new Result.Error<>("User not found"));
                }
            })
            .addOnFailureListener(e -> resultFuture.complete(new Result.Error<>(e.getMessage())));

        return resultFuture;
    }

    public CompletableFuture<Result<Void>> updateUserOnlineStatus(String userId, boolean isOnline) {
        CompletableFuture<Result<Void>> resultFuture = new CompletableFuture<>();

        DocumentReference userRef = firestore.collection(FirebaseCollections.USERS).document(userId);

        Map<String, Object> updates = new HashMap<>();
        updates.put("online", isOnline);
        if (!isOnline) {
            updates.put("lastOnline", System.currentTimeMillis());
        }

        userRef.update(updates)
            .addOnSuccessListener(aVoid -> resultFuture.complete(new Result.Success<>(null)))
            .addOnFailureListener(e -> resultFuture.complete(new Result.Error<>(e.getMessage())));

        return resultFuture;
    }

    public ListenerRegistration addUserStatusListener(String userId, Consumer<User> onUserStatusChanged) {
        DocumentReference userRef = firestore.collection(FirebaseCollections.USERS).document(userId);

        ListenerRegistration registration = userRef.addSnapshotListener((snapshot, error) -> {
            if (error != null) {
                return;
            }

            if (snapshot != null && snapshot.exists()) {
                User user = snapshot.toObject(User.class);
                if (user != null) {
                    onUserStatusChanged.accept(user);
                }
            }
        });

        userStatusListeners.put(userId, registration);
        return registration;
    }

    public void removeUserStatusListener(String userId) {
        ListenerRegistration registration = userStatusListeners.get(userId);
        if (registration != null) {
            registration.remove();
            userStatusListeners.remove(userId);
        }
    }

    public CompletableFuture<Result<Chat>> createChat(Chat chat) {
        CompletableFuture<Result<Chat>> resultFuture = new CompletableFuture<>();

        DocumentReference chatRef = firestore.collection(FirebaseCollections.CHATS).document();
        String chatId = chatRef.getId();
        chat.setChatId(chatId);

        chatRef.set(chat)
            .addOnSuccessListener(aVoid -> {
                List<CompletableFuture<Void>> futures = new ArrayList<>();

                for (String userId : chat.getParticipantIds()) {
                    CompletableFuture<Void> future = new CompletableFuture<>();
                    futures.add(future);

                    DocumentReference userChatRef = firestore
                            .collection(FirebaseCollections.USER_CHATS)
                            .document(userId)
                            .collection("chats")
                            .document(chatId);

                    Map<String, Object> userChatData = new HashMap<>();
                    userChatData.put("chatId", chatId);
                    userChatData.put("timestamp", System.currentTimeMillis());

                    userChatRef.set(userChatData)
                        .addOnSuccessListener(aVoid2 -> future.complete(null))
                        .addOnFailureListener(future::completeExceptionally);
                }

                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenRun(() -> resultFuture.complete(new Result.Success<>(chat)))
                    .exceptionally(e -> {
                        resultFuture.complete(new Result.Error<>(e.getMessage()));
                        return null;
                    });
            })
            .addOnFailureListener(e -> resultFuture.complete(new Result.Error<>(e.getMessage())));

        return resultFuture;
    }

    public CompletableFuture<Result<Chat>> getChatById(String chatId) {
        CompletableFuture<Result<Chat>> resultFuture = new CompletableFuture<>();

        firestore.collection(FirebaseCollections.CHATS).document(chatId).get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    Chat chat = documentSnapshot.toObject(Chat.class);
                    resultFuture.complete(new Result.Success<>(chat));
                } else {
                    resultFuture.complete(new Result.Error<>("Chat not found"));
                }
            })
            .addOnFailureListener(e -> resultFuture.complete(new Result.Error<>(e.getMessage())));

        return resultFuture;
    }

    public CompletableFuture<Result<List<Chat>>> getUserChats(String userId) {
        CompletableFuture<Result<List<Chat>>> resultFuture = new CompletableFuture<>();

        firestore.collection(FirebaseCollections.CHATS)
            .whereArrayContains("participantIds", userId)
            .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                List<Chat> chats = new ArrayList<>();
                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    Chat chat = document.toObject(Chat.class);
                    chats.add(chat);
                }
                resultFuture.complete(new Result.Success<>(chats));
            })
            .addOnFailureListener(e -> resultFuture.complete(new Result.Error<>(e.getMessage())));

        return resultFuture;
    }

    public CompletableFuture<Result<Void>> updateChatLastMessage(String chatId, Message message) {
        CompletableFuture<Result<Void>> resultFuture = new CompletableFuture<>();

        DocumentReference chatRef = firestore.collection(FirebaseCollections.CHATS).document(chatId);

        Map<String, Object> updates = new HashMap<>();
        updates.put("lastMessageContent", message.getMessageType() == 1 ? "" : message.getContent());
        updates.put("lastMessageSenderId", message.getSenderId());
        updates.put("lastMessageSenderName", message.getSenderName() != null ? message.getSenderName() : "");
        updates.put("lastMessageSenderEmail", message.getSenderEmail() != null ? message.getSenderEmail() : "");
        updates.put("lastMessageTimestamp", FieldValue.serverTimestamp());
        updates.put("lastMessageRead", false);
        updates.put("lastMessageType", message.getMessageType());

        chatRef.update(updates)
            .addOnSuccessListener(aVoid -> resultFuture.complete(new Result.Success<>(null)))
            .addOnFailureListener(e -> resultFuture.complete(new Result.Error<>(e.getMessage())));

        return resultFuture;
    }

    public ListenerRegistration addUserChatsListener(String userId, Consumer<List<Chat>> onChatsChanged) {
        Query query = firestore.collection(FirebaseCollections.CHATS)
                .whereArrayContains("participantIds", userId)
                .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING);

        ListenerRegistration registration = query.addSnapshotListener((querySnapshots, error) -> {
            if (error != null) {
                return;
            }

            if (querySnapshots != null && !querySnapshots.isEmpty()) {
                List<Chat> chats = new ArrayList<>();
                for (QueryDocumentSnapshot doc : querySnapshots) {
                    Chat chat = doc.toObject(Chat.class);
                    chats.add(chat);
                }
                onChatsChanged.accept(chats);
            } else {
                onChatsChanged.accept(new ArrayList<>());
            }
        });

        String listenerKey = "userChats_" + userId;
        chatListeners.put(listenerKey, registration);
        return registration;
    }

    public void removeUserChatsListener(String userId) {
        String listenerKey = "userChats_" + userId;
        ListenerRegistration registration = chatListeners.get(listenerKey);
        if (registration != null) {
            registration.remove();
            chatListeners.remove(listenerKey);
        }
    }

    public CompletableFuture<Result<Message>> sendMessage(Message message) {
        CompletableFuture<Result<Message>> resultFuture = new CompletableFuture<>();

        DocumentReference messageRef = firestore
                .collection(FirebaseCollections.CHATS)
                .document(message.getChatId())
                .collection(FirebaseCollections.MESSAGES)
                .document();

        String messageId = messageRef.getId();
        message.setMessageId(messageId);

        messageRef.set(message)
            .addOnSuccessListener(aVoid -> {
                updateChatLastMessage(message.getChatId(), message)
                    .thenAccept(result -> {
                        if (result instanceof Result.Success) {
                            resultFuture.complete(new Result.Success<>(message));
                        } else {
                            String errorMessage = ((Result.Error<Void>) result).getErrorMessage();
                            resultFuture.complete(new Result.Error<>(errorMessage));
                        }
                    });
            })
            .addOnFailureListener(e -> resultFuture.complete(new Result.Error<>(e.getMessage())));

        return resultFuture;
    }

    public CompletableFuture<Result<List<Message>>> getChatMessages(String chatId, int limit) {
        CompletableFuture<Result<List<Message>>> resultFuture = new CompletableFuture<>();

        firestore.collection(FirebaseCollections.CHATS)
            .document(chatId)
            .collection(FirebaseCollections.MESSAGES)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(limit)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                List<Message> messages = new ArrayList<>();
                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    Message message = document.toObject(Message.class);
                    messages.add(message);
                }
                resultFuture.complete(new Result.Success<>(messages));
            })
            .addOnFailureListener(e -> resultFuture.complete(new Result.Error<>(e.getMessage())));

        return resultFuture;
    }

    public ListenerRegistration addChatMessagesListener(
            String chatId,
            int limit,
            Consumer<List<Message>> onInitialMessages,
            Consumer<Message> onNewMessages,
            Consumer<Message> onModifiedMessages) {

        Query query = firestore.collection(FirebaseCollections.CHATS)
                .document(chatId)
                .collection(FirebaseCollections.MESSAGES)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit);

        ListenerRegistration registration = query.addSnapshotListener((querySnapshots, error) -> {
            if (error != null) {
                return;
            }

            if (querySnapshots != null) {
                if (onInitialMessages != null && !querySnapshots.isEmpty()) {
                    List<Message> messages = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshots) {
                        messages.add(doc.toObject(Message.class));
                    }
                    onInitialMessages.accept(messages);
                }

                for (DocumentChange change : querySnapshots.getDocumentChanges()) {
                    Message message = change.getDocument().toObject(Message.class);

                    switch (change.getType()) {
                        case ADDED:
                            if (onNewMessages != null && change.getNewIndex() == 0) {
                                onNewMessages.accept(message);
                            }
                            break;
                        case MODIFIED:
                            if (onModifiedMessages != null) {
                                onModifiedMessages.accept(message);
                            }
                            break;
                    }
                }
            }
        });

        messageListeners.put(chatId, registration);
        return registration;
    }

    public void removeChatMessagesListener(String chatId) {
        ListenerRegistration registration = messageListeners.get(chatId);
        if (registration != null) {
            registration.remove();
            messageListeners.remove(chatId);
        }
    }

    public void removeAllListeners() {
        for (ListenerRegistration registration : messageListeners.values()) {
            registration.remove();
        }
        messageListeners.clear();

        for (ListenerRegistration registration : chatListeners.values()) {
            registration.remove();
        }
        chatListeners.clear();

        for (ListenerRegistration registration : userStatusListeners.values()) {
            registration.remove();
        }
        userStatusListeners.clear();
    }

    public CompletableFuture<Result<List<Message>>> getChatMessagesPaginated(
            String chatId, Date timestampCursor, int pageSize) {

        CompletableFuture<Result<List<Message>>> resultFuture = new CompletableFuture<>();

        Query query = firestore.collection(FirebaseCollections.CHATS)
                .document(chatId)
                .collection(FirebaseCollections.MESSAGES)
                .orderBy("timestamp", Query.Direction.DESCENDING);

        if (timestampCursor != null) {
            query = query.startAfter(timestampCursor);
        }

        query = query.limit(pageSize);

        query.get().addOnSuccessListener(queryDocumentSnapshots -> {
            List<Message> messages = new ArrayList<>();
            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                Message message = document.toObject(Message.class);
                messages.add(message);
            }
            resultFuture.complete(new Result.Success<>(messages));
        }).addOnFailureListener(e ->
            resultFuture.complete(new Result.Error<>(e.getMessage()))
        );

        return resultFuture;
    }

    public CompletableFuture<Result<Void>> markMessagesAsRead(String chatId, String userId) {
        CompletableFuture<Result<Void>> resultFuture = new CompletableFuture<>();

        firestore.collection(FirebaseCollections.CHATS)
            .document(chatId)
            .update("lastMessageRead", true)
            .addOnSuccessListener(aVoid -> {
                firestore.collection(FirebaseCollections.CHATS)
                    .document(chatId)
                    .collection(FirebaseCollections.MESSAGES)
                    .whereEqualTo("read", false)
                    .whereNotEqualTo("senderId", userId)
                    .get()
                    .addOnSuccessListener(querySnapshots -> {
                        if (querySnapshots.isEmpty()) {
                            resultFuture.complete(new Result.Success<>(null));
                            return;
                        }

                        List<CompletableFuture<Void>> futures = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : querySnapshots) {
                            CompletableFuture<Void> future = new CompletableFuture<>();
                            futures.add(future);

                            doc.getReference().update("read", true)
                                .addOnSuccessListener(a -> future.complete(null))
                                .addOnFailureListener(future::completeExceptionally);
                        }

                        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                            .thenRun(() -> resultFuture.complete(new Result.Success<>(null)))
                            .exceptionally(e -> {
                                resultFuture.complete(new Result.Error<>(e.getMessage()));
                                return null;
                            });
                    })
                    .addOnFailureListener(e -> resultFuture.complete(new Result.Error<>(e.getMessage())));
            })
            .addOnFailureListener(e -> resultFuture.complete(new Result.Error<>(e.getMessage())));

        return resultFuture;
    }
}
