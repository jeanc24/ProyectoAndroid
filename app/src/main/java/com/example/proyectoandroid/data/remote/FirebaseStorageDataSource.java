package com.example.proyectoandroid.data.remote;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.example.proyectoandroid.utils.Result;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class FirebaseStorageDataSource {

    private static final String TAG = "FirebaseStorageSource";
    private static final String IMAGES_FOLDER = "images";
    private static final int MAX_IMAGE_SIZE_KB = 500; // 500KB máximo
    private static final int COMPRESSION_QUALITY = 80; // Calidad de compresión JPEG (0-100)

    private final FirebaseStorage storage;

    public FirebaseStorageDataSource() {
        this.storage = FirebaseStorage.getInstance();
    }

    public CompletableFuture<Result<String>> uploadImage(Context context, Uri imageUri, String chatId) {
        CompletableFuture<Result<String>> resultFuture = new CompletableFuture<>();

        if (imageUri == null) {
            resultFuture.complete(new Result.Error<>("URI de imagen no válida"));
            return resultFuture;
        }

        try {
            String fileName = UUID.randomUUID().toString() + "." + getFileExtension(context, imageUri);
            StorageReference storageRef = storage.getReference()
                    .child(IMAGES_FOLDER)
                    .child(chatId)
                    .child(fileName);

            File compressedImageFile = compressImage(context, imageUri);

            if (compressedImageFile == null) {
                uploadImageFile(storageRef, imageUri, resultFuture);
            } else {
                Uri compressedUri = Uri.fromFile(compressedImageFile);
                uploadImageFile(storageRef, compressedUri, resultFuture);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error al subir imagen", e);
            resultFuture.complete(new Result.Error<>(e.getMessage()));
        }

        return resultFuture;
    }

    public CompletableFuture<Result<Void>> deleteImage(String imageUrl) {
        CompletableFuture<Result<Void>> resultFuture = new CompletableFuture<>();

        if (imageUrl == null || imageUrl.isEmpty()) {
            resultFuture.complete(new Result.Error<>("URL de imagen no válida"));
            return resultFuture;
        }

        try {
            StorageReference storageRef = storage.getReferenceFromUrl(imageUrl);

            storageRef.delete()
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Imagen eliminada correctamente");
                        resultFuture.complete(new Result.Success<>(null));
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error al eliminar imagen", e);
                        resultFuture.complete(new Result.Error<>(e.getMessage()));
                    });

        } catch (Exception e) {
            Log.e(TAG, "Error al procesar la eliminación de imagen", e);
            resultFuture.complete(new Result.Error<>(e.getMessage()));
        }

        return resultFuture;
    }

    private void uploadImageFile(StorageReference storageRef, Uri fileUri,
            CompletableFuture<Result<String>> resultFuture) {

        UploadTask uploadTask = storageRef.putFile(fileUri);

        uploadTask.continueWithTask(task -> {
            if (!task.isSuccessful() && task.getException() != null) {
                throw task.getException();
            }
            return storageRef.getDownloadUrl();
        }).addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                String downloadUrl = task.getResult().toString();
                Log.d(TAG, "Imagen subida correctamente: " + downloadUrl);
                resultFuture.complete(new Result.Success<>(downloadUrl));
            } else {
                Log.e(TAG, "Error al subir imagen", task.getException());
                resultFuture.complete(new Result.Error<>(task.getException() != null ?
                        task.getException().getMessage() : "Error desconocido"));
            }
        });
    }

    private File compressImage(Context context, Uri imageUri) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
            if (inputStream == null) {
                Log.e(TAG, "No se pudo abrir la imagen desde URI");
                return null;
            }

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(inputStream, null, options);
            inputStream.close();

            int originalWidth = options.outWidth;
            int originalHeight = options.outHeight;
            int maxDimension = 1200; // Máxima dimensión para mantener buena calidad

            int scale = 1;
            while ((originalWidth / scale) > maxDimension || (originalHeight / scale) > maxDimension) {
                scale *= 2;
            }

            inputStream = context.getContentResolver().openInputStream(imageUri);
            options = new BitmapFactory.Options();
            options.inSampleSize = scale;
            options.inJustDecodeBounds = false;

            Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, options);
            if (inputStream != null) {
                inputStream.close();
            }

            if (bitmap == null) {
                Log.e(TAG, "No se pudo decodificar la imagen");
                return null;
            }

            File compressedImageFile = new File(context.getCacheDir(), "compressed_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream fos = new FileOutputStream(compressedImageFile);

            bitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESSION_QUALITY, fos);
            fos.flush();
            fos.close();

            bitmap.recycle();

            Log.d(TAG, "Imagen comprimida correctamente: " + compressedImageFile.length()/1024 + "KB");

            return compressedImageFile;
        } catch (IOException e) {
            Log.e(TAG, "Error al comprimir la imagen", e);
            return null;
        }
    }

    private String getFileExtension(Context context, Uri uri) {
        String extension = "";

        try {
            String mimeType = context.getContentResolver().getType(uri);
            if (mimeType != null) {
                extension = MimeTypeMap.getSingleton()
                        .getExtensionFromMimeType(mimeType);
            } else {
                String path = uri.getPath();
                if (path != null && path.lastIndexOf('.') != -1) {
                    extension = path.substring(path.lastIndexOf('.') + 1);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error al obtener extensión del archivo", e);
        }

        return extension.isEmpty() ? "jpg" : extension;
    }
}
