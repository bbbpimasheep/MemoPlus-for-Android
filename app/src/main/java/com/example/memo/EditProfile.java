package com.example.memo;

import static androidx.core.content.PackageManagerCompat.LOG_TAG;

import static com.example.memo.MainActivity.getCSRFToken;
import static com.example.memo.MainActivity.uri_s;
import static com.example.memo.Registration.getToken;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.content.Intent;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

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

public class EditProfile extends AppCompatActivity {
    private static AppDatabase db;
    static final int SELECT_IMAGE_REQUEST = 1;
    private static String authToken;
    ImageButton cancelButton, selectButton, setNameButton, setPwdButton, setSignButton, setAvatarButton;
    EditText editName, oldPwd, newPwd, editSign;
    TextView IDView;
    CircularImageView iconView;
    String userName, userID, password, signature;
    Parcelable iconUrl;

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

        new Thread(() -> {
            authToken = db.userDao().getToken(userID);
        }).start();

        Intent intent = getIntent();
        userName = intent.getStringExtra("name");
        editName.setText(userName);
        userID = intent.getStringExtra("ID");
        IDView.setText(userID);
        password = intent.getStringExtra("password");
        oldPwd.setText(password);
        signature = intent.getStringExtra("signature");
        editSign.setText(signature);


        cancelButton.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("RestrictedApi")
            @Override
            public void onClick(View v) {
                Log.d(LOG_TAG, "Cancel button clicked!");
                Intent intent = new Intent(EditProfile.this, PersonalProfile.class);
                startActivity(intent);
            }
        });

        setNameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    sendPOST_changeUsername(userID, editName.getText().toString());
                } catch (IOException | JSONException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        setPwdButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    String newPassword = newPwd.getText().toString();
                    if (!password.equals(newPassword)) {
                        sendPOST_changePassword(userID, password, newPassword);
                    } else {
                        newPwd.setError("Same!");
                        newPwd.requestFocus();
                    }
                } catch (IOException | JSONException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        setSignButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    sendPOST_changePersonalSignature(userID, editSign.getText().toString());
                } catch (IOException | JSONException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        setAvatarButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    changeAvatar(iconView);
                } catch (IOException | JSONException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    public static void sendPOST_changeUsername(String userID, String newName) throws IOException, JSONException {
        URI uri = null;
        try {
            uri = new URI( uri_s + "register");
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }
        URL url = uri.toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; utf-8");
        conn.setRequestProperty("Accept", "application/json");

        String csrfToken = getCSRFToken();
        conn.setRequestProperty("X-CSRFToken", csrfToken);
        conn.setRequestProperty("Cookie", "csrftoken=" + csrfToken);
        conn.setDoOutput(true);
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
    }

    public static void sendPOST_changePassword(String userID, String oldPassword, String newPassword) throws IOException, JSONException {
        URI uri = null;
        try {
            uri = new URI( uri_s + "register");
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }
        URL url = uri.toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; utf-8");
        conn.setRequestProperty("Accept", "application/json");

        String csrfToken = getCSRFToken();
        conn.setRequestProperty("X-CSRFToken", csrfToken);
        conn.setRequestProperty("Cookie", "csrftoken=" + csrfToken);
        conn.setDoOutput(true);
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
    }

    public static void sendPOST_changePersonalSignature(String userID, String newSignature) throws IOException, JSONException {
        URI uri = null;
        try {
            uri = new URI( uri_s + "register");
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }
        URL url = uri.toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; utf-8");
        conn.setRequestProperty("Accept", "application/json");

        String csrfToken = getCSRFToken();
        conn.setRequestProperty("X-CSRFToken", csrfToken);
        conn.setRequestProperty("Cookie", "csrftoken=" + csrfToken);
        conn.setDoOutput(true);
        conn.setRequestProperty("Authorization", authToken);

        JSONObject jsonInputString = new JSONObject();
        jsonInputString.put("userID", userID);
        jsonInputString.put("PersonalSignature", newSignature);

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
