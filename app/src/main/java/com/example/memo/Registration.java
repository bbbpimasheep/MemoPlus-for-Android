package com.example.memo;

import static androidx.core.content.PackageManagerCompat.LOG_TAG;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.json.JSONException;
import org.json.JSONObject;

public class Registration extends AppCompatActivity {
    // 保存 token 的变量
    static String token = null;
    Button registerButton;
    EditText userid, password;
    CircularImageView icon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        this.registerButton = findViewById(R.id.register_button);
        this.userid = findViewById(R.id.enter_id);
        this.password = findViewById(R.id.enter_password);

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    sendPOST(userid.getText().toString(), password.getText().toString());
                } catch (IOException | JSONException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }
    public static void sendPOST(String ID, String password) throws IOException, JSONException {
        URI uri = null;
        try {
            uri = new URI("http://localhost:8000/NotepadServer/register");
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }
        URL url = uri.toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; utf-8");
        conn.setRequestProperty("Accept", "application/json");
        conn.setDoOutput(true);

        String jsonInputString = String.format(
                "{\"userID\": \"%s\", \"password\": \"%s\"}", ID, password);

        try(OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonInputString.getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        int responseCode = conn.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            try(BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"))) {
                StringBuilder response = new StringBuilder();
                String responseLine = null;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                // 解析响应体，获取 token
                JSONObject jsonResponse = new JSONObject(response.toString());
                token = jsonResponse.getString("token");
                System.out.println("Token: " + token);
            }
        } else {
            System.out.println("POST request not working");
        }

        conn.disconnect();
    }

    public static void sendAvatar(ImageView avatar) throws IOException, JSONException {
        URI uri = null;
        try {
            uri = new URI("http://localhost:8000/NotepadServer/register");
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }
        URL url = uri.toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "image/png");
        conn.setRequestProperty("Accept", "application/json, text/plain, */*");
        conn.setDoOutput(true);

        avatar.setDrawingCacheEnabled(true);
        Bitmap bitmap = avatar.getDrawingCache();
        avatar.setDrawingCacheEnabled(false);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);

        try(OutputStream os = conn.getOutputStream()) {
            byte[] imageBytes = byteArrayOutputStream.toByteArray();
            os.write(imageBytes, 0, imageBytes.length);
        }

        int responseCode = conn.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            try(BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"))) {
                StringBuilder response = new StringBuilder();
                String responseLine = null;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                // 解析响应体
                if (conn.getContentType() != null && conn.getContentType().contains("application/json")) {
                    JSONObject jsonResponse = new JSONObject(response.toString());
                    String message = jsonResponse.getString("message");
                    // 假设服务器返回了一个"message"字段，显示上传结果的消息
                    System.out.println("Server response: " + message);
                }
            }
        } else {
            System.out.println("PNG upload failed with HTTP error code: " + responseCode);
        }

        bitmap.recycle();
        conn.disconnect();
    }

    // 获取 token 的方法
    public static String getToken() {
        return token;
    }
}