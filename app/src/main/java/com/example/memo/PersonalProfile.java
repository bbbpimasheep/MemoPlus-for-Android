package com.example.memo;

import static androidx.core.content.PackageManagerCompat.LOG_TAG;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PersonalProfile extends AppCompatActivity {
    private static AppDatabase db;
    ImageButton backButton, editProfile;
    TextView nameView, IDView, signView;
    String password;
    ExecutorService executorService;
    CircularImageView iconView;
    @SuppressLint("RestrictedApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        backButton = findViewById(R.id.back_button);
        editProfile = findViewById(R.id.edit_profile_button);
        nameView = findViewById(R.id.name_title);
        IDView = findViewById(R.id.user_id_view);
        signView = findViewById(R.id.signature);
        iconView = findViewById(R.id.icon);

        db = MemoPlus.getInstance().getAppDatabase();

        executorService = Executors.newFixedThreadPool(1);

        displayInfo();

        backButton.setOnClickListener(v -> {
            Log.d(LOG_TAG, "Back button clicked!");
            // Intent intent = new Intent(PersonalProfile.this, MainActivity.class);
            // intent.putExtra("login", true);
            // startActivity(intent);
            finish();
        });

        editProfile.setOnClickListener(v -> {
            Log.d(LOG_TAG, "Back button clicked!");
            Intent intent = new Intent(PersonalProfile.this, EditProfile.class);
            // intent.putExtra("name", nameView.getText().toString());
            // intent.putExtra("ID", IDView.getText().toString());
            // intent.putExtra("signature", signView.getText().toString());
            // intent.putExtra("password", password);
            startActivity(intent);
        });
    }

    private void displayInfo() {
        executorService.submit(() -> {
            User user = db.userDao().getAllUsers().get(0);
            String iconPath = "NoPath";
            if (!Objects.equals(user.avatar, "Null")) {
                // iconPath = downloadIcon();
            }
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(() -> {
                nameView.setText(user.username);
                IDView.setText(user.userID);
                signView.setText(user.signature);
                if (!iconPath.equals("NoPath")) {
                    bindIcon(iconPath);
                }
            });
        });
    }

    private void bindIcon(String iconPath) {
        try {
            File imageFile = new File(iconPath);
            InputStream inputStream = new FileInputStream(imageFile);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            // Bitmap scaledBitmap = scaleBitmapToFitImageView(bitmap, imageView);
            iconView.setImageBitmap(bitmap);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
