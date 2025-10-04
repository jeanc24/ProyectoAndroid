package com.example.proyectoandroid.chat;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.proyectoandroid.R;
import com.example.proyectoandroid.data.model.Message;
import com.example.proyectoandroid.data.model.User;
import com.example.proyectoandroid.di.ServiceLocator;
import com.example.proyectoandroid.domain.usecase.ListenMessagesUseCase;
import com.example.proyectoandroid.utils.Result;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView rvMessages;
    private EditText etMessage;
    private Button btnSend;
    private Button btnAttachImage; // new

    private MessageAdapter messageAdapter;
    private List<Message> messagesList = new ArrayList<>();

    private String chatId;
    private String chatTitle;
    private String currentUserId;

    private ListenMessagesUseCase listenMessagesUseCase;
    private ListenerRegistration messagesListener;

    // Image picker launcher
    private ActivityResultLauncher<String> pickImageLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        rvMessages = findViewById(R.id.rvMessages);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        btnAttachImage = findViewById(R.id.btnAttachImage);

        chatId = getIntent().getStringExtra("chatId");
        chatTitle = getIntent().getStringExtra("chatTitle");
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(chatTitle != null ? chatTitle : "Chat");
        }

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
                    // Manejar actualización si se requiere
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

        // Register image picker
        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                setSendingEnabled(false);
                Toast.makeText(this, "Subiendo imagen...", Toast.LENGTH_SHORT).show();
                listenMessagesUseCase
                        .uploadAndSendImageMessage(getApplicationContext(), chatId, uri)
                        .thenAccept(result -> runOnUiThread(() -> {
                            setSendingEnabled(true);
                            if (result.isSuccess()) {
                                Toast.makeText(this, "Imagen enviada", Toast.LENGTH_SHORT).show();
                            } else {
                                String error = ((Result.Error<?>) result).getErrorMessage();
                                Toast.makeText(this, "Error enviando imagen: " + error, Toast.LENGTH_LONG).show();
                            }
                        }));
            }
        });

        btnAttachImage.setOnClickListener(v -> {
            try {
                pickImageLauncher.launch("image/*");
            } catch (Exception e) {
                Toast.makeText(this, "No se pudo abrir el selector de imágenes", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setSendingEnabled(boolean enabled) {
        btnSend.setEnabled(enabled);
        btnAttachImage.setEnabled(enabled);
        etMessage.setEnabled(enabled);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        getOnBackPressedDispatcher().onBackPressed();
        return true;
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