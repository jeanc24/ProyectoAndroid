package com.example.proyectoandroid.data.model;

import com.google.firebase.firestore.DocumentId;

public class User {

    @DocumentId
    private String documentId;
    private String uid;
    private String email;
    private String displayName;
    private String photoUrl;
    private String profileImageUrl;
    private boolean online;
    private long lastOnline;
    private String fcmToken;

    // Clave pública para E2EE (añade este campo)
    private String publicKey; // Base64, puedes guardar la clave pública aquí

    // Nuevos campos para compatibilidad Firestore
    private long lastSeen;
    private long tokenUpdatedAt;

    public User() {}

    public User(String uid, String email, String displayName) {
        this.uid = uid;
        this.email = email;
        this.displayName = displayName;
        this.photoUrl = "";
        this.profileImageUrl = "";
        this.online = false;
        this.lastOnline = System.currentTimeMillis();
        this.fcmToken = "";
        this.lastSeen = 0;
        this.tokenUpdatedAt = 0;
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

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }

    public boolean isOnline() { return online; }
    public void setOnline(boolean online) { this.online = online; }

    public long getLastOnline() { return lastOnline; }
    public void setLastOnline(long lastOnline) { this.lastOnline = lastOnline; }

    public String getFcmToken() { return fcmToken; }
    public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }

    public long getLastSeen() { return lastSeen; }
    public void setLastSeen(long lastSeen) { this.lastSeen = lastSeen; }

    public long getTokenUpdatedAt() { return tokenUpdatedAt; }
    public void setTokenUpdatedAt(long tokenUpdatedAt) { this.tokenUpdatedAt = tokenUpdatedAt; }

    public String getPublicKey() { return publicKey; }
    public void setPublicKey(String publicKey) { this.publicKey = publicKey; }
}