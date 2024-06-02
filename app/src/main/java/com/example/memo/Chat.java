package com.example.memo;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.example.memo.MainActivity.uri_s;

public class Chat extends AppCompatActivity {
    private ImageButton back2Home;
    private String messageText;
    private TextView messageTextView;
    private String userID;
    ExecutorService executorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        Intent intent = getIntent();
        userID = intent.getStringExtra("ID");
        Log.d("IDID", userID);

        back2Home = findViewById(R.id.back_button);
        messageTextView = findViewById(R.id.messageText);
        messageTextView.setMovementMethod(new ScrollingMovementMethod());

        messageText = "正在为您准备个性推荐……";
        messageTextView.setText(messageText);

        back2Home.setOnClickListener(v -> {
            finish();
        });

        executorService = Executors.newFixedThreadPool(1);
        executorService.submit(() -> {
            try {
                sendPOST_return_personalized_recommendation(userID);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void sendPOST_return_personalized_recommendation(String userID) throws IOException, JSONException {
        URI uri = null;
        try {
            uri = new URI(uri_s + "return_personalized_recommendation");
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }
        URL url = uri.toURL();
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) url.openConnection();
        } catch (IOException e) {
            Log.d("error", "Failed to open connection: " + e.toString());
            return;
        }

        try {
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; utf-8");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);
        } catch (ProtocolException e) {
            Log.d("error", "Failed to set request method or properties: " + e.toString());
            return;
        }

        JSONObject jsonInputString = new JSONObject();
        jsonInputString.put("userID", userID);

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonInputString.toString().getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        } catch (Exception e) {
            Log.d("error", e.toString());
        }

        if (conn == null) {
            messageTextView.setText("抱歉，暂时没有个性推荐");
            return;
        }

        int responseCode = 200;
        try {
            responseCode = conn.getResponseCode();
        } catch (Exception e) {
            Log.d("error", e.toString());
        }

        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }

                // 解析 JSON 响应
                JSONObject jsonResponse = new JSONObject(response.toString());
                // 假设返回的数据结构中有个 key 为 "personalizedRecommendation" 的字段
                String personalizedRecommendation = jsonResponse.getString("personalizedRecommendation");

                messageTextView.setText(personalizedRecommendation);
            } catch (JSONException e) {
                Log.d("error", "Failed to parse JSON response: " + e.toString());
                messageTextView.setText("抱歉，解析个性推荐时出错");
            }
        } else {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                messageTextView.setText("Error: " + response.toString());
            }
        }
    }
}