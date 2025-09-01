package com.example.easychat;

public class ChatActivityState {
    private static String activeChatroomId = null;

    public static String getActiveChatroomId() {
        return activeChatroomId;
    }

    public static void setActiveChatroomId(String chatroomId) {
        activeChatroomId = chatroomId;
    }

    public static void clearActiveChatroomId() {
        activeChatroomId = null;
    }
}