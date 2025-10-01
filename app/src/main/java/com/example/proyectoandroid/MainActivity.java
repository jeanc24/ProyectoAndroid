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
import com.example.proyectoandroid.di.ServiceLocator;
import com.example.proyectoandroid.domain.usecase.GetCurrentUserUseCase;
import com.example.proyectoandroid.domain.usecase.ListUserChatsUseCase;
import com.example.proyectoandroid.domain.usecase.LoginUserUseCase;
import com.example.proyectoandroid.utils.ImageLoader;
import com.example.proyectoandroid.utils.Result;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView rvChats;
    private ChatAdapter chatAdapter;
    private List<Chat> chatList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializar el caché de imágenes
        ImageLoader.init(getApplicationContext());

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Esta línea elimina el título por defecto ("ProyectoAndroid") del Toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        rvChats = findViewById(R.id.rvChats);
        rvChats.setLayoutManager(new LinearLayoutManager(this));
        chatAdapter = new ChatAdapter(chatList, chat -> openChat(chat));
        rvChats.setAdapter(chatAdapter);

        // Verifica si el usuario está logueado
        GetCurrentUserUseCase getCurrentUserUseCase = ServiceLocator.getInstance(getApplicationContext()).provideGetCurrentUserUseCase();
        if (!getCurrentUserUseCase.isUserLoggedIn()) {
            startActivity(new Intent(this, com.example.proyectoandroid.login.LoginActivity.class));
            finish();
            return;
        }

        // Carga los chats del usuario
        ListUserChatsUseCase listUserChatsUseCase = ServiceLocator.getInstance(getApplicationContext()).provideListUserChatsUseCase();
        listUserChatsUseCase.execute().thenAccept(result -> {
            runOnUiThread(() -> {
                if (result.isSuccess()) {
                    List<Chat> chats = ((Result.Success<List<Chat>>) result).getData();
                    chatList.clear();
                    chatList.addAll(chats);
                    chatAdapter.notifyDataSetChanged();
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
        intent.putExtra("chatTitle", chat.getChatName());
        startActivity(intent);
    }
}