package com.example.proyectoandroid.chat;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.proyectoandroid.R;
import com.example.proyectoandroid.data.model.Message;
import com.example.proyectoandroid.data.model.User;
import com.example.proyectoandroid.data.remote.FirestoreDataSource;
import com.example.proyectoandroid.di.ServiceLocator;
import com.example.proyectoandroid.domain.usecase.ListenMessagesUseCase;
import com.example.proyectoandroid.utils.Result;
import com.squareup.picasso.Picasso;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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
    private String otherUserId;
    private User otherUser;

    private ListenMessagesUseCase listenMessagesUseCase;
    private ListenerRegistration messagesListener;
    private FirestoreDataSource firestoreDataSource;

    // Image picker launcher
    private ActivityResultLauncher<String> pickImageLauncher;

    // UI for header/status
    private ImageView imgAvatar;
    private TextView tvChatTitle;
    private TextView tvOnlineStatus;
    private View onlineIndicator;

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

        // Header/status UI (assuming you have these views in your layout)
        imgAvatar = findViewById(R.id.imgAvatar);
        tvChatTitle = findViewById(R.id.tvChatTitle);
        tvOnlineStatus = findViewById(R.id.tvOnlineStatus);
        onlineIndicator = findViewById(R.id.onlineIndicator);

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

        firestoreDataSource = new FirestoreDataSource();

        // Obtener el otro usuario (suponiendo que en chat privado hay dos participantes)
        firestoreDataSource.getChatById(chatId)
                .thenAccept(result -> {
                    if (result instanceof Result.Success) {
                        List<String> participants = ((Result.Success<com.example.proyectoandroid.data.model.Chat>) result).getData().getParticipantIds();
                        for (String uid : participants) {
                            if (!uid.equals(currentUserId)) {
                                otherUserId = uid;
                                break;
                            }
                        }
                        // Escuchar el estado en línea del otro usuario
                        firestoreDataSource.addUserStatusListener(otherUserId, user -> runOnUiThread(() -> {
                            otherUser = user;
                            setupChatHeader(otherUser);
                        }));
                    }
                });

        listenMessagesUseCase = ServiceLocator.getInstance(getApplicationContext()).provideListenMessagesUseCase();

        messagesListener = listenMessagesUseCase.listenForMessagesWithoutMarkingAsRead(
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
            // Enviar el mensaje; el use case cifra internamente
            listenMessagesUseCase.sendTextMessage(chatId, content)
                    .thenAccept(result -> runOnUiThread(() -> {
                        if (result.isSuccess()) {
                            etMessage.setText("");
                            // Marcar el chat como leído únicamente después de enviar
                            listenMessagesUseCase.markChatAsRead(chatId);
                        } else {
                            String error = ((com.example.proyectoandroid.utils.Result.Error<?>) result).getErrorMessage();
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
                                // Marcar el chat como leído tras enviar imagen
                                listenMessagesUseCase.markChatAsRead(chatId);
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

    /**
     * Actualiza el header del chat con el avatar y el estado en línea del otro usuario
     */
    private void setupChatHeader(User user) {
        if (tvChatTitle != null) {
            tvChatTitle.setText(user.getDisplayName());
        }
        if (imgAvatar != null) {
            if (user.getPhotoUrl() != null && !user.getPhotoUrl().isEmpty()) {
                Picasso.get().load(user.getPhotoUrl()).placeholder(R.drawable.ic_profile_placeholder).into(imgAvatar);
            } else {
                imgAvatar.setImageResource(R.drawable.ic_profile_placeholder);
            }
        }
        if (user.isOnline()) {
            if (onlineIndicator != null) onlineIndicator.setVisibility(android.view.View.VISIBLE);
            if (tvOnlineStatus != null) {
                tvOnlineStatus.setVisibility(android.view.View.VISIBLE);
                tvOnlineStatus.setText("En línea");
                tvOnlineStatus.setTextColor(0xFF4CAF50); // Verde
            }
        } else {
            if (onlineIndicator != null) onlineIndicator.setVisibility(android.view.View.GONE);
            if (tvOnlineStatus != null) {
                tvOnlineStatus.setVisibility(android.view.View.VISIBLE);
                tvOnlineStatus.setText("Últ. vez: " + formatLastOnline(user.getLastOnline()));
                tvOnlineStatus.setTextColor(0xFF888888); // Gris
            }
        }
    }

    private String formatLastOnline(long lastOnlineMillis) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        return sdf.format(new Date(lastOnlineMillis));
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
        if (firestoreDataSource != null && otherUserId != null) {
            firestoreDataSource.removeUserStatusListener(otherUserId);
        }
    }
}