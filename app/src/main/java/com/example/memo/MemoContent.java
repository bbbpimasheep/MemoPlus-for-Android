package com.example.memo;

import static androidx.core.content.PackageManagerCompat.LOG_TAG;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

public class MemoContent extends AppCompatActivity{
    ImageButton back2home;
    EditText title, time;
    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.memo_content);

        this.back2home = findViewById(R.id.back_button);
        this.title = findViewById(R.id.memo_title);

        back2home.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("RestrictedApi")
            @Override
            public void onClick(View v) {
                Log.d(LOG_TAG, "Back button clicked!");
                Intent intent = new Intent(MemoContent.this, MainActivity.class);
                startActivity(intent);
            }
        });

        Intent intent = getIntent();
        String memoTitle = intent.getStringExtra("TITLE");
        title.setText(memoTitle);
        String memoTime = intent.getStringExtra("TIME");

    }
}
