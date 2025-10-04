package com.example.proyectoandroid;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.FirebaseMessaging;

public class FCMDebugActivity extends AppCompatActivity {

    private static final String TAG = "FCMDebugActivity";
    private TextView tvStatus;
    private TextView tvToken;
    private Button btnGetToken;
    private Button btnTestLocal;
    private Button btnCheckConnection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fcm_debug);

        tvStatus = findViewById(R.id.tv_status);
        tvToken = findViewById(R.id.tv_token);
        btnGetToken = findViewById(R.id.btn_get_token);
        btnTestLocal = findViewById(R.id.btn_test_local);
        btnCheckConnection = findViewById(R.id.btn_check_connection);
        Button btnBack = findViewById(R.id.btn_back);

        btnGetToken.setOnClickListener(v -> refreshToken());
        btnTestLocal.setOnClickListener(v -> testLocalNotification());
        btnCheckConnection.setOnClickListener(v -> checkConnectivity());
        btnBack.setOnClickListener(v -> {
            finish();
        });

        // Mostrar información inicial
        checkConnectivity();
    }

    private void refreshToken() {
        updateStatus("Obteniendo token FCM...");

        FirebaseMessaging.getInstance().getToken()
            .addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    Log.w(TAG, "Error obteniendo token FCM", task.getException());
                    updateStatus("Error: " + task.getException().getMessage());
                    return;
                }

                String token = task.getResult();
                String userId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                        FirebaseAuth.getInstance().getCurrentUser().getUid() : "No autenticado";

                updateToken("UID: " + userId + "\n\nToken: " + token);
                updateStatus("Token obtenido con éxito");

                // Registrar el token en Firestore para asegurar que está actualizado
                if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                    new com.example.proyectoandroid.notifications.NotificationManager(this)
                            .updateUserFcmToken(userId, token);
                    updateStatus("Token registrado en Firestore para usuario: " + userId);
                }
            });
    }

    private void testLocalNotification() {
        updateStatus("Probando notificación local...");

        try {
            // Datos para la notificación de prueba
            String title = "Mensaje de prueba local";
            String body = "Esta es una notificación de prueba local";
            String chatId = "test_chat_id";

            // Crear intent para abrir ChatActivity al pulsar la notificación
            Intent intent = new Intent(this, com.example.proyectoandroid.chat.ChatActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            if (chatId != null) {
                intent.putExtra("chatId", chatId);
            }

            int requestCode = chatId.hashCode();
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    this,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
            );

            // Configurar notificación
            String channelId = getString(R.string.default_notification_channel_id);
            Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

            NotificationCompat.Builder notificationBuilder =
                    new NotificationCompat.Builder(this, channelId)
                            .setSmallIcon(R.drawable.ic_notification)
                            .setContentTitle(title)
                            .setContentText(body)
                            .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                            .setAutoCancel(true)
                            .setSound(defaultSoundUri)
                            .setContentIntent(pendingIntent)
                            .setPriority(NotificationCompat.PRIORITY_HIGH)
                            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                            .setVibrate(new long[]{0, 250, 250, 250});

            // Mostrar notificación
            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            int notificationId = 1001;
            notificationManager.notify(notificationId, notificationBuilder.build());

            updateStatus("Notificación local mostrada con éxito");
            Toast.makeText(this, "Notificación enviada, revisa la barra de estado", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.e(TAG, "Error al mostrar notificación local", e);
            updateStatus("Error: " + e.getMessage());
        }
    }

    private void checkConnectivity() {
        updateStatus("Verificando conectividad...");

        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();

        String networkDetails = isConnected ?
                "Conectado: " + activeNetwork.getTypeName() +
                "\nRoaming: " + activeNetwork.isRoaming() :
                "No conectado";

        updateStatus("Estado de conectividad: " + (isConnected ? "ONLINE" : "OFFLINE") +
                "\n" + networkDetails);

        if (!isConnected) {
            updateStatus(getStatus() +
                    "\n\nError de conectividad detectado. Las notificaciones pueden no funcionar " +
                    "correctamente hasta que se reestablezca la conexión.");
        }
    }

    private void updateStatus(String status) {
        runOnUiThread(() -> tvStatus.setText(status));
    }

    private void updateToken(String tokenInfo) {
        runOnUiThread(() -> tvToken.setText(tokenInfo));
    }

    private String getStatus() {
        return tvStatus.getText().toString();
    }
}
