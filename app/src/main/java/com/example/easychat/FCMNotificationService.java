package com.example.easychat;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import com.example.easychat.model.UserModel;
import com.example.easychat.utils.AndroidUtil;
import com.example.easychat.utils.FirebaseUtil;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class FCMNotificationService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        if (remoteMessage.getNotification() == null) return;

        String title = remoteMessage.getNotification().getTitle();
        String body = remoteMessage.getNotification().getBody();
        String userId = remoteMessage.getData().get("userId");
        String chatroomId = remoteMessage.getData().get("chatroomId");

        if (userId == null || chatroomId == null) return;

        if (chatroomId.equals(ChatActivityState.getActiveChatroomId())) {
            return;
        }

        // Usa um ID numérico consistente baseado no chatroomId
        int notificationId = chatroomId.hashCode();

        FirebaseUtil.allUserCollectionReference().document(userId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        UserModel sender = task.getResult().toObject(UserModel.class);
                        if (sender != null) {
                            Intent intent = new Intent(this, ChatActivity.class);
                            AndroidUtil.passUserModelAsIntent(intent, sender);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                            PendingIntent pendingIntent = PendingIntent.getActivity(this, notificationId, intent,
                                    PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

                            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, "chat_messages")
                                    .setContentTitle(title)
                                    .setContentText(body)
                                    .setSmallIcon(R.drawable.chat_icon)
                                    .setAutoCancel(true)
                                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                                    .setContentIntent(pendingIntent);

                            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                NotificationChannel channel = new NotificationChannel("chat_messages", "Chat Messages",
                                        NotificationManager.IMPORTANCE_DEFAULT);
                                manager.createNotificationChannel(channel);
                            }

                            manager.notify(notificationId, notificationBuilder.build());
                        }
                    }
                });
    }
}