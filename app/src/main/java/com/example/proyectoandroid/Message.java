package com.example.proyectoandroid;

public class Message {
    public String sender;
    public String text;
    public boolean isIncoming;
    public long timestamp; // NUEVO
    public MessageStatus status; // NUEVO
    public String imageUrl; // NUEVO para imágenes

    public enum MessageStatus {
        SENDING, SENT, DELIVERED, READ
    }

    public Message(String sender, String text, boolean isIncoming, long timestamp) {
        this.sender = sender;
        this.text = text;
        this.isIncoming = isIncoming;
        this.timestamp = timestamp;
        this.status = isIncoming ? MessageStatus.READ : MessageStatus.SENDING;
    }
}