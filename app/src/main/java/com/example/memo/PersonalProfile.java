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
    ImageButton backButton, editProfile;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        /*
        userDao = ((MemoPlus) getApplication()).getDb().userDao();

        new Thread(new Runnable() {
            @Override
            public void run() {
                // 使用 UserDao 的 getAll 方法获取所有用户
                users = userDao.getAll();
                // 记得在主线程中更新UI
                runOnUiThread(() -> updateProfileUI(users.get(0)));
            }
            @SuppressLint("SetTextI18n")
            private void updateProfileUI(User user) {
                TextView nameTitle = findViewById(R.id.name_title);
                TextView idView = findViewById(R.id.user_id_view);
                TextView signature = findViewById(R.id.signature);
                nameTitle.setText(user.name);
                idView.setText("ID: " + user.ID);
                signature.setText("“ " + user.signature + " ”");
            }
        }).start();
        */
        backButton = findViewById(R.id.back_button);
        editProfile = findViewById(R.id.edit_profile_button);

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
                startActivity(intent);
            }
        });
    }
}
