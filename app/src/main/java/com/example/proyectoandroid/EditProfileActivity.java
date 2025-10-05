package com.example.proyectoandroid;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.example.proyectoandroid.data.model.User;
import com.example.proyectoandroid.data.remote.FirestoreDataSource;
import com.example.proyectoandroid.data.remote.FirebaseStorageDataSource;
import com.example.proyectoandroid.utils.ImageLoader;
import com.example.proyectoandroid.utils.Result;
import com.google.firebase.auth.FirebaseAuth;
import com.squareup.picasso.Picasso;

public class EditProfileActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 123;
    private ImageView profileImage;
    private EditText editDisplayName;
    private Button btnChangePhoto, btnSaveProfile;
    private Uri imageUri = null;
    private FirestoreDataSource firestoreDataSource;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        profileImage = findViewById(R.id.profileImage);
        editDisplayName = findViewById(R.id.editDisplayName);
        btnChangePhoto = findViewById(R.id.btnChangePhoto);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);

        firestoreDataSource = new FirestoreDataSource();

        // Cargar usuario actual
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        firestoreDataSource.getUserById(uid)
                .thenAccept(result -> {
                    if (result instanceof Result.Success) {
                        currentUser = ((Result.Success<User>) result).getData();
                        runOnUiThread(() -> updateUIWithUser(currentUser));
                    } else {
                        runOnUiThread(() -> {
                            Toast.makeText(this, "No se pudo cargar el usuario", Toast.LENGTH_SHORT).show();
                            finish();
                        });
                    }
                });

        btnChangePhoto.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, PICK_IMAGE_REQUEST);
        });
        Button btnRemovePhoto = findViewById(R.id.btnRemovePhoto);

        btnRemovePhoto.setOnClickListener(v -> {
            imageUri = null; // Olvida cualquier imagen temporal
            if (currentUser != null) {
                currentUser.setPhotoUrl(""); // Elimina la URL de la foto
            }
            profileImage.setImageResource(R.drawable.ic_profile_placeholder); // Muestra el placeholder
        });

        btnSaveProfile.setOnClickListener(v -> {
            if (currentUser == null) {
                Toast.makeText(this, "Usuario no cargado", Toast.LENGTH_SHORT).show();
                return;
            }
            String newName = editDisplayName.getText().toString().trim();
            if (imageUri != null) {
                uploadImageAndUpdateProfile(imageUri, newName);
            } else {
                updateProfile(newName, currentUser.getPhotoUrl());
            }
        });
    }

    private void updateUIWithUser(User user) {
        if (user.getPhotoUrl() != null && !user.getPhotoUrl().isEmpty()) {
            Picasso.get().load(user.getPhotoUrl()).placeholder(R.drawable.ic_profile_placeholder).into(profileImage);
        } else {
            profileImage.setImageResource(R.drawable.ic_profile_placeholder);
        }
        editDisplayName.setText(user.getDisplayName());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            imageUri = data.getData();
            Picasso.get().load(imageUri).placeholder(R.drawable.ic_profile_placeholder).into(profileImage);
        }
    }

    private void uploadImageAndUpdateProfile(Uri uri, String newName) {
        // Usa tu FirebaseStorageDataSource
        FirebaseStorageDataSource storageDataSource = new FirebaseStorageDataSource();
        String uid = currentUser.getUid();
        storageDataSource.uploadImage(this, uri, uid)
                .thenAccept(result -> {
                    if (result instanceof Result.Success) {
                        String imageUrl = ((Result.Success<String>) result).getData();
                        runOnUiThread(() -> updateProfile(newName, imageUrl));
                    } else {
                        runOnUiThread(() -> Toast.makeText(this, "Error subiendo imagen", Toast.LENGTH_SHORT).show());
                    }
                });
    }

    private void updateProfile(String newName, String imageUrl) {
        currentUser.setDisplayName(newName);
        currentUser.setPhotoUrl(imageUrl != null ? imageUrl : "");
        firestoreDataSource.createOrUpdateUser(currentUser)
                .thenAccept(result -> runOnUiThread(() -> {
                    Toast.makeText(this, "Perfil actualizado", Toast.LENGTH_SHORT).show();
                    finish();
                }));
    }
}