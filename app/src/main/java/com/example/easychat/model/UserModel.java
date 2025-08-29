package com.example.easychat.model;

import com.google.firebase.Timestamp;

public class UserModel {
    private String phone;
    private String username;
    private Timestamp createdTimestamp;
    private String userId;
    private String fcmToken;
    private String userStatus; // Campo atualizado de boolean para String

    public UserModel() {
    }

    public UserModel(String phone, String username, Timestamp createdTimestamp,String userId) {
        this.phone = phone;
        this.username = username;
        this.createdTimestamp = createdTimestamp;
        this.userId = userId;
        this.userStatus = "offline"; // Valor padrão
    }

    // Getter e Setter para o campo atualizado
    public String getUserStatus() {
        return userStatus;
    }

    public void setUserStatus(String userStatus) {
        this.userStatus = userStatus;
    }

    // Getters e Setters existentes
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public Timestamp getCreatedTimestamp() { return createdTimestamp; }
    public void setCreatedTimestamp(Timestamp createdTimestamp) { this.createdTimestamp = createdTimestamp; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getFcmToken() { return fcmToken; }
    public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }
}