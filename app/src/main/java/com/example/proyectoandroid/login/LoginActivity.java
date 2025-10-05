package com.example.proyectoandroid.login;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.proyectoandroid.MainActivity;
import com.example.proyectoandroid.R;
import com.example.proyectoandroid.di.ServiceLocator;
import com.example.proyectoandroid.domain.usecase.LoginUserUseCase;
import com.example.proyectoandroid.utils.Result;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private EditText etEmail, etPassword;
    private Button btnLogin, btnGoRegister;
    private ProgressBar progressBar;
    private LoginUserUseCase loginUseCase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnGoRegister = findViewById(R.id.btnGoRegister);
        progressBar = findViewById(R.id.progressBar);

        loginUseCase = ServiceLocator.getInstance(getApplicationContext()).provideLoginUserUseCase();

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show();
                return;
            }

            performLogin(email, password);
        });

        btnGoRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, com.example.proyectoandroid.register.RegisterActivity.class));
        });
    }

    private void performLogin(String email, String password) {
        Log.d(TAG, "Iniciando login para: " + email);
        setLoadingState(true);

        loginUseCase.execute(email, password)
                .thenAccept(result -> {
                    Log.d(TAG, "Login completado, resultado: " + (result.isSuccess() ? "exitoso" : "fallido"));

                    runOnUiThread(() -> {
                        setLoadingState(false);

                        if (result.isSuccess()) {
                            Log.d(TAG, "Login exitoso, navegando a MainActivity");
                            Toast.makeText(this, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show();

                            // Pequeño delay para asegurar que la sesión se guarde
                            Toast.makeText(this, "Login exitoso", Toast.LENGTH_SHORT).show();
                            new android.os.Handler().postDelayed(() -> {
                                startActivity(new Intent(this, MainActivity.class));
                                finish();
                            }, 1500);

                        } else {
                            String error = ((Result.Error<?>) result).getErrorMessage();
                            Log.e(TAG, "Error en login: " + error);
                            Toast.makeText(this, "Error: " + error, Toast.LENGTH_LONG).show();
                        }
                    });
                })
                .exceptionally(throwable -> {
                    Log.e(TAG, "Excepción en login", throwable);
                    runOnUiThread(() -> {
                        setLoadingState(false);
                        Toast.makeText(this, "Error de conexión: " + throwable.getMessage(), Toast.LENGTH_LONG).show();
                    });
                    return null;
                });
    }

    private void navigateToMainActivity() {
        Log.d(TAG, "Navegando a MainActivity");
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setLoadingState(boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(ProgressBar.VISIBLE);
            btnLogin.setEnabled(false);
            btnGoRegister.setEnabled(false);
            btnLogin.setText("Iniciando sesión...");
        } else {
            progressBar.setVisibility(ProgressBar.GONE);
            btnLogin.setEnabled(true);
            btnGoRegister.setEnabled(true);
            btnLogin.setText("Ingresar");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume llamado");

        // Verificar si ya está logueado
        if (ServiceLocator.getInstance(getApplicationContext())
                .provideGetCurrentUserUseCase()
                .isUserLoggedIn()) {
            Log.d(TAG, "Usuario ya logueado, navegando a MainActivity");
            navigateToMainActivity();
        }
    }

}