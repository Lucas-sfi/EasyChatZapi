package com.example.easychat.model;

import com.google.firebase.Timestamp;
import java.util.List;

public class ChatMessageModel {
    private String message;
    private String senderId;
    private Timestamp timestamp;
    private int status;
    private List<String> searchKeywords;

    // VARIÁVEIS DE STATUS ADICIONADAS AQUI
    public static final int STATUS_SENT = 0;
    public static final int STATUS_READ = 1;

    public ChatMessageModel() {
    }

    public ChatMessageModel(String message, String senderId, Timestamp timestamp, int status, List<String> searchKeywords) {
        this.message = message;
        this.senderId = senderId;
        this.timestamp = timestamp;
        this.status = status;
        this.searchKeywords = searchKeywords;
    }

    public List<String> getSearchKeywords() {
        return searchKeywords;
    }

    public void setSearchKeywords(List<String> searchKeywords) {
        this.searchKeywords = searchKeywords;
    }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }
    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }
}