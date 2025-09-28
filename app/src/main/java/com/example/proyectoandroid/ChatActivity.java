package com.example.proyectoandroid;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import java.util.Arrays;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Título del chat (recibido por Intent)
        String chatTitle = getIntent().getStringExtra("chatTitle");
        TextView tvChatTitle = findViewById(R.id.tvChatTitle);
        tvChatTitle.setText(chatTitle != null ? chatTitle : "Chat");

        // Mensajes mock
        List<Message> mockMessages = Arrays.asList(
                new Message("Juan", "¡Hola! ¿Cómo estás?", true,1),
                new Message("Yo", "Bien, ¿y tú?", false,2),
                new Message("Juan", "Todo bien, gracias.", true,3)
        );

        RecyclerView rvMessages = findViewById(R.id.rvMessages);
        rvMessages.setLayoutManager(new LinearLayoutManager(this));
        rvMessages.setAdapter(new MessageAdapter(mockMessages));
    }
}