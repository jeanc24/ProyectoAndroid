package com.example.proyectoandroid.data.model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Chat {
    @DocumentId
    private String chatId;
    private List<String> participantIds;
    private String lastMessageContent;
    private String lastMessageSenderId;
    private String lastMessageSenderName;
    private String lastMessageSenderEmail;
    @ServerTimestamp
    private Date lastMessageTimestamp;
    private boolean isGroupChat;
    private String chatName;
    private String chatImageUrl;
    private boolean lastMessageRead;
    private int lastMessageType; // <-- NUEVO CAMPO

    public Chat() {
        participantIds = new ArrayList<>();
        lastMessageRead = true;
        lastMessageType = 0;
    }

    public Chat(String user1Id, String user2Id) {
        this.participantIds = new ArrayList<>();
        this.participantIds.add(user1Id);
        this.participantIds.add(user2Id);
        this.isGroupChat = false;
        this.lastMessageContent = "";
        this.lastMessageSenderName = "";
        this.lastMessageSenderEmail = "";
        this.lastMessageRead = true;
        this.lastMessageType = 0;
    }

    public Chat(List<String> participantIds, String chatName) {
        this.participantIds = participantIds;
        this.chatName = chatName;
        this.isGroupChat = true;
        this.lastMessageContent = "";
        this.chatImageUrl = "";
        this.lastMessageSenderName = "";
        this.lastMessageSenderEmail = "";
        this.lastMessageRead = true;
        this.lastMessageType = 0;
    }

    // Getters y setters
    public String getChatId() { return chatId; }
    public void setChatId(String chatId) { this.chatId = chatId; }

    public List<String> getParticipantIds() { return participantIds; }
    public void setParticipantIds(List<String> participantIds) { this.participantIds = participantIds; }

    public String getLastMessageContent() { return lastMessageContent; }
    public void setLastMessageContent(String lastMessageContent) { this.lastMessageContent = lastMessageContent; }

    public String getLastMessageSenderId() { return lastMessageSenderId; }
    public void setLastMessageSenderId(String lastMessageSenderId) { this.lastMessageSenderId = lastMessageSenderId; }

    public Date getLastMessageTimestamp() { return lastMessageTimestamp; }
    public void setLastMessageTimestamp(Date lastMessageTimestamp) { this.lastMessageTimestamp = lastMessageTimestamp; }

    public boolean isGroupChat() { return isGroupChat; }
    public void setGroupChat(boolean groupChat) { isGroupChat = groupChat; }

    public String getChatName() { return chatName; }
    public void setChatName(String chatName) { this.chatName = chatName; }

    public String getChatImageUrl() { return chatImageUrl; }
    public void setChatImageUrl(String chatImageUrl) { this.chatImageUrl = chatImageUrl; }

    public String getLastMessageSenderName() { return lastMessageSenderName; }
    public void setLastMessageSenderName(String lastMessageSenderName) { this.lastMessageSenderName = lastMessageSenderName; }

    public String getLastMessageSenderEmail() { return lastMessageSenderEmail; }
    public void setLastMessageSenderEmail(String lastMessageSenderEmail) { this.lastMessageSenderEmail = lastMessageSenderEmail; }

    public boolean isLastMessageRead() { return lastMessageRead; }
    public void setLastMessageRead(boolean lastMessageRead) { this.lastMessageRead = lastMessageRead; }

    public int getLastMessageType() { return lastMessageType; }
    public void setLastMessageType(int lastMessageType) { this.lastMessageType = lastMessageType; }
}