package com.example.proyectoandroid;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.messaging.FirebaseMessaging;

public class ChatApplication extends Application {
    
    private static final String TAG = "ChatApplication";
    private static final int FCM_RETRY_DELAY_MS = 10000; // 10 segundos
    private static final int MAX_FCM_RETRIES = 3;
    private Handler mainHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        
        mainHandler = new Handler(Looper.getMainLooper());

        // Inicializar Firebase
        try {
            FirebaseApp.initializeApp(this);
            Log.d(TAG, "Firebase inicializado correctamente");
        } catch (Exception e) {
            Log.e(TAG, "Error inicializando Firebase", e);
        }

        // Crear canal de notificaciones inmediatamente
        createNotificationChannel();

        // Intentar actualizar token FCM, con reintentos
        updateFCMTokenWithRetry(0);
    }
    
    private void createNotificationChannel() {
        try {
            String channelId = getString(R.string.default_notification_channel_id);
            String channelName = getString(R.string.notification_channel_name);
            String channelDescription = getString(R.string.notification_channel_description);

            Log.d(TAG, "Creando canal de notificaciones: " + channelId);

            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            if (notificationManager != null) {
                // Crear el canal con prioridad alta para garantizar que se muestre
                NotificationChannel channel = new NotificationChannel(
                        channelId,
                        channelName,
                        NotificationManager.IMPORTANCE_HIGH);

                channel.setDescription(channelDescription);
                channel.enableLights(true);
                channel.enableVibration(true);
                channel.setVibrationPattern(new long[]{0, 250, 250, 250});
                channel.setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC);

                notificationManager.createNotificationChannel(channel);
                Log.d(TAG, "Canal de notificaciones creado exitosamente");
            } else {
                Log.e(TAG, "No se pudo obtener el NotificationManager");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error creando canal de notificaciones", e);
        }
    }

    private void updateFCMTokenWithRetry(int retryCount) {
        try {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            String uid = currentUser != null ? currentUser.getUid() : null;

            if (uid != null) {
                // Usuario autenticado, actualizar token
                FirebaseMessaging.getInstance().getToken()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            String token = task.getResult();
                            Log.d(TAG, "Token FCM obtenido: " + token);

                            // Actualizar en Firestore (incluso si la actualización falla,
                            // el token se guardó localmente y FCM lo usará)
                            try {
                                new com.example.proyectoandroid.notifications.NotificationManager(this)
                                    .updateUserFcmToken(uid, token);
                                Log.d(TAG, "Token FCM actualizado en Firestore");
                            } catch (Exception e) {
                                Log.e(TAG, "Error actualizando token en Firestore", e);
                                // Reintentar si hay un error de red
                                scheduleTokenUpdateRetry(uid, token, retryCount);
                            }
                        } else {
                            Log.e(TAG, "Error obteniendo token FCM", task.getException());

                            // Reintentar obtener token si no hemos superado el límite
                            if (retryCount < MAX_FCM_RETRIES) {
                                Log.d(TAG, "Programando reintento de obtención de token FCM #" + (retryCount + 1));
                                mainHandler.postDelayed(() -> updateFCMTokenWithRetry(retryCount + 1),
                                                      FCM_RETRY_DELAY_MS);
                            }
                        }
                    });
            } else {
                Log.d(TAG, "No hay usuario autenticado. El token FCM se actualizará tras el login.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error en updateFCMTokenWithRetry", e);
        }
    }

    private void scheduleTokenUpdateRetry(String uid, String token, int retryCount) {
        if (retryCount < MAX_FCM_RETRIES) {
            Log.d(TAG, "Programando reintento #" + (retryCount + 1) + " para actualizar token FCM en Firestore");
            mainHandler.postDelayed(() -> {
                try {
                    new com.example.proyectoandroid.notifications.NotificationManager(this)
                        .updateUserFcmToken(uid, token);
                    Log.d(TAG, "Token FCM actualizado en Firestore en el reintento #" + (retryCount + 1));
                } catch (Exception e) {
                    Log.e(TAG, "Error actualizando token en reintento #" + (retryCount + 1), e);
                    scheduleTokenUpdateRetry(uid, token, retryCount + 1);
                }
            }, FCM_RETRY_DELAY_MS);
        }
    }
}