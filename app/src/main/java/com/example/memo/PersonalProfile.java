package com.example.memo;

import static androidx.core.content.PackageManagerCompat.LOG_TAG;

import static com.example.memo.MainActivity.uri_s;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PersonalProfile extends AppCompatActivity {
    private static AppDatabase db;
    private UserDao userDao;
    ImageButton backButton, editProfile;
    TextView nameView, IDView, signView;
    String password, avatarPath = "NoPath";
    static String authToken;
    ExecutorService executorService;
    CircularImageView iconView;
    static File dir;
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
        userDao = db.userDao();

        dir = PersonalProfile.this.getFilesDir();
        if (!dir.exists()) {
            // 创建文件夹
            boolean isDirCreated = dir.mkdir();
            if (isDirCreated) {
                Log.d("Directory", "Created Successfully");
            } else {
                Log.d("Directory", "Already Exists");
            }
        }

        executorService = Executors.newFixedThreadPool(1);

        backButton.setOnClickListener(v -> {
            Log.d(LOG_TAG, "Back button clicked!");
            finish();
        });

        editProfile.setOnClickListener(v -> {
            Log.d(LOG_TAG, "Back button clicked!");
            Intent intent = new Intent(PersonalProfile.this, EditProfile.class);
            startActivity(intent);
        });
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    protected void onResume() {
        super.onResume();
        displayInfo();
    }

    private void displayInfo() {
        executorService.submit(() -> {
            User user = db.userDao().getAllUsers().get(0);
            authToken = user.token;
            Handler handler1 = new Handler(Looper.getMainLooper());
            handler1.post(() -> {
                nameView.setText(user.username);
                IDView.setText(user.userID);
                signView.setText(user.signature);
            });
            Log.d("avatar", user.avatar);
            Handler handler2 = new Handler(Looper.getMainLooper());
            if (Objects.equals(user.avatar, "Check")) {
                downloadIcon(user.userID);
                handler2.post(() -> {
                    if (!avatarPath.equals("NoPath")) {
                        bindIcon();
                    }
                });
            } else {
                avatarPath = user.avatar;
                handler2.post(() -> {
                    if (!avatarPath.equals("NoPath")) {
                        bindIcon();
                    }
                });
            }
        });
    }

    private void downloadIcon(String userID) {
        sendGET_getAvatar(userID, new OnHttpCallback(){
            @Override
            public void onSuccess(String feedBack) {
                avatarPath = feedBack;
                User syujin = userDao.getAllUsers().get(0);
                syujin.avatar = avatarPath;
                userDao.updateUser(syujin);
            }
            @Override
            public void onFailure(Exception e) {e.printStackTrace();}
        });
    }

    private void bindIcon() {
        try {
            File imageFile = new File(avatarPath);
            InputStream inputStream = new FileInputStream(imageFile);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            // Bitmap scaledBitmap = scaleBitmapToFitImageView(bitmap, imageView);
            iconView.setImageBitmap(bitmap);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void sendGET_getAvatar(String userID, OnHttpCallback callback) {
        try {
            String path = performGetAvatar(userID); // 假设这是获取到的 userID
            callback.onSuccess(path);
        } catch (Exception e) {
            callback.onFailure(e);
        }
    }

    private static String performGetAvatar(String userID) throws IOException, JSONException {
        URI uri = null;
        try {
            uri = new URI(uri_s + "getAvatar");
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return "NoPath";
        }
        URL url = uri.toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("Authorization", authToken);

        JSONObject jsonInputString = new JSONObject();
        jsonInputString.put("userID", userID);
        conn.setDoOutput(true);
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonInputString.toString().getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        int code = conn.getResponseCode();
        System.out.println(code);
        if (code != 200) {
            System.out.println("Avatar retrieval failed");
            return "NoPAth";
        }

        InputStream is = conn.getInputStream();
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File outputFile = new File(dir, userID + "_" + timeStamp + "_avatar.jpg");
        FileOutputStream fos = new FileOutputStream(outputFile);
        byte[] buffer = new byte[1024];
        int len;
        while ((len = is.read(buffer)) != -1) {
            fos.write(buffer, 0, len);
        }
        fos.close();

        int responseCode = conn.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            try(BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"))) {
                StringBuilder response = new StringBuilder();
                String responseLine = null;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                System.out.println(response.toString());
                return outputFile.getPath();
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
        return "NoPath";
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdownNow();
        }
    }

}
