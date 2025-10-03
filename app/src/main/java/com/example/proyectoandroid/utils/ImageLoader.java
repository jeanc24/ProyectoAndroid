package com.example.proyectoandroid.utils;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;

import com.squareup.picasso.OkHttp3Downloader;
import com.squareup.picasso.Picasso;

import okhttp3.Cache;
import okhttp3.OkHttpClient;
import java.io.File;

public class ImageLoader {

    private static final String TAG = "ImageLoader";
    private static final int CACHE_SIZE = 250 * 1024 * 1024; // 250MB
    private static Picasso instance;

    public static void init(@NonNull Context context) {
        if (instance == null) {
            File cacheDir = new File(context.getCacheDir(), "picasso-cache");
            if (!cacheDir.exists()) {
                cacheDir.mkdirs();
            }

            Cache cache = new Cache(cacheDir, CACHE_SIZE);
            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .cache(cache)
                    .build();

            Picasso.Builder builder = new Picasso.Builder(context);
            builder.downloader(new OkHttp3Downloader(okHttpClient));
            builder.indicatorsEnabled(false); // Deshabilitar indicadores en producción

            builder.listener((picasso, uri, exception) ->
                    Log.e(TAG, "Error loading image: " + uri, exception));

            instance = builder.build();
            Picasso.setSingletonInstance(instance);
        }
    }

    public static Picasso getInstance() {
        if (instance == null) {
            throw new IllegalStateException("ImageLoader no inicializado. Llamar a init() primero.");
        }
        return instance;
    }

    public static void clearCache(Context context) {
        try {
            // Limpiar caché de disco
            File cacheDir = new File(context.getCacheDir(), "picasso-cache");
            if (cacheDir.exists() && cacheDir.isDirectory()) {
                for (File file : cacheDir.listFiles()) {
                    file.delete();
                }
            }

            // También limpiamos el caché de memoria
            if (instance != null) {
                Picasso.setSingletonInstance(null);
                instance = null;
                init(context); // Reinicializar con caché limpio
            }
        } catch (Exception e) {
            Log.e(TAG, "Error clearing cache", e);
        }
    }

    public static void prefetchImage(String url) {
        if (url != null && !url.isEmpty() && instance != null) {
            instance.load(url).fetch();
        }
    }
}
