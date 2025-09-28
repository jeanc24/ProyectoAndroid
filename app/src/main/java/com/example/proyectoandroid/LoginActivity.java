package com.example.proyectoandroid;

import android.content.Intent; // <-- Import necesario
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin, btnGoRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login); // Vincula el XML

        // Vinculación con los IDs del layout
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnGoRegister = findViewById(R.id.btnGoRegister);

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString();
            String password = etPassword.getText().toString();
            // Aquí va la lógica de login (puedes conectar Firebase después)
        });

        btnGoRegister.setOnClickListener(v -> {
            // Lógica para ir a la pantalla de registro
            Intent intent = new Intent(LoginActivity.this, RegistrarActivity.class);
            startActivity(intent);
        });
    }
}