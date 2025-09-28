package com.example.proyectoandroid;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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

        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        testAuthButton.setOnClickListener(v -> testAuthentication());
        testMessagingButton.setOnClickListener(v -> testMessaging());
        testRealtimeButton.setOnClickListener(v -> testRealtimeListeners());
    }

    private void testAuthentication() {
        clearResults();
        appendResult("Starting Authentication Tests...");
        Toast.makeText(this, "Ejecutando pruebas de autenticación...", Toast.LENGTH_SHORT).show();

        executorService.execute(() -> {
            try {
                BackendTester tester = BackendTester.getInstance(
                        serviceLocator.provideAuthRepository(),
                        null,
                        null);
                boolean success = tester.testAuthentication();

                mainHandler.post(() -> {
                    if (success) {
                        appendResult("Authentication tests PASSED!");
                    } else {
                        appendResult("Authentication tests FAILED!");
                    }
                    appendResult("Check Logcat (tag: BackendTester) for detailed results.");
                });

                tester.cleanUp();

            } catch (Exception e) {
                Log.e(TAG, "Error running authentication tests", e);
                mainHandler.post(() ->
                    appendResult("Error running tests: " + e.getMessage())
                );
            }
        });
    }

    private void testMessaging() {
        clearResults();
        appendResult("Starting Messaging Tests...");
        Toast.makeText(this, "Ejecutando pruebas de mensajería...", Toast.LENGTH_SHORT).show();

        executorService.execute(() -> {
            try {
                BackendTester tester = BackendTester.getInstance(
                        serviceLocator.provideAuthRepository(),
                        serviceLocator.provideChatRepository(),
                        serviceLocator.provideMessageRepository()
                );

                appendResultThreadSafe("Testing chat creation...");
                boolean success = tester.testMessaging();

                mainHandler.post(() -> {
                    if (success) {
                        appendResult("Messaging tests PASSED!");
                        appendResult("Chat creation");
                        appendResult("Send/receive messages");
                        appendResult("Message listeners");
                        appendResult("Message pagination");
                    } else {
                        appendResult("Messaging tests FAILED!");
                    }
                    appendResult("Check Logcat (tag: BackendTester) for detailed results.");
                });

                tester.cleanUp();

            } catch (Exception e) {
                Log.e(TAG, "Error running messaging tests", e);
                mainHandler.post(() ->
                    appendResult("Error running tests: " + e.getMessage())
                );
            }
        });
    }

    private void testRealtimeListeners() {
        clearResults();
        appendResult("Starting Real-time Features Tests...");
        Toast.makeText(this, "Ejecutando pruebas de características en tiempo real...", Toast.LENGTH_SHORT).show();

        executorService.execute(() -> {
            try {
                BackendTester tester = BackendTester.getInstance(
                        serviceLocator.provideAuthRepository(),
                        serviceLocator.provideChatRepository(),
                        serviceLocator.provideMessageRepository()
                );

                appendResultThreadSafe("Testing online/offline status...");
                boolean success = tester.testRealtimeFeatures();

                mainHandler.post(() -> {
                    if (success) {
                        appendResult("Real-time features tests PASSED!");
                        appendResult("Online/offline status");
                        appendResult("Chat listeners");
                    } else {
                        appendResult("Real-time features tests FAILED!");
                    }
                    appendResult("Check Logcat (tag: BackendTester) for detailed results.");
                });

                tester.cleanUp();

            } catch (Exception e) {
                Log.e(TAG, "Error running real-time features tests", e);
                mainHandler.post(() ->
                    appendResult("Error running tests: " + e.getMessage())
                );
            }
        });
    }

    private void appendResultThreadSafe(String text) {
        mainHandler.post(() -> appendResult(text));
    }

    private void appendResult(String text) {
        if (resultTextView != null) {
            resultTextView.append(text + "\n\n");
            scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
        }
    }

    private void clearResults() {
        if (resultTextView != null) {
            resultTextView.setText("");
        }
    }

    @Override
    protected void onDestroy() {
        executorService.shutdownNow();
        super.onDestroy();
    }
}
