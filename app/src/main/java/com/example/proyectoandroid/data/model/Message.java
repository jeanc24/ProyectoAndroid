package com.example.proyectoandroid.data.model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class Message {
    @DocumentId
    private String messageId;
    private String chatId;
    private String senderId;
    private String senderName;
    private String senderEmail;
    private String content; // Cifrado con clave única por chat
    private int messageType; // 0 = texto, 1 = imagen
    private String imageUrl;
    @ServerTimestamp
    private Date timestamp;
    private boolean isRead;

    public Message() {}

    public Message(String chatId, String senderId, String senderName, String senderEmail, String content, int messageType, String imageUrl) {
        this.chatId = chatId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.senderEmail = senderEmail;
        this.content = content;
        this.messageType = messageType;
        this.imageUrl = imageUrl;
        this.isRead = false;
    }

    // Getters y setters
    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public String getChatId() { return chatId; }
    public void setChatId(String chatId) { this.chatId = chatId; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getSenderEmail() { return senderEmail; }
    public void setSenderEmail(String senderEmail) { this.senderEmail = senderEmail; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public int getMessageType() { return messageType; }
    public void setMessageType(int messageType) { this.messageType = messageType; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }
}