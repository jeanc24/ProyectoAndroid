package com.example.proyectoandroid.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class FCMPreferences {
    private static final String TAG = "FCMPreferences";
    private static final String PREFS_NAME = "fcm_prefs";
    private static final String KEY_FCM_TOKEN = "fcm_token";
    private static final String KEY_PENDING_UPLOAD = "pending_upload";
    private static final String KEY_USER_ID = "user_id";

    private final SharedPreferences prefs;

    public FCMPreferences(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void saveToken(String token) {
        if (token == null || token.isEmpty()) {
            return;
        }

        try {
            prefs.edit()
                .putString(KEY_FCM_TOKEN, token)
                .apply();
            Log.d(TAG, "Token FCM guardado localmente: " + token);
        } catch (Exception e) {
            Log.e(TAG, "Error guardando token FCM", e);
        }
    }

    public String getToken() {
        return prefs.getString(KEY_FCM_TOKEN, "");
    }

    public void markPendingUpload(String userId) {
        try {
            prefs.edit()
                .putBoolean(KEY_PENDING_UPLOAD, true)
                .putString(KEY_USER_ID, userId)
                .apply();
            Log.d(TAG, "Token marcado para sincronización pendiente (usuario: " + userId + ")");
        } catch (Exception e) {
            Log.e(TAG, "Error marcando token pendiente", e);
        }
    }

    public boolean hasPendingUpload() {
        return prefs.getBoolean(KEY_PENDING_UPLOAD, false);
    }

    public String getPendingUserId() {
        return prefs.getString(KEY_USER_ID, "");
    }

    public void clearPendingUpload() {
        try {
            prefs.edit()
                .putBoolean(KEY_PENDING_UPLOAD, false)
                .apply();
            Log.d(TAG, "Token marcado como sincronizado");
        } catch (Exception e) {
            Log.e(TAG, "Error al limpiar estado pendiente", e);
        }
    }
}
