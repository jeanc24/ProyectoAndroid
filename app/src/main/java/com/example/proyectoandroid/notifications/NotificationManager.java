package com.example.proyectoandroid.notifications;

import android.content.Context;
import android.util.Log;

import com.example.proyectoandroid.data.model.User;
import com.example.proyectoandroid.utils.FirebaseCollections;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.Map;

public class NotificationManager {

    private static final String TAG = "NotificationManager";
    private final FirebaseFirestore firestore;
    private final Context context;

    public NotificationManager(Context context) {
        this.context = context;
        this.firestore = FirebaseFirestore.getInstance();
    }

    public void registerUserForNotifications(String userId) {
        if (userId == null || userId.isEmpty()) {
            return;
        }

        FirebaseMessaging.getInstance().getToken()
            .addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    Log.w(TAG, "Error al obtener token FCM", task.getException());
                    return;
                }

                String token = task.getResult();
                updateUserFcmToken(userId, token);
            });
    }

    public void updateUserFcmToken(String userId, String token) {
        if (userId == null || userId.isEmpty() || token == null || token.isEmpty()) {
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("fcmToken", token);

        firestore.collection(FirebaseCollections.USERS)
            .document(userId)
            .update(updates)
            .addOnSuccessListener(aVoid ->
                Log.d(TAG, "Token FCM actualizado correctamente"))
            .addOnFailureListener(e ->
                Log.e(TAG, "Error al actualizar el token FCM", e));
    }

    public void unregisterUserFromNotifications(String userId) {
        if (userId == null || userId.isEmpty()) {
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("fcmToken", "");
        updates.put("online", false);
        updates.put("lastSeen", System.currentTimeMillis());

        firestore.collection(FirebaseCollections.USERS)
            .document(userId)
            .update(updates)
            .addOnSuccessListener(aVoid ->
                Log.d(TAG, "Token FCM eliminado correctamente"))
            .addOnFailureListener(e ->
                Log.e(TAG, "Error al eliminar el token FCM", e));
    }
}
