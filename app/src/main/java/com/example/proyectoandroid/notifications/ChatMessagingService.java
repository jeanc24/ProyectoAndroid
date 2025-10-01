package com.example.proyectoandroid.notifications;

import android.app.NotificationChannel;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.example.proyectoandroid.R;
import com.example.proyectoandroid.chat.ChatActivity;
import com.example.proyectoandroid.data.model.User;
import com.example.proyectoandroid.data.repository.AuthRepository;
import com.example.proyectoandroid.di.ServiceLocator;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class ChatMessagingService extends FirebaseMessagingService {

    private static final String TAG = "ChatMessagingService";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        Map<String, String> data = remoteMessage.getData();

        if (data.size() > 0) {
            String title = data.get("title");
            String body = data.get("body");
            String chatId = data.get("chatId");
            String senderId = data.get("senderId");

            AuthRepository authRepository = ServiceLocator.getInstance(this).provideAuthRepository();
            User currentUser = authRepository.getCurrentUser();

            if (currentUser != null && !currentUser.getUid().equals(senderId)) {
                sendNotification(title != null ? title : getString(R.string.new_message_notification_title),
                        body != null ? body : getString(R.string.new_message_notification_text),
                        chatId);
            }
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        AuthRepository authRepository = ServiceLocator.getInstance(this).provideAuthRepository();
        User currentUser = authRepository.getCurrentUser();

        if (currentUser != null) {
            com.example.proyectoandroid.notifications.NotificationManager notificationManager =
                    new com.example.proyectoandroid.notifications.NotificationManager(this);
            notificationManager.updateUserFcmToken(currentUser.getUid(), token);
        }
    }

    private void sendNotification(String title, String messageBody, String chatId) {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("chatId", chatId);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        String channelId = getString(R.string.default_notification_channel_id);
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, channelId)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle(title)
                        .setContentText(messageBody)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent)
                        .setColor(getResources().getColor(R.color.notification_color, getTheme()));

        android.app.NotificationManager notificationManager =
                (android.app.NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    getString(R.string.notification_channel_name),
                    android.app.NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription(getString(R.string.notification_channel_description));
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify(0, notificationBuilder.build());
    }
}
