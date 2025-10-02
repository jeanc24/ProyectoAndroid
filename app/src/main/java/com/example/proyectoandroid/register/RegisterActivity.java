package com.example.proyectoandroid.register;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.proyectoandroid.R;
import com.example.proyectoandroid.login.LoginActivity;
import com.example.proyectoandroid.di.ServiceLocator;
import com.example.proyectoandroid.domain.usecase.LoginUserUseCase;
import com.example.proyectoandroid.utils.Result;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";
    private EditText etEmail, etPassword, etDisplayName;
    private Button btnRegister, btnGoLogin;
    private ProgressBar progressBar;
    private LoginUserUseCase loginUseCase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etEmail = findViewById(R.id.etRegisterEmail);
        etPassword = findViewById(R.id.etRegisterPassword);
        etDisplayName = findViewById(R.id.etRegisterDisplayName); // Agrega este campo al XML
        btnRegister = findViewById(R.id.btnRegister);
        btnGoLogin = findViewById(R.id.btnGoLogin);
        progressBar = findViewById(R.id.progressBar);

        loginUseCase = ServiceLocator.getInstance(getApplicationContext()).provideLoginUserUseCase();

        btnRegister.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String displayName = etDisplayName.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty() || displayName.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show();
                return;
            }

            if (password.length() < 6) {
                Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show();
                return;
            }

            checkEmailExistsAndRegister(email, password, displayName);
        });

        btnGoLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void checkEmailExistsAndRegister(String email, String password, String displayName) {
        setLoadingState(true);

        loginUseCase.execute(email, password)
                .thenAccept(result -> runOnUiThread(() -> {
                    setLoadingState(false);

                    if (result.isSuccess()) {
                        // El usuario ya existe con ese email
                        Toast.makeText(this, "Este correo ya está registrado. Usa otro o inicia sesión.", Toast.LENGTH_LONG).show();
                    } else {
                        // El correo no existe, proceder con el registro
                        performRegister(email, password, displayName);
                    }
                }))
                .exceptionally(throwable -> {
                    runOnUiThread(() -> {
                        setLoadingState(false);
                        // Si el error es "usuario no existe", se puede registrar; si es otro, mostrar mensaje
                        performRegister(email, password, displayName);
                    });
                    return null;
                });
    }

    private void performRegister(String email, String password, String displayName) {
        Log.d(TAG, "Iniciando registro para: " + email);
        setLoadingState(true);

        loginUseCase.registerUser(email, password, displayName)
                .thenAccept(result -> {
                    Log.d(TAG, "Registro completado, resultado: " + (result.isSuccess() ? "exitoso" : "fallido"));

                    runOnUiThread(() -> {
                        setLoadingState(false);

                        if (result.isSuccess()) {
                            Log.d(TAG, "Registro exitoso");
                            Toast.makeText(this, "¡Registro exitoso, Bienvenido!", Toast.LENGTH_SHORT).show();

                            // Pequeño delay para mejor UX
                            new Handler().postDelayed(() -> {
                                Intent intent = new Intent(this, LoginActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            }, 1500);

                        } else {
                            String error = ((Result.Error<?>) result).getErrorMessage();
                            Log.e(TAG, "Error en registro: " + error);
                            Toast.makeText(this, "Error: " + error, Toast.LENGTH_LONG).show();
                        }
                    });
                })
                .exceptionally(throwable -> {
                    Log.e(TAG, "Excepción en registro", throwable);
                    runOnUiThread(() -> {
                        setLoadingState(false);
                        Toast.makeText(this, "Error de conexión: " + throwable.getMessage(), Toast.LENGTH_LONG).show();
                    });
                    return null;
                });
    }

    private void setLoadingState(boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(ProgressBar.VISIBLE);
            btnRegister.setEnabled(false);
            btnGoLogin.setEnabled(false);
            btnRegister.setText("Registrando...");
        } else {
            progressBar.setVisibility(ProgressBar.GONE);
            btnRegister.setEnabled(true);
            btnGoLogin.setEnabled(true);
            btnRegister.setText("Registrarse");
        }
    }
}