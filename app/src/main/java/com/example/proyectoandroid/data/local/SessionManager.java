package com.example.proyectoandroid.data.local;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.proyectoandroid.data.model.User;
import com.google.gson.Gson;

public class SessionManager {
    private static final String PREFS_NAME = "ChatAppPreferences";
    private static final String KEY_USER = "user_data";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";

    private final SharedPreferences preferences;
    private final SharedPreferences.Editor editor;
    private final Gson gson;
    private final Context context;

    public SessionManager(Context context) {
        this.context = context.getApplicationContext();
        preferences = this.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        editor = preferences.edit();
        gson = new Gson();
    }

    public void saveUserSession(User user) {
        String userJson = gson.toJson(user);
        editor.putString(KEY_USER, userJson);
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.apply();
    }

    public User getUserData() {
        String userJson = preferences.getString(KEY_USER, null);
        if (userJson != null) {
            return gson.fromJson(userJson, User.class);
        }
        return null;
    }

    public boolean isLoggedIn() {
        return preferences.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public void clearUserSession() {
        editor.clear();
        editor.apply();
    }

    public void updateUserField(String key, Object value) {
        User user = getUserData();
        if (user == null) return;

        if (key.equals("online")) {
            user.setOnline((Boolean) value);
        } else if (key.equals("lastOnline")) {
            user.setLastOnline((Long) value);
        } else if (key.equals("fcmToken")) {
            user.setFcmToken((String) value);
        }

        saveUserSession(user);
    }

    public Context getContext() {
        return context;
    }
}
