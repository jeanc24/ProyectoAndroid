package com.example.proyectoandroid;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.example.proyectoandroid.data.model.Chat;
import com.example.proyectoandroid.data.model.User;
import com.example.proyectoandroid.di.ServiceLocator;
import com.example.proyectoandroid.domain.usecase.GetCurrentUserUseCase;
import com.example.proyectoandroid.domain.usecase.LoginUserUseCase;
import com.example.proyectoandroid.utils.ImageLoader;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private RecyclerView rvChats;
    private ChatAdapter chatAdapter;
    private List<Chat> chatList = new ArrayList<>();
    private Map<String, User> userMap = new HashMap<>();
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ImageLoader.init(getApplicationContext());

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(this, com.example.proyectoandroid.CrearChatActivity.class);
            startActivity(intent);
        });

        rvChats = findViewById(R.id.rvChats);
        rvChats.setLayoutManager(new LinearLayoutManager(this));

        GetCurrentUserUseCase getCurrentUserUseCase = ServiceLocator.getInstance(getApplicationContext()).provideGetCurrentUserUseCase();
        if (!getCurrentUserUseCase.isUserLoggedIn()) {
            startActivity(new Intent(this, com.example.proyectoandroid.login.LoginActivity.class));
            finish();
            return;
        }
        currentUserId = getCurrentUserUseCase.execute().getUid();

        loadAllUsersAndChats();
    }

    private void loadAllUsersAndChats() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    userMap.clear();
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : querySnapshot) {
                        User user = doc.toObject(User.class);
                        userMap.put(user.getUid(), user);
                    }
                    listenChatsRealtime();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error cargando usuarios: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void listenChatsRealtime() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("chats")
                .whereArrayContains("participantIds", currentUserId)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Error cargando chats: " + error.getMessage(), Toast.LENGTH_LONG).show();
                        return;
                    }
                    chatList.clear();
                    if (querySnapshot != null) {
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            Chat chat = doc.toObject(Chat.class);
                            chatList.add(chat);
                        }
                        // Ordenar por timestamp más reciente primero
                        Collections.sort(chatList, (c1, c2) -> {
                            Date d1 = c1.getLastMessageTimestamp();
                            Date d2 = c2.getLastMessageTimestamp();
                            long t1 = (d1 != null) ? d1.getTime() : 0L;
                            long t2 = (d2 != null) ? d2.getTime() : 0L;
                            return Long.compare(t2, t1);
                        });
                        if (chatAdapter == null) {
                            chatAdapter = new ChatAdapter(chatList, userMap, currentUserId, chat -> openChat(chat));
                            rvChats.setAdapter(chatAdapter);
                        } else {
                            chatAdapter.notifyDataSetChanged();
                        }
                    }
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            LoginUserUseCase loginUserUseCase = ServiceLocator.getInstance(getApplicationContext()).provideLoginUserUseCase();
            loginUserUseCase.signOut();
            Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, com.example.proyectoandroid.login.LoginActivity.class));
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void openChat(Chat chat) {
        Intent intent = new Intent(this, com.example.proyectoandroid.chat.ChatActivity.class);
        intent.putExtra("chatId", chat.getChatId());
        intent.putExtra("chatTitle", getOtherUserDisplayName(chat));
        startActivity(intent);
    }

    private String getOtherUserDisplayName(Chat chat) {
        if (!chat.isGroupChat() && chat.getParticipantIds().size() == 2 && currentUserId != null) {
            String otherUserId = chat.getParticipantIds().get(0).equals(currentUserId)
                    ? chat.getParticipantIds().get(1)
                    : chat.getParticipantIds().get(0);
            User otherUser = userMap.get(otherUserId);
            return (otherUser != null && otherUser.getDisplayName() != null && !otherUser.getDisplayName().isEmpty())
                    ? otherUser.getDisplayName()
                    : otherUserId;
        } else {
            return chat.getChatName();
        }
    }
}