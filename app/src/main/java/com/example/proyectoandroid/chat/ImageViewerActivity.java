package com.example.proyectoandroid.chat;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.proyectoandroid.R;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

public class ImageViewerActivity extends AppCompatActivity {

    private static final String EXTRA_IMAGE_URL = "image_url";
    private static final String EXTRA_IMAGE_SENDER = "image_sender";

    private ImageView ivFullScreenImage;
    private ProgressBar progressBar;

    public static void launch(Context context, String imageUrl, String sender) {
        Intent intent = new Intent(context, ImageViewerActivity.class);
        intent.putExtra(EXTRA_IMAGE_URL, imageUrl);
        intent.putExtra(EXTRA_IMAGE_SENDER, sender);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_viewer);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

            // Obtener el nombre del remitente del intent
            String sender = getIntent().getStringExtra(EXTRA_IMAGE_SENDER);
            getSupportActionBar().setTitle(sender != null ? sender : "Imagen");
        }

        ivFullScreenImage = findViewById(R.id.ivFullScreenImage);
        progressBar = findViewById(R.id.progressBar);

        String imageUrl = getIntent().getStringExtra(EXTRA_IMAGE_URL);
        if (imageUrl != null && !imageUrl.isEmpty()) {
            loadImage(imageUrl);
        } else {
            Toast.makeText(this, "Error: URL de imagen no válida", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void loadImage(String imageUrl) {
        progressBar.setVisibility(View.VISIBLE);

        Picasso.get()
                .load(imageUrl)
                .into(ivFullScreenImage, new Callback() {
                    @Override
                    public void onSuccess() {
                        progressBar.setVisibility(View.GONE);
                    }

                    @Override
                    public void onError(Exception e) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(ImageViewerActivity.this,
                                "Error cargando la imagen", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
