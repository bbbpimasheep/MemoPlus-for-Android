package com.example.memo;

import static androidx.core.content.PackageManagerCompat.LOG_TAG;

import static com.example.memo.MainActivity.uri_s;
import static com.example.memo.Registration.getToken;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EditProfile extends AppCompatActivity {
    private static AppDatabase db;
    private UserDao userDao;
    static final int SELECT_IMAGE_REQUEST = 1;
    private static String authToken;
    ImageButton cancelButton, selectButton, setNameButton, setPwdButton, setSignButton, setAvatarButton;
    EditText editName, oldPwd, newPwd, editSign;
    TextView IDView;
    CircularImageView iconView;
    String userName, userID, password, signature;
    Parcelable iconUrl;
    ExecutorService executorService;

    @SuppressLint("RestrictedApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        this.cancelButton = findViewById(R.id.cancel_button);
        this.selectButton = findViewById(R.id.select_icon_button);
        this.setNameButton = findViewById(R.id.setname_button);
        this.setPwdButton = findViewById(R.id.setpwd_button);
        this.setSignButton = findViewById(R.id.setsign_button);
        this.setAvatarButton = findViewById(R.id.setavatar_button);
        this.IDView = findViewById(R.id.user_id);
        this.editName = findViewById(R.id.name);
        this.oldPwd = findViewById(R.id.old_password);
        this.newPwd = findViewById(R.id.new_password);
        this.editSign = findViewById(R.id.signature);

        db = MemoPlus.getInstance().getAppDatabase();
        userDao = db.userDao();

        executorService = Executors.newFixedThreadPool(1);
        /*
        Intent intent = getIntent();
        userName = intent.getStringExtra("name");
        editName.setText(userName);
        userID = intent.getStringExtra("ID");
        IDView.setText(userID);
        password = intent.getStringExtra("password");
        oldPwd.setText(password);
        signature = intent.getStringExtra("signature");
        editSign.setText(signature);
         */

        initializeInfo();

        cancelButton.setOnClickListener(v -> {
            Log.d(LOG_TAG, "Cancel button clicked!");
            // Intent intent = new Intent(EditProfile.this, PersonalProfile.class);
            // startActivity(intent);
            finish();
        });

        setNameButton.setOnClickListener(v -> {
            executorService.submit(() -> {
                sendPOST_changeUsername(userID, editName.getText().toString(), new OnHttpCallback(){
                    @Override
                    public void onSuccess(String feedBack) {
                        if (Objects.equals(feedBack, "Success")) {
                            userDao.updateUsername(userID, editName.getText().toString());
                            Handler handler = new Handler(Looper.getMainLooper());
                            handler.post(() -> {Toast.makeText(EditProfile.this, "OK", Toast.LENGTH_SHORT).show();});
                        }
                    }
                    @Override
                    public void onFailure(Exception e) {
                        e.printStackTrace();
                    }
                });
            });
        });

        setPwdButton.setOnClickListener(v -> {
            String newPassword = newPwd.getText().toString();
            if (!password.equals(newPassword)) {
                executorService.submit(() -> {
                    sendPOST_changePassword(userID, password, newPassword, new OnHttpCallback(){
                        @Override
                        public void onSuccess(String feedBack) {
                            if (Objects.equals(feedBack, "Success")) {
                                userDao.updatePassword(userID, newPassword);
                                Handler handler = new Handler(Looper.getMainLooper());
                                handler.post(() -> {Toast.makeText(EditProfile.this, "OK", Toast.LENGTH_SHORT).show();});
                            }
                        }
                        @Override
                        public void onFailure(Exception e) {
                            e.printStackTrace();
                        }
                    });
                });
            } else {
                newPwd.setError("Same!");
                newPwd.requestFocus();
            }
        });

        setSignButton.setOnClickListener(v -> {
            executorService.submit(() -> {
                sendPOST_changePersonalSignature(userID, editSign.getText().toString(), new OnHttpCallback(){
                    @Override
                    public void onSuccess(String feedBack) {
                        if (Objects.equals(feedBack, "Success")) {
                            userDao.updateSignature(userID, editSign.getText().toString());
                            Handler handler = new Handler(Looper.getMainLooper());
                            handler.post(() -> {Toast.makeText(EditProfile.this, "OK", Toast.LENGTH_SHORT).show();});
                        }
                    }
                    @Override
                    public void onFailure(Exception e) {
                        e.printStackTrace();
                    }
                });
            });
        });

        setAvatarButton.setOnClickListener(v -> {
            try {
                changeAvatar(iconView);
            } catch (IOException | JSONException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void initializeInfo() {
        executorService.submit(() -> {
            User user = userDao.getAllUsers().get(0);
            authToken = user.token;
            password = user.password;
            userID = user.userID;
            String iconPath = "NoPath";
            if (!Objects.equals(user.avatar, "Null")) {
                // iconPath = downloadIcon();
            }
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(() -> {
                editName.setText(user.username);
                IDView.setText(user.userID);
                oldPwd.setText(user.password);
                editSign.setText(user.signature);
                if (!iconPath.equals("NoPath")) {
                    // bindIcon(iconPath);
                }
            });
        });
    }

    public static void sendPOST_changeUsername(String userID, String newName, OnHttpCallback callback) {
        try {
            String feedBack = performChangeUsername(userID, newName); // 假设这是获取到的 userID
            callback.onSuccess(feedBack);
        } catch (Exception e) {
            callback.onFailure(e);
        }
    }

    private static String performChangeUsername(String userID, String newName) throws IOException, JSONException {
        Log.d("change-name", newName);
        URI uri = null;
        try {
            uri = new URI( uri_s + "changeUsername");
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return "";
        }
        URL url = uri.toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; utf-8");
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("Authorization", authToken);

        JSONObject jsonInputString = new JSONObject();
        jsonInputString.put("userID", userID);
        jsonInputString.put("newUsername", newName);

        try(OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonInputString.toString().getBytes("utf-8");
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
                System.out.println(response.toString());
                Log.d("change-name", response.toString());
                return "Success";
            }
        } else {
            try(BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "utf-8"))) {
                StringBuilder response = new StringBuilder();
                String responseLine = null;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                System.out.println("Error: " + response.toString());
            }
        }
        return "Failed";
    }

    public static void sendPOST_changePassword(String userID, String oldPassword, String newPassword, OnHttpCallback callback) {
        try {
            String feedBack = performChangePassword(userID, oldPassword, newPassword); // 假设这是获取到的 userID
            callback.onSuccess(feedBack);
        } catch (Exception e) {
            callback.onFailure(e);
        }
    }

    private static String performChangePassword(String userID, String oldPassword, String newPassword) throws IOException, JSONException {
        Log.d("change-pwd", oldPassword);
        URI uri = null;
        try {
            uri = new URI( uri_s + "changePassword");
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return "";
        }
        URL url = uri.toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; utf-8");
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("Authorization", authToken);

        JSONObject jsonInputString = new JSONObject();
        jsonInputString.put("userID", userID);
        jsonInputString.put("oldPassword", oldPassword);
        jsonInputString.put("newPassword", newPassword);

        try(OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonInputString.toString().getBytes("utf-8");
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
                System.out.println(response.toString());
                return "Success";
            }
        } else {
            try(BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "utf-8"))) {
                StringBuilder response = new StringBuilder();
                String responseLine = null;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                System.out.println("Error: " + response.toString());
            }
        }
        return "Failed";
    }

    public static void sendPOST_changePersonalSignature(String userID, String newSignature, OnHttpCallback callback) {
        try {
            String feedBack = performChangeSignature(userID, newSignature); // 假设这是获取到的 userID
            callback.onSuccess(feedBack);
        } catch (Exception e) {
            callback.onFailure(e);
        }
    }

    private static String performChangeSignature(String userID, String newSignature) throws IOException, JSONException {
        URI uri = null;
        try {
            uri = new URI( uri_s + "changePersonalSignature");
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return "";
        }
        URL url = uri.toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; utf-8");
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("Authorization", authToken);

        JSONObject jsonInputString = new JSONObject();
        jsonInputString.put("userID", userID);
        jsonInputString.put("newPersonalSignature", newSignature);

        try(OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonInputString.toString().getBytes("utf-8");
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
                System.out.println(response.toString());
                return "Success";
            }
        } else {
            try(BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "utf-8"))) {
                StringBuilder response = new StringBuilder();
                String responseLine = null;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                System.out.println("Error: " + response.toString());
            }
        }
        return "Failed";
    }

    public static void changeAvatar(ImageView avatar) throws IOException, JSONException {
        URI uri = null;
        try {
            uri = new URI(uri_s);
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

    public void selectIcon(View view) {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, SELECT_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        iconView = findViewById(R.id.select_icon);
        if (requestCode == SELECT_IMAGE_REQUEST && resultCode == RESULT_OK) {
            if (data != null && data.getData() != null) {
                iconView.setImageURI(data.getData());
                this.iconUrl = data.getData();
            }
        }
    }
}
