package com.example.proyectoandroid.data.model;

import com.google.firebase.firestore.DocumentId;

public class User {
    @DocumentId
    private String documentId; // Cambio importante aquí
    private String uid;
    private String email;
    private String displayName;
    private String profileImageUrl;
    private boolean online;
    private long lastOnline;
    private String fcmToken;

    public User() {
        // Constructor vacío requerido por Firebase
    }

    public User(String uid, String email, String displayName) {
        this.uid = uid;
        this.email = email;
        this.displayName = displayName;
        this.profileImageUrl = "";
        this.online = false;
        this.lastOnline = System.currentTimeMillis();
        this.fcmToken = "";
    }

    // Getters y setters
    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }

    public boolean isOnline() { return online; }
    public void setOnline(boolean online) { this.online = online; }

    public long getLastOnline() { return lastOnline; }
    public void setLastOnline(long lastOnline) { this.lastOnline = lastOnline; }

    public String getFcmToken() { return fcmToken; }
    public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }
}