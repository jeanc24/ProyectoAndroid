package com.example.proyectoandroid;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.proyectoandroid.di.ServiceLocator;
import com.google.firebase.FirebaseApp;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BackendTestActivity extends AppCompatActivity {

    private static final String TAG = "BackendTestActivity";
    private TextView resultTextView;
    private ScrollView scrollView;
    private ExecutorService executorService;
    private Handler mainHandler;
    private ServiceLocator serviceLocator;

    private static final int NOTIFICATION_PERMISSION_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backend_test);

        FirebaseApp.initializeApp(this);

        serviceLocator = ServiceLocator.getInstance(getApplicationContext());

        resultTextView = findViewById(R.id.result_text_view);
        scrollView = findViewById(R.id.scroll_view);
        Button testAuthButton = findViewById(R.id.test_auth_button);
        Button testMessagingButton = findViewById(R.id.test_messaging_button);
        Button testRealtimeButton = findViewById(R.id.test_realtime_button);
        Button testSprint3Button = findViewById(R.id.test_sprint3_button);
        Button testNotificationsButton = findViewById(R.id.test_notifications_button);
        Button checkNotificationsPermissionButton = findViewById(R.id.check_notifications_permission);

        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        testAuthButton.setOnClickListener(v -> testAuthentication());
        testMessagingButton.setOnClickListener(v -> testMessaging());
        testRealtimeButton.setOnClickListener(v -> testRealtimeListeners());
        testSprint3Button.setOnClickListener(v -> testSprint3Features());
        testNotificationsButton.setOnClickListener(v -> {
            if (checkAndRequestNotificationPermission()) {
                testNotificationsOnly();
            }
        });

        if (checkNotificationsPermissionButton != null) {
            checkNotificationsPermissionButton.setOnClickListener(v -> checkNotificationStatus());
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkAndRequestNotificationPermission();
        }
    }

    private boolean checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {

                appendResult("\n⚠️ Se requiere permiso para mostrar notificaciones");

                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.POST_NOTIFICATIONS)) {

                    new AlertDialog.Builder(this)
                        .setTitle("Permiso de notificaciones")
                        .setMessage("Esta app necesita permiso para mostrar notificaciones para " +
                                "que puedas ver los mensajes nuevos.")
                        .setPositiveButton("Conceder", (dialog, which) -> {
                            ActivityCompat.requestPermissions(this,
                                new String[]{Manifest.permission.POST_NOTIFICATIONS},
                                NOTIFICATION_PERMISSION_CODE);
                        })
                        .setNegativeButton("Cancelar", (dialog, which) -> {
                            appendResult("\nPermiso de notificaciones denegado por usuario");
                            dialog.dismiss();
                        })
                        .create()
                        .show();
                } else {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.POST_NOTIFICATIONS},
                            NOTIFICATION_PERMISSION_CODE);
                }
                return false;
            }
        }
        return true;
    }

    private void checkNotificationStatus() {
        boolean hasPermission = false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasPermission = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;

            appendResult("\n--- Estado de notificaciones ---");
            if (hasPermission) {
                appendResult("✓ Permiso de notificaciones concedido");
            } else {
                appendResult("Permiso de notificaciones NO concedido");
                appendResult("Por favor concede el permiso cuando se solicite");

                checkAndRequestNotificationPermission();
            }
        } else {
            hasPermission = true;
            appendResult("\n--- Estado de notificaciones ---");
            appendResult("✓ No se requiere permiso en esta versión de Android");
        }

        boolean notificationsEnabled = areNotificationsEnabled();
        if (notificationsEnabled) {
            appendResult("✓ Notificaciones habilitadas en configuración del sistema");
        } else {
            appendResult("Notificaciones deshabilitadas en configuración del sistema");
            appendResult("Abriendo configuración de la app...");

            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
            startActivity(intent);
        }
    }

    private boolean areNotificationsEnabled() {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        return notificationManager.areNotificationsEnabled();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                          @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                appendResult("\n✓ Permiso de notificaciones concedido");
                testNotificationsOnly();
            } else {
                appendResult("\nPermiso de notificaciones denegado");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.POST_NOTIFICATIONS)) {
                        appendResult("Debes habilitar el permiso manualmente en configuración");

                        new AlertDialog.Builder(this)
                            .setTitle("Permiso requerido")
                            .setMessage("Las notificaciones son necesarias para que la app " +
                                    "funcione correctamente. Por favor, habilítalas en la configuración.")
                            .setPositiveButton("Ir a Configuración", (dialog, which) -> {
                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package", getPackageName(), null);
                                intent.setData(uri);
                                startActivity(intent);
                            })
                            .setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss())
                            .create()
                            .show();
                    }
                }
            }
        }
    }

    private void testAuthentication() {
        appendResult("\nEjecutando pruebas de autenticación...");

        executorService.execute(() -> {
            BackendTester tester = BackendTester.getInstance(
                    serviceLocator.provideAuthRepository(),
                    serviceLocator.provideChatRepository(),
                    serviceLocator.provideMessageRepository());

            try {
                boolean success = tester.testAuthentication();
                String result = success ?
                        "Pruebas de autenticación EXITOSAS" :
                        "Pruebas de autenticación FALLIDAS";
                appendResult(result);
            } catch (Exception e) {
                appendResult("Error en pruebas de autenticación: " + e.getMessage());
                Log.e(TAG, "Error durante pruebas de autenticación", e);
            }
        });
    }

    private void testMessaging() {
        appendResult("\nEjecutando pruebas de mensajería...");

        executorService.execute(() -> {
            BackendTester tester = BackendTester.getInstance(
                    serviceLocator.provideAuthRepository(),
                    serviceLocator.provideChatRepository(),
                    serviceLocator.provideMessageRepository());

            try {
                boolean success = tester.testMessaging();
                String result = success ?
                        "Pruebas de mensajería EXITOSAS" :
                        "Pruebas de mensajería FALLIDAS";
                appendResult(result);
            } catch (Exception e) {
                appendResult("Error en pruebas de mensajería: " + e.getMessage());
                Log.e(TAG, "Error durante pruebas de mensajería", e);
            }
        });
    }

    private void testRealtimeListeners() {
        appendResult("\nEjecutando pruebas de funcionalidades en tiempo real...");

        executorService.execute(() -> {
            BackendTester tester = BackendTester.getInstance(
                    serviceLocator.provideAuthRepository(),
                    serviceLocator.provideChatRepository(),
                    serviceLocator.provideMessageRepository());

            try {
                boolean success = tester.testRealtimeFeatures();
                String result = success ?
                        "Pruebas de funcionalidades en tiempo real EXITOSAS" :
                        "Pruebas de funcionalidades en tiempo real FALLIDAS";
                appendResult(result);
            } catch (Exception e) {
                appendResult("Error en pruebas de funcionalidades en tiempo real: " + e.getMessage());
                Log.e(TAG, "Error durante pruebas de funcionalidades en tiempo real", e);
            }
        });
    }

    private void testSprint3Features() {
        appendResult("\nEjecutando pruebas del Sprint 3 (Notificaciones e Imágenes)...");

        executorService.execute(() -> {
            BackendTester tester = BackendTester.getInstance(
                    serviceLocator.provideAuthRepository(),
                    serviceLocator.provideChatRepository(),
                    serviceLocator.provideMessageRepository());

            try {
                boolean success = tester.testSprint3Features(getApplicationContext());
                String result =
                        success ? "Pruebas del Sprint 3 EXITOSAS" : "Pruebas del Sprint 3 FALLIDAS";
                appendResult(result);

                if (success) {
                    appendResult("✓ Registro de token FCM exitoso");
                    appendResult("✓ Subida de imágenes exitosa");
                    appendResult("✓ Caché de imágenes funcionando");
                }
            } catch (Exception e) {
                appendResult("Error en pruebas del Sprint 3: " + e.getMessage());
                Log.e(TAG, "Error durante pruebas del Sprint 3", e);
            }
        });
    }

    private void testNotificationsOnly() {
        appendResult("\nEjecutando pruebas de Notificaciones...");

        executorService.execute(() -> {
            com.example.proyectoandroid.notifications.NotificationTester notificationTester =
                    new com.example.proyectoandroid.notifications.NotificationTester(this);

            try {
                appendResult("1. Verificando registro de token FCM...");
                boolean tokenRegistered = notificationTester.verifyFcmTokenRegistration().get();
                appendResult(tokenRegistered
                        ? "Token FCM registrado correctamente"
                        : "Error al registrar token FCM");

                appendResult("2. Enviando notificación de prueba...");
                boolean notificationSent = notificationTester.testNotification().get();
                appendResult(notificationSent
                        ? "Notificación de prueba enviada correctamente"
                        : "Error al enviar notificación de prueba");

                boolean success = tokenRegistered && notificationSent;
                String result = success ?
                        "Pruebas de Notificaciones EXITOSAS" :
                        "Pruebas de Notificaciones FALLIDAS";
                appendResult(result);

                if (success) {
                    runOnUiThread(() -> {
                        Toast.makeText(BackendTestActivity.this,
                                "Deberías ver una notificación ahora",
                                Toast.LENGTH_LONG).show();
                    });
                }
            } catch (Exception e) {
                appendResult("Error en pruebas de Notificaciones: " + e.getMessage());
                Log.e(TAG, "Error durante pruebas de Notificaciones", e);
            }
        });
    }

    private void appendResult(String text) {
        mainHandler.post(() -> {
            resultTextView.append("\n" + text);
            scrollView.fullScroll(ScrollView.FOCUS_DOWN);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdownNow();
    }
}
