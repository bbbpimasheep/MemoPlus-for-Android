package com.example.memo;

import static androidx.core.content.PackageManagerCompat.LOG_TAG;

import static com.example.memo.MainActivity.getCSRFToken;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class Login extends AppCompatActivity{
    private AppDatabase db;
    static String authToken, username;
    ImageButton back2home;
    Button go2regist, login;
    EditText userID, password;
    boolean logined = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        this.back2home = findViewById(R.id.back_button);
        this.go2regist = findViewById(R.id.register_button);
        this.login = findViewById(R.id.login_button);
        this.userID = findViewById(R.id.enter_id);
        this.password =findViewById(R.id.enter_password);

        back2home.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("RestrictedApi")
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Login.this, MainActivity.class);
                intent.putExtra("login", logined);
                startActivity(intent);
            }
        });

        go2regist.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("RestrictedApi")
            @Override
            public void onClick(View v) {
                Log.d(LOG_TAG, "Go to Registration button clicked!");
                Intent intent = new Intent(Login.this, Registration.class);
                startActivity(intent);
            }
        });

        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    sendPOST_login(userID.getText().toString(), password.getText().toString());
                    logined = true;
                } catch (IOException | JSONException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    public static void sendPOST_login(String userID, String password) throws IOException, JSONException {
        URI uri = null;
        try {
            uri = new URI("http://localhost:8000/NotepadServer/login");
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

        JSONObject jsonInputString = new JSONObject();
        jsonInputString.put("userID", userID);
        jsonInputString.put("password", password);

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
                JSONObject jsonResponse = new JSONObject(response.toString());
                authToken = jsonResponse.getString("token");
                username = jsonResponse.getString("username");
                // 检查并获取personalSignature
                String personalSignature;
                if (jsonResponse.isNull("personalSignature")) {
                    personalSignature = "";  // 或者其他默认值
                } else {
                    personalSignature = jsonResponse.getString("personalSignature");
                }

                // 检查并获取noteList
                JSONArray noteList;
                if (jsonResponse.isNull("noteList")) {
                    noteList = new JSONArray();  // 或者其他默认值
                } else {
                    noteList = jsonResponse.getJSONArray("noteList");
                }

                System.out.println("Note List: " + noteList.toString());
                System.out.println("Token: " + authToken);
                System.out.println("Username: " + username);
                System.out.println("Personal Signature: " + personalSignature);
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
}
