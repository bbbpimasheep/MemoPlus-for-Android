package com.example.memo;

import static androidx.core.content.PackageManagerCompat.LOG_TAG;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.List;

public class PersonalProfile extends AppCompatActivity {
    private static AppDatabase db;
    ImageButton backButton, editProfile;
    TextView nameView, IDView, signView;
    String password;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        backButton = findViewById(R.id.back_button);
        editProfile = findViewById(R.id.edit_profile_button);
        nameView = findViewById(R.id.name_title);
        IDView = findViewById(R.id.user_id_view);
        signView = findViewById(R.id.signature);

        db = MemoPlus.getInstance().getAppDatabase();
        new Thread(() -> {
            User user = db.userDao().getAllUsers().get(0);
            nameView.setText(user.username);
            IDView.setText(user.userID);
            signView.setText(user.signature);
        }).start();

        backButton.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("RestrictedApi")
            @Override
            public void onClick(View v) {
                Log.d(LOG_TAG, "Back button clicked!");
                Intent intent = new Intent(PersonalProfile.this, MainActivity.class);
                intent.putExtra("login", false);
                startActivity(intent);
            }
        });

        editProfile.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("RestrictedApi")
            @Override
            public void onClick(View v) {
                Log.d(LOG_TAG, "Back button clicked!");
                Intent intent = new Intent(PersonalProfile.this, EditProfile.class);
                intent.putExtra("name", nameView.getText().toString());
                intent.putExtra("ID", IDView.getText().toString());
                intent.putExtra("signature", signView.getText().toString());
                intent.putExtra("password", password);
                startActivity(intent);
            }
        });
    }
}
