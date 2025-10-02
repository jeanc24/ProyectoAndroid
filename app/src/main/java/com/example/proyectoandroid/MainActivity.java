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
import com.example.proyectoandroid.domain.usecase.ListUserChatsUseCase;
import com.example.proyectoandroid.domain.usecase.LoginUserUseCase;
import com.example.proyectoandroid.utils.Result;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// IMPORTANTE: Asegúrate de tener el modelo User completo con getUid() y getDisplayName()

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

        // 1. Obtén el usuario actual
        GetCurrentUserUseCase getCurrentUserUseCase = ServiceLocator.getInstance(getApplicationContext()).provideGetCurrentUserUseCase();
        if (!getCurrentUserUseCase.isUserLoggedIn()) {
            startActivity(new Intent(this, com.example.proyectoandroid.login.LoginActivity.class));
            finish();
            return;
        }
        currentUserId = getCurrentUserUseCase.execute().getUid();

        // 2. Carga todos los usuarios (para el userMap)
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
                    // Cuando tengas el userMap, carga los chats
                    loadChats();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error cargando usuarios: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void loadChats() {
        ListUserChatsUseCase listUserChatsUseCase = ServiceLocator.getInstance(getApplicationContext()).provideListUserChatsUseCase();
        listUserChatsUseCase.execute().thenAccept(result -> {
            runOnUiThread(() -> {
                if (result.isSuccess()) {
                    List<Chat> chats = ((Result.Success<List<Chat>>) result).getData();
                    chatList.clear();
                    chatList.addAll(chats);
                    // Inicializa el adapter con el userMap y el id actual
                    if (chatAdapter == null) {
                        chatAdapter = new ChatAdapter(chatList, userMap, currentUserId, chat -> openChat(chat));
                        rvChats.setAdapter(chatAdapter);
                    } else {
                        chatAdapter.notifyDataSetChanged();
                    }
                } else {
                    String error = ((Result.Error<?>) result).getErrorMessage();
                    Toast.makeText(this, "Error cargando chats: " + error, Toast.LENGTH_LONG).show();
                }
            });
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
            // Lógica para cerrar sesión
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
        // Puedes pasar el nombre del otro usuario si quieres mostrarlo en el encabezado del chat
        intent.putExtra("chatTitle", getOtherUserDisplayName(chat));
        startActivity(intent);
    }

    // Función para obtener el nombre del otro usuario en chat individual
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