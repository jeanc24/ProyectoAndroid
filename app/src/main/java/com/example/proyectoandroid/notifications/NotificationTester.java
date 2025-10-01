package com.example.proyectoandroid.notifications;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.example.proyectoandroid.R;
import com.example.proyectoandroid.chat.ChatActivity;
import com.example.proyectoandroid.data.model.User;
import com.example.proyectoandroid.data.repository.AuthRepository;
import com.example.proyectoandroid.di.ServiceLocator;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.RemoteMessage;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class NotificationTester {

    private static final String TAG = "NotificationTester";
    private final Context context;
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PASSWORD = "Test123!";

    public NotificationTester(Context context) {
        this.context = context;
    }

    public CompletableFuture<Boolean> ensureAuthenticated() {
        CompletableFuture<Boolean> resultFuture = new CompletableFuture<>();

        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser != null) {
            Log.d(TAG, "Usuario ya autenticado: " + currentUser.getUid());
            ensureUserDocumentExists(currentUser)
                .thenAccept(exists -> resultFuture.complete(exists));
            return resultFuture;
        }

        Log.d(TAG, "No hay usuario autenticado. Intentando registrar un usuario de prueba...");
        auth.createUserWithEmailAndPassword(TEST_EMAIL, TEST_PASSWORD)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.d(TAG, "Usuario de prueba creado exitosamente");
                    FirebaseUser user = auth.getCurrentUser();
                    createUserDocument(user)
                        .thenAccept(created -> resultFuture.complete(created));
                } else {
                    Log.d(TAG, "No se pudo crear usuario. Intentando iniciar sesión...");
                    auth.signInWithEmailAndPassword(TEST_EMAIL, TEST_PASSWORD)
                        .addOnCompleteListener(loginTask -> {
                            if (loginTask.isSuccessful()) {
                                Log.d(TAG, "Inicio de sesión exitoso");
                                FirebaseUser user = auth.getCurrentUser();
                                ensureUserDocumentExists(user)
                                    .thenAccept(exists -> resultFuture.complete(exists));
                            } else {
                                Log.e(TAG, "No se pudo autenticar: " + loginTask.getException().getMessage());
                                resultFuture.complete(false);
                            }
                        });
                }
            });

        return resultFuture;
    }

    private CompletableFuture<Boolean> ensureUserDocumentExists(FirebaseUser user) {
        CompletableFuture<Boolean> resultFuture = new CompletableFuture<>();
        
        if (user == null) {
            resultFuture.complete(false);
            return resultFuture;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(user.getUid()).get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    if (task.getResult().exists()) {
                        Log.d(TAG, "Documento de usuario ya existe en Firestore");
                        resultFuture.complete(true);
                    } else {
                        Log.d(TAG, "Documento de usuario no existe, creándolo...");
                        createUserDocument(user)
                            .thenAccept(created -> resultFuture.complete(created));
                    }
                } else {
                    Log.e(TAG, "Error verificando documento de usuario", task.getException());
                    resultFuture.complete(false);
                }
            });
        
        return resultFuture;
    }

    private CompletableFuture<Boolean> createUserDocument(FirebaseUser user) {
        CompletableFuture<Boolean> resultFuture = new CompletableFuture<>();
        
        if (user == null) {
            resultFuture.complete(false);
            return resultFuture;
        }
        
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> userData = new HashMap<>();
        userData.put("uid", user.getUid());
        userData.put("email", user.getEmail());
        userData.put("displayName", user.getDisplayName() != null ? user.getDisplayName() : "Usuario de Prueba");
        userData.put("photoUrl", user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "");
        userData.put("createdAt", System.currentTimeMillis());
        userData.put("lastSeen", System.currentTimeMillis());
        userData.put("online", true);

        FirebaseMessaging.getInstance().getToken()
            .addOnCompleteListener(tokenTask -> {
                if (tokenTask.isSuccessful()) {
                    String token = tokenTask.getResult();
                    userData.put("fcmToken", token);

                    db.collection("users").document(user.getUid())
                        .set(userData)
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Documento de usuario creado exitosamente en Firestore");
                            resultFuture.complete(true);
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error al crear documento de usuario", e);
                            resultFuture.complete(false);
                        });
                } else {
                    Log.e(TAG, "Error obteniendo token FCM para el nuevo usuario", tokenTask.getException());
                    userData.put("fcmToken", "");
                    db.collection("users").document(user.getUid())
                        .set(userData)
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Documento de usuario creado sin token FCM");
                            resultFuture.complete(true);
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error al crear documento de usuario", e);
                            resultFuture.complete(false);
                        });
                }
            });
        
        return resultFuture;
    }

    public CompletableFuture<Boolean> testNotification() {
        CompletableFuture<Boolean> resultFuture = new CompletableFuture<>();

        ensureAuthenticated().thenAccept(authenticated -> {
            if (!authenticated) {
                Log.e(TAG, "Error: No se pudo autenticar para probar notificaciones");
                resultFuture.complete(false);
                return;
            }

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) {
                Log.e(TAG, "Error: No hay usuario autenticado para probar notificaciones");
                resultFuture.complete(false);
                return;
            }

            Log.d(TAG, "Usuario autenticado: " + user.getUid());

            FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.e(TAG, "Error obteniendo token FCM para prueba", task.getException());
                        resultFuture.complete(false);
                        return;
                    }

                    String token = task.getResult();
                    Log.d(TAG, "Token FCM para prueba: " + token);

                    com.example.proyectoandroid.notifications.NotificationManager notificationManager =
                            new com.example.proyectoandroid.notifications.NotificationManager(context);
                    notificationManager.updateUserFcmToken(user.getUid(), token);

                    Map<String, String> data = new HashMap<>();
                    data.put("title", "Notificación de prueba");
                    data.put("body", "Esta es una notificación de prueba para verificar FCM");
                    data.put("chatId", "test_chat_id");
                    data.put("senderId", "test_sender_id");

                    try {
                        Log.d(TAG, "Simulando notificación entrante...");

                        String title = data.get("title");
                        String body = data.get("body");
                        String chatId = data.get("chatId");

                        sendLocalNotification(title, body, chatId);

                        Log.d(TAG, "Notificación de prueba procesada correctamente");
                        resultFuture.complete(true);
                    } catch (Exception e) {
                        Log.e(TAG, "Error al procesar notificación de prueba", e);
                        resultFuture.complete(false);
                    }
                });
        });

        return resultFuture;
    }

    private void sendLocalNotification(String title, String messageBody, String chatId) {
        Log.d(TAG, "Enviando notificación local: " + title);

        Intent intent = new Intent(context, ChatActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("chatId", chatId);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        String channelId = context.getString(R.string.default_notification_channel_id);
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(context, channelId)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle(title)
                        .setContentText(messageBody)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent)
                        .setColor(context.getResources().getColor(R.color.notification_color, context.getTheme()));

        android.app.NotificationManager notificationManager =
                (android.app.NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    context.getString(R.string.notification_channel_name),
                    android.app.NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription(context.getString(R.string.notification_channel_description));
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify(100, notificationBuilder.build());
        Log.d(TAG, "Notificación local enviada exitosamente");
    }

    public CompletableFuture<Boolean> verifyFcmTokenRegistration() {
        CompletableFuture<Boolean> resultFuture = new CompletableFuture<>();

        ensureAuthenticated().thenAccept(authenticated -> {
            if (!authenticated) {
                Log.e(TAG, "Error: No se pudo autenticar para verificar token FCM");
                resultFuture.complete(false);
                return;
            }

            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser == null) {
                Log.e(TAG, "Error: No hay usuario autenticado para verificar token FCM");
                resultFuture.complete(false);
                return;
            }

            com.example.proyectoandroid.notifications.NotificationManager notificationManager =
                    new com.example.proyectoandroid.notifications.NotificationManager(context);
            notificationManager.registerUserForNotifications(currentUser.getUid());

            try {
                Thread.sleep(1000);
                resultFuture.complete(true);
            } catch (InterruptedException e) {
                Log.e(TAG, "Error al esperar verificación de token", e);
                resultFuture.complete(false);
            }
        });

        return resultFuture;
    }
}