package com.example.memo;

import static androidx.core.content.PackageManagerCompat.LOG_TAG;

import static com.example.memo.MainActivity.uri_s;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Login extends AppCompatActivity{
    private static AppDatabase db;
    private UserDao userDao;
    private NoteDao noteDao;
    ImageButton back2home;
    Button go2regist, login;
    EditText userID, password;
    boolean logined = false;
    static ExecutorService executorService;
    String authtoken, username, signature;
    JSONArray noteList;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        db = MemoPlus.getInstance().getAppDatabase();
        userDao = db.userDao();
        noteDao = db.noteDao();

        this.back2home = findViewById(R.id.back_button);
        this.go2regist = findViewById(R.id.register_button);
        this.login = findViewById(R.id.login_button);
        this.userID = findViewById(R.id.enter_id);
        this.password =findViewById(R.id.enter_password);

        executorService = Executors.newFixedThreadPool(1);

        back2home.setOnClickListener(v -> {
            Intent intent = new Intent(Login.this, MainActivity.class);
            intent.putExtra("login", logined);
            Log.d("login", String.valueOf(logined));
            setResult(RESULT_OK, intent);
            finish();
        });

        go2regist.setOnClickListener(v -> {
            Log.d("register", "Go to Registration button clicked!");
            Intent intent = new Intent(Login.this, Registration.class);
            startActivity(intent);
        });

        login.setOnClickListener(v -> {
            executorService.submit(() -> {
                sendPOST_login(userID.getText().toString(), password.getText().toString(), new OnHttpCallback(){
                    @Override
                    public void onSuccess(String feedBack) {
                        logined = true;
                        authtoken = feedBack;
                        if (!Objects.equals(authtoken, "Error")) {
                            updateUser();
                            updateNoteDB();
                            logined = true;
                            Handler handler = new Handler(Looper.getMainLooper());
                            handler.post(() -> {Toast.makeText(Login.this, "Login success", Toast.LENGTH_SHORT).show();});
                        } else {
                            Handler handler = new Handler(Looper.getMainLooper());
                            handler.post(() -> {Toast.makeText(Login.this, "Login failed. Please retry.", Toast.LENGTH_SHORT).show();});
                        }
                    }
                    @Override
                    public void onFailure(Exception e) {
                        e.printStackTrace();
                        Handler handler = new Handler(Looper.getMainLooper());
                        handler.post(() -> {Toast.makeText(Login.this, "Login failed. Please retry.", Toast.LENGTH_SHORT).show();});
                    }
                });
            });
        });
    }

    private void updateNoteDB() {
        noteDao.deleteAllNotes();
        Log.d("note-list", String.valueOf(noteList));
        for (int i = 0; i < noteList.length(); i++) {
            JSONObject noteC;
            try {noteC = noteList.getJSONObject(i);}
            catch (JSONException e) {throw new RuntimeException(e);}
            Note noteL = new Note();
            noteL.last_save = "Cloud";
            try {
                noteL.id = noteC.getInt("demosticId");
                noteL.title = noteC.getString("title");
                noteL.type = noteC.getString("type");
                JSONArray fileJson = noteC.getJSONArray("file");

                List<String> fileList = new ArrayList<>();
                for(int j = 0; j < fileJson.length(); j++) {
                    fileList.add(fileJson.getJSONObject(j).toString());
                }
                noteL.files = fileList;
                noteL.last_edit = "";
                noteDao.insertNote(noteL);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void updateUser() {
        userDao.deleteAllUsers();
        User syujin = new User();
        syujin.userID = userID.getText().toString();
        syujin.password = password.getText().toString();
        syujin.token = authtoken;
        syujin.username = username;
        syujin.signature = signature;
        syujin.avatar = "Null";
        userDao.insertUser(syujin);
    }

    public void sendPOST_login(String userID, String password, OnHttpCallback callback) {
        try {
            String token = performLoginRequest(userID, password); // 假设这是获取到的 userID
            callback.onSuccess(token);
        } catch (Exception e) {
            callback.onFailure(e);
        }
    }

    private String performLoginRequest(String userID, String password) throws IOException, JSONException {
        URI uri = null;
        try {
            uri = new URI(uri_s + "login");
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

        JSONObject jsonInputString = new JSONObject();
        jsonInputString.put("userID", userID);
        jsonInputString.put("password", password);

        try(OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonInputString.toString().getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        String _authToken = "";
        int responseCode = conn.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            try(BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"))) {
                StringBuilder response = new StringBuilder();
                String responseLine = null;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                JSONObject jsonResponse = new JSONObject(response.toString());

                // 获取 token 和对应的用户名
                _authToken = jsonResponse.getString("token");
                String _username = jsonResponse.getString("username");
                if (!_username.equals("")) {this.username = _username;}

                // 检查并获取personalSignature
                String personalSignature;
                if (jsonResponse.isNull("personalSignature")) {personalSignature = "";} // 或者其他默认值
                else {personalSignature = jsonResponse.getString("personalSignature");}
                this.signature = personalSignature;

                // 检查并获取noteList
                JSONArray _noteList;
                if (jsonResponse.isNull("noteList")) {_noteList = new JSONArray();} // 或者其他默认值
                else {_noteList = jsonResponse.getJSONArray("noteList");}
                this.noteList = _noteList;

                return _authToken;
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
        return "Error";
    }
}
