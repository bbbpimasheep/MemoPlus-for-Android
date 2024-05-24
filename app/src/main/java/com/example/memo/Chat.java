package com.example.memo;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class Chat extends AppCompatActivity {
    private RecyclerView recyclerView;
    private MessageAdapter messageAdapter;
    private List<Message> messageList;
    private EditText messageInput;
    private Button sendButton;
    private ImageButton back2Home;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        recyclerView = findViewById(R.id.recyclerView);
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);
        back2Home = findViewById(R.id.back_button);

        messageList = new ArrayList<>();
        messageAdapter = new MessageAdapter(this, messageList);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(messageAdapter);

        back2Home.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Chat.this, MainActivity.class);
                startActivity(intent);
            }
        });

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String messageText = messageInput.getText().toString();
                if (!messageText.isEmpty()) {
                    messageList.add(new Message(messageText, Message.TYPE_SENT));
                    messageAdapter.notifyItemInserted(messageList.size() - 1);
                    recyclerView.scrollToPosition(messageList.size() - 1);
                    messageInput.setText("");

                    // Simulate receiving a message
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            messageList.add(new Message("Received: " + messageText, Message.TYPE_RECEIVED));
                            messageAdapter.notifyItemInserted(messageList.size() - 1);
                            recyclerView.scrollToPosition(messageList.size() - 1);
                        }
                    }, 1000);
                }
            }
        });
    }
}
