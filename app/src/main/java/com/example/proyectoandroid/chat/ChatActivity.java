package com.example.proyectoandroid.chat;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.proyectoandroid.R;
import com.example.proyectoandroid.data.model.Message;
import com.example.proyectoandroid.data.model.User;
import com.example.proyectoandroid.di.ServiceLocator;
import com.example.proyectoandroid.domain.usecase.GetCurrentUserUseCase;
import com.example.proyectoandroid.domain.usecase.ListenMessagesUseCase;
import com.example.proyectoandroid.utils.Result;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private TextView tvChatTitle;
    private RecyclerView rvMessages;
    private EditText etMessage;
    private Button btnSend;

    private MessageAdapter messageAdapter;
    private List<Message> messagesList = new ArrayList<>();

    private String chatId;
    private String chatTitle;
    private String currentUserId;

    private ListenMessagesUseCase listenMessagesUseCase;
    private ListenerRegistration messagesListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        tvChatTitle = findViewById(R.id.tvChatTitle);
        rvMessages = findViewById(R.id.rvMessages);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);

        chatId = getIntent().getStringExtra("chatId");
        chatTitle = getIntent().getStringExtra("chatTitle");
        tvChatTitle.setText(chatTitle != null ? chatTitle : "Chat");

        User currentUser = ServiceLocator.getInstance(getApplicationContext())
                .provideGetCurrentUserUseCase().execute();
        currentUserId = currentUser != null ? currentUser.getUid() : "";

        messageAdapter = new MessageAdapter(messagesList, currentUserId);
        rvMessages.setLayoutManager(new LinearLayoutManager(this));
        rvMessages.setAdapter(messageAdapter);

        listenMessagesUseCase = ServiceLocator.getInstance(getApplicationContext()).provideListenMessagesUseCase();

        messagesListener = listenMessagesUseCase.listenForMessages(
                chatId,
                50,
                initialMessages -> runOnUiThread(() -> {
                    messageAdapter.setMessages(initialMessages);
                    rvMessages.scrollToPosition(messageAdapter.getItemCount() - 1);
                }),
                newMessage -> runOnUiThread(() -> {
                    messageAdapter.addMessage(newMessage);
                    rvMessages.scrollToPosition(messageAdapter.getItemCount() - 1);
                }),
                updatedMessage -> runOnUiThread(() -> {
                    // Actualización de mensaje si lo necesitas
                })
        );

        btnSend.setOnClickListener(v -> {
            String content = etMessage.getText().toString().trim();
            if (content.isEmpty()) {
                Toast.makeText(this, "Escribe un mensaje", Toast.LENGTH_SHORT).show();
                return;
            }
            listenMessagesUseCase.sendTextMessage(chatId, content)
                    .thenAccept(result -> runOnUiThread(() -> {
                        if (result.isSuccess()) {
                            etMessage.setText("");
                        } else {
                            String error = ((Result.Error<?>) result).getErrorMessage();
                            Toast.makeText(this, "Error enviando mensaje: " + error, Toast.LENGTH_LONG).show();
                        }
                    }));
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listenMessagesUseCase != null && chatId != null) {
            listenMessagesUseCase.stopListeningForMessages(chatId);
        }
        if (messagesListener != null) {
            messagesListener.remove();
        }
    }
}