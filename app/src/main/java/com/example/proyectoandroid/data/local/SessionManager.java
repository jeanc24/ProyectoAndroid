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

    public SessionManager(Context context) {
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
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

    public void clearSession() {
        editor.clear();
        editor.apply();
    }

    public void updateUserField(String key, Object value) {
        User user = getUserData();
        if (user == null) return;

        try {
            switch (key) {
                case "displayName":
                    user.setDisplayName((String) value);
                    break;
                case "profileImageUrl":
                    user.setProfileImageUrl((String) value);
                    break;
                case "online":
                    user.setOnline((Boolean) value);
                    break;
                case "lastOnline":
                    user.setLastOnline((Long) value);
                    break;
            }
            saveUserSession(user);
        } catch (ClassCastException e) {

        }
    }
}
