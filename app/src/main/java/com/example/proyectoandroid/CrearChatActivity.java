package com.example.proyectoandroid;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.proyectoandroid.R;
import com.example.proyectoandroid.chat.ChatActivity;
import com.example.proyectoandroid.data.model.User;
import com.example.proyectoandroid.di.ServiceLocator;
import com.example.proyectoandroid.domain.usecase.CreateChatUseCase;
import com.example.proyectoandroid.domain.usecase.GetCurrentUserUseCase;
import com.example.proyectoandroid.utils.Result;
import java.util.ArrayList;
import java.util.List;

public class CrearChatActivity extends AppCompatActivity {

    private EditText etSearch;
    private RecyclerView rvUsers;
    private ProgressBar progressBar;
    private UserAdapter userAdapter;
    private List<User> userList = new ArrayList<>();
    private List<User> filteredList = new ArrayList<>();
    private CreateChatUseCase createChatUseCase;
    private GetCurrentUserUseCase getCurrentUserUseCase;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_chat);

        etSearch = findViewById(R.id.etSearch);
        rvUsers = findViewById(R.id.rvUsers);
        progressBar = findViewById(R.id.progressBar);

        createChatUseCase = ServiceLocator.getInstance(getApplicationContext()).provideCreateChatUseCase();
        getCurrentUserUseCase = ServiceLocator.getInstance(getApplicationContext()).provideGetCurrentUserUseCase();
        currentUser = getCurrentUserUseCase.execute();

        userAdapter = new UserAdapter(filteredList, this::startChatWithUser);
        rvUsers.setLayoutManager(new LinearLayoutManager(this));
        rvUsers.setAdapter(userAdapter);

        loadUsers();

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterUsers(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadUsers() {
        progressBar.setVisibility(ProgressBar.VISIBLE);

        ServiceLocator.getInstance(getApplicationContext()).provideFirestoreDataSource()
                .firestore.collection("users")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    userList.clear();
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : querySnapshot) {
                        User user = doc.toObject(User.class);
                        // Evita NullPointerException verificando null antes de comparar
                        if (currentUser != null && user.getUid() != null && currentUser.getUid() != null && !user.getUid().equals(currentUser.getUid())) {
                            userList.add(user);
                        }
                    }
                    filterUsers(etSearch.getText().toString());
                    progressBar.setVisibility(ProgressBar.GONE);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error cargando usuarios: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    progressBar.setVisibility(ProgressBar.GONE);
                });
    }

    // Reemplaza streams por bucle para compatibilidad con minSdk < 24
    private void filterUsers(String query) {
        filteredList.clear();
        if (query == null || query.isEmpty()) {
            filteredList.addAll(userList);
        } else {
            String q = query.toLowerCase();
            for (User u : userList) {
                if ((u.getDisplayName() != null && u.getDisplayName().toLowerCase().contains(q)) ||
                        (u.getEmail() != null && u.getEmail().toLowerCase().contains(q))) {
                    filteredList.add(u);
                }
            }
        }
        userAdapter.notifyDataSetChanged();
    }

    private void startChatWithUser(User user) {
        progressBar.setVisibility(ProgressBar.VISIBLE);

        createChatUseCase.execute(user.getUid()).thenAccept(result -> runOnUiThread(() -> {
            progressBar.setVisibility(ProgressBar.GONE);

            if (result.isSuccess()) {
                com.example.proyectoandroid.data.model.Chat chat = ((Result.Success<com.example.proyectoandroid.data.model.Chat>) result).getData();
                Intent intent = new Intent(this, ChatActivity.class);
                intent.putExtra("chatId", chat.getChatId());
                intent.putExtra("chatTitle", chat.getChatName() != null ? chat.getChatName() : user.getDisplayName());
                startActivity(intent);
                finish();
            } else {
                String error = ((Result.Error<?>) result).getErrorMessage();
                Toast.makeText(this, "Error creando chat: " + error, Toast.LENGTH_LONG).show();
            }
        }));
    }
}