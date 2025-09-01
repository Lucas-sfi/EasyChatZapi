package com.example.easychat.model;

import com.google.firebase.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class UserModel {
    private String phone;
    private String email;
    private String username;
    private String searchUsername;
    private Timestamp createdTimestamp;
    private String userId;
    private String fcmToken;
    private String userStatus;
    private int age;
    private String city;
    private List<String> contacts; // NOVO CAMPO

    public UserModel() {
        this.contacts = new ArrayList<>(); // Inicializa a lista
    }

    // Construtor para login com telefone
    public UserModel(String phone, String username, Timestamp createdTimestamp,String userId) {
        this.phone = phone;
        this.username = username;
        this.searchUsername = username.toLowerCase();
        this.createdTimestamp = createdTimestamp;
        this.userId = userId;
        this.userStatus = "offline";
        this.age = 0;
        this.city = "";
        this.email = null;
        this.contacts = new ArrayList<>(); // Inicializa a lista
    }

    // Novo construtor para login com e-mail
    public UserModel(String email, String username, Timestamp createdTimestamp, String userId, boolean isEmail) {
        this.email = email;
        this.username = username;
        this.searchUsername = username.toLowerCase();
        this.createdTimestamp = createdTimestamp;
        this.userId = userId;
        this.userStatus = "offline";
        this.age = 0;
        this.city = "";
        this.phone = null;
        this.contacts = new ArrayList<>(); // Inicializa a lista
    }

    // GETTER E SETTER PARA O NOVO CAMPO
    public List<String> getContacts() {
        return contacts;
    }

    public void setContacts(List<String> contacts) {
        this.contacts = contacts;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    // Getters e Setters existentes
    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getSearchUsername() { return searchUsername; }
    public void setSearchUsername(String searchUsername) { this.searchUsername = searchUsername; }
    public Timestamp getCreatedTimestamp() { return createdTimestamp; }
    public void setCreatedTimestamp(Timestamp createdTimestamp) { this.createdTimestamp = createdTimestamp; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getFcmToken() { return fcmToken; }
    public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }
    public String getUserStatus() { return userStatus; }
    public void setUserStatus(String userStatus) { this.userStatus = userStatus; }
}