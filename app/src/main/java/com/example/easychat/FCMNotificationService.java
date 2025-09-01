package com.example.easychat;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class FCMNotificationService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        if (remoteMessage.getNotification() == null || remoteMessage.getData().isEmpty()) {
            return;
        }

        String chatroomId = remoteMessage.getData().get("chatroomId");
        if (chatroomId == null) {
            return;
        }

        if (chatroomId.equals(ChatActivityState.getActiveChatroomId())) {
            return;
        }

        String title = remoteMessage.getNotification().getTitle();
        String body = remoteMessage.getNotification().getBody();
        String isGroupChatStr = remoteMessage.getData().get("isGroupChat");
        boolean isGroupChat = "true".equals(isGroupChatStr);

        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("chatroomId", chatroomId);

        if (isGroupChat) {
            intent.putExtra("isGroupChat", true);
            intent.putExtra("groupName", remoteMessage.getData().get("groupName"));
        } else {
            intent.putExtra("userId", remoteMessage.getData().get("userId"));
            intent.putExtra("username", title);
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        int notificationId = chatroomId.hashCode();
        PendingIntent pendingIntent = PendingIntent.getActivity(this, notificationId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, "chat_messages")
                .setContentTitle(title)
                .setContentText(body)
                .setSmallIcon(R.drawable.chat_icon)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("chat_messages", "Chat Messages", NotificationManager.IMPORTANCE_HIGH);
            manager.createNotificationChannel(channel);
        }

        manager.notify(notificationId, notificationBuilder.build());
    }
}