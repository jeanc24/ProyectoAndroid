package com.example.proyectoandroid.data.model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.PropertyName;

public class User {
    @DocumentId
    private String uid;
    private String email;
    private String displayName;
    private String profileImageUrl;
    private long lastOnline;
    private boolean online;
    private String fcmToken;

    public User() {}

    public User(String uid, String email, String displayName) {
        this.uid = uid;
        this.email = email;
        this.displayName = displayName;
        this.lastOnline = System.currentTimeMillis();
        this.online = false;
        this.profileImageUrl = "";
        this.fcmToken = "";
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    @PropertyName("last_online")
    public long getLastOnline() {
        return lastOnline;
    }

    @PropertyName("last_online")
    public void setLastOnline(long lastOnline) {
        this.lastOnline = lastOnline;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public String getFcmToken() {
        return fcmToken;
    }

    public void setFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }
}
