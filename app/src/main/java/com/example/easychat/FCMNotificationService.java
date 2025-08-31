package com.example.easychat;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

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

        // Extrair dados da notificação
        String title = remoteMessage.getNotification().getTitle();
        String body = remoteMessage.getNotification().getBody();
        String userId = remoteMessage.getData().get("userId");

        if (userId == null) return;

        // Buscar os detalhes do usuário que enviou a mensagem
        FirebaseUtil.allUserCollectionReference().document(userId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        UserModel sender = task.getResult().toObject(UserModel.class);
                        if (sender != null) {
                            // Criar a Intent que será aberta ao clicar na notificação
                            Intent intent = new Intent(this, ChatActivity.class);
                            AndroidUtil.passUserModelAsIntent(intent, sender);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                                    PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

                            // Construir a notificação
                            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, "chat_messages")
                                    .setContentTitle(title)
                                    .setContentText(body)
                                    .setSmallIcon(R.drawable.ic_launcher_foreground) // Use um ícone apropriado
                                    .setAutoCancel(true) // A notificação desaparecerá ao ser clicada
                                    .setContentIntent(pendingIntent);

                            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

                            // Criar o canal de notificação para Android 8.0+
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                NotificationChannel channel = new NotificationChannel("chat_messages", "Chat Messages",
                                        NotificationManager.IMPORTANCE_DEFAULT);
                                manager.createNotificationChannel(channel);
                            }

                            // Exibir a notificação
                            manager.notify(0, notificationBuilder.build());
                        }
                    }
                });
    }
}