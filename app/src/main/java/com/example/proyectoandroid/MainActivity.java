package com.example.proyectoandroid;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.proyectoandroid.databinding.ActivityMainBinding;

import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);

        // Datos mock para los chats
        List<Chat> mockChats = Arrays.asList(
                new Chat("Juan", "Hola, ¿cómo estás?", R.mipmap.ic_launcher),
                new Chat("Grupo Familia", "Cenamos a las 8", R.mipmap.ic_launcher),
                new Chat("Oficina", "Reunión a las 10", R.mipmap.ic_launcher)
        );

        // Vinculación del RecyclerView
        RecyclerView chatsRecyclerView = findViewById(R.id.chatsRecyclerView);
        ChatAdapter adapter = new ChatAdapter(mockChats, chat -> {
            // Navegación al chat individual (puedes crear ChatActivity más adelante)
            Intent intent = new Intent(MainActivity.this, ChatActivity.class);
            intent.putExtra("chatTitle", chat.title);
            startActivity(intent);
        });

        chatsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatsRecyclerView.setAdapter(adapter);

        // Puedes configurar el FAB para crear nuevos chats en el futuro
        binding.fab.setOnClickListener(view -> {
            // Acción para crear nuevo chat (a futuro, ahora puedes dejarlo vacío)
        });
    }
}