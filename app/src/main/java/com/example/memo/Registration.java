package com.example.memo;

import static androidx.core.content.PackageManagerCompat.LOG_TAG;

import static com.example.memo.MainActivity.uri_s;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.JSONException;
import org.json.JSONObject;


public class Registration extends AppCompatActivity {
    // 保存 token 的变量
    static String token = null;
    Button registerButton;
    ImageButton back2login;
    TextView showID;
    EditText username, password, confirm;
    CircularImageView icon;
    static ExecutorService executorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        this.username = findViewById(R.id.enter_name);
        this.password = findViewById(R.id.enter_password);
        this.confirm = findViewById(R.id.enter_password_again);
        this.back2login = findViewById(R.id.back_button);
        this.registerButton = findViewById(R.id.register_button);
        this.showID = findViewById(R.id.show_id);

        executorService = Executors.newFixedThreadPool(1);

        back2login.setOnClickListener(v -> {finish();});

        registerButton.setOnClickListener(v -> {
            String pwdText = password.getText().toString(), conText = confirm.getText().toString();
            if (pwdText.equals(conText)) {
                Log.d("shit", "in");
                sendPOST_register(username.getText().toString(), password.getText().toString(), new OnHttpCallback(){
                    @SuppressLint("SetTextI18n")
                    @Override
                    public void onSuccess(String feedBack) {
                        String userID = feedBack;
                        if (!Objects.equals(userID, "Error")) {
                            Handler handler = new Handler(Looper.getMainLooper());
                            handler.post(() -> {
                                showID.setText("User ID: " + userID + "\n Remember it! You won't see this again.");
                                Toast.makeText(Registration.this, "Registration success.", Toast.LENGTH_SHORT).show();
                            });
                        } else {
                            Handler handler = new Handler(Looper.getMainLooper());
                            handler.post(() -> {Toast.makeText(Registration.this, "Registration failed. Please retry.", Toast.LENGTH_SHORT).show();});
                        }
                    }
                    @Override
                    public void onFailure(Exception e) {
                        e.printStackTrace();
                        Handler handler = new Handler(Looper.getMainLooper());
                        handler.post(() -> {Toast.makeText(Registration.this, "Registration failed. Please retry.", Toast.LENGTH_SHORT).show();});
                    }
                });
            } else {
                confirm.setError("Not match");
                confirm.requestFocus();
            }
        });
    }

    public static void sendPOST_register(String username, String password, OnHttpCallback callback)  {
        executorService.submit(() -> {
            try {
                String userID = performRegisterRequest(username, password); // 假设这是获取到的 userID
                callback.onSuccess(userID);
            } catch (Exception e) {
                callback.onFailure(e);
            }
        });
    }

    private static String performRegisterRequest(String username, String password) throws IOException, JSONException {
        URI uri = null;
        try {
            uri = new URI( uri_s + "register");
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return "Error";
        }
        URL url = uri.toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; utf-8");
        conn.setRequestProperty("Accept", "application/json");
        conn.setDoOutput(true);

        String jsonInputString = "{\"username\": \"" + username + "\", \"password\": \"" + password + "\"}";

        try(OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonInputString.getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        String userid = "";
        int responseCode = conn.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            try(BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"))) {
                StringBuilder response = new StringBuilder();
                String responseLine = null;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                JSONObject jsonResponse = new JSONObject(response.toString());
                return jsonResponse.getString("userID");
            }
        } else {
            System.out.println("POST request not worked");
        }
        return "Error";
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