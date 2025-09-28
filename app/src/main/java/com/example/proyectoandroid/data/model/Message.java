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
    private String content;
    private String imageUrl;
    @ServerTimestamp
    private Date timestamp;
    private boolean read;
    private int messageType;

    public Message() {}

    public Message(String chatId, String senderId, String senderName, String content) {
        this.chatId = chatId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.content = content;
        this.read = false;
        this.messageType = 0;
    }

    public Message(String chatId, String senderId, String senderName, String imageUrl, boolean isImage) {
        this.chatId = chatId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.imageUrl = imageUrl;
        this.content = "";
        this.read = false;
        this.messageType = 1;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public int getMessageType() {
        return messageType;
    }

    public void setMessageType(int messageType) {
        this.messageType = messageType;
    }
}
