package com.example.memo;

import static androidx.core.content.PackageManagerCompat.LOG_TAG;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.LayoutInflater;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    AppDatabase db;
    private UserDao userDao;
    private NoteDao noteDao;
    private static String authToken;
    RecyclerView memoRecyclerView;
    MemoAdapter adapter;
    TextView bottomSum;
    ImageButton homeButton, addMemo, aiButton;
    boolean login = true; // false
    List<MemoItem> MemoList;
    ExecutorService executorService;
    int maxID = -1;

    public static String uri_s = "http://android.xulincaigou.online:8000/NotepadServer/";

    @SuppressLint({"SetTextI18n", "RestrictedApi", "NotifyDataSetChanged"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = MemoPlus.getInstance().getAppDatabase();
        userDao = db.userDao();
        noteDao = db.noteDao();

        this.memoRecyclerView = findViewById(R.id.recycler_view);
        this.bottomSum = findViewById(R.id.bottom_bar);
        this.homeButton = findViewById(R.id.home_button);
        this.addMemo = findViewById(R.id.add_memo_button);
        this.aiButton = findViewById(R.id.ai_button);

        this.MemoList = new ArrayList<>();

        Intent getIntent = getIntent();
        this.login = getIntent.getBooleanExtra("login", true); // false

        // new Thread(() -> {noteDao.deleteAllNotes();}).start();

        aiButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, Chat.class);;
                startActivity(intent);
            }
        });

        homeButton.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("RestrictedApi")
            @Override
            public void onClick(View v) {
                Log.d(LOG_TAG, "Home button clicked!");
                // Intent intent = new Intent(MainActivity.this, PersonalProfile.class);;
                Intent intent;
                if(login) intent = new Intent(MainActivity.this, PersonalProfile.class);
                else intent = new Intent(MainActivity.this, Login.class);
                startActivity(intent);
            }
        });

        addMemo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ExecutorService localExeService = Executors.newFixedThreadPool(1);
                localExeService.submit(() -> {
                    Note newNote = new Note();
                    maxID += 1;
                    newNote.title = "New Title " + maxID;
                    newNote.id = maxID;
                    Log.d("title-id-new", String.valueOf(maxID));
                    newNote.type = "Not chosen yet";
                    String timeStamp = new SimpleDateFormat("MM.dd HH:mm").format(new Date());
                    newNote.last_edit = "Newly created at " + timeStamp;
                    newNote.last_save = "";
                    newNote.files = new ArrayList<>();
                    String content = "{\"content\": \"Type here.\"," +
                            "\"type\": \"text\"}";
                    newNote.files.add(content);
                    noteDao.insertNote(newNote);
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(() -> {
                        MemoItem item = new MemoItem();
                        item.title = newNote.title;
                        item.memo_abstract = "What's going on?";
                        item.edit_time = newNote.last_edit;
                        MemoList.add(item);
                        bottomSum.setText("Total: " + MemoList.size() + " memos");
                        setAdapter();
                    });
                });
            }
        });
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    protected void onResume() {
        super.onResume();
        this.executorService = Executors.newFixedThreadPool(1);
        if (login) {
            executorService.submit(() -> {
                List<User> users = db.userDao().getAllUsers();
                if (!users.isEmpty()) authToken = users.get(0).userID;
                List<Note> notes = noteDao.getAllNotes();
                if (notes.isEmpty()) {
                    Log.d("notes", "files list is empty");
                    initializeMain();
                }
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(() -> {
                    MemoList.clear();
                    for(Note note: notes) {
                        if (note.id > maxID) {
                            maxID = note.id;
                        }
                        Log.d("title-id-main", String.valueOf(maxID));
                        MemoItem item = new MemoItem();
                        item.title = note.title;
                        Log.d("title", note.title);
                        item.edit_time = note.last_edit;
                        String abs = "";
                        try {
                            if (!note.files.isEmpty()) {
                                JSONObject jsonAbs = new JSONObject(note.files.get(0));
                                abs = jsonAbs.getString("content");
                            }
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                        if (abs.length() > 36) {
                            abs = abs.substring(0,36);
                        }
                        item.memo_abstract = abs + "...";
                        item.type = note.type;
                        MemoList.add(item);
                        Log.d("title", String.valueOf(MemoList.size()));
                    }
                    adapter.notifyDataSetChanged();
                    setAdapter();
                });
            });
        }
        bottomSum.setText("Total: " + MemoList.size() + " memos");
        setAdapter();
    }

    public void initializeMain() {
        executorService.submit(() -> {
            // 插入Note的逻辑
            Note newNote = new Note();
            // 设置Note的属性
            noteDao.insertNote(newNote);

            // 插入完成后更新UI
            runOnUiThread(() -> {
                // 更新UI逻辑
            });
        });
        Note Intro = new Note();
        Intro.title = "Intro: Start your own Memo+";
        String timeStamp = new SimpleDateFormat("MM.dd HH:mm").format(new Date());
        Intro.last_edit = "Newly created at " + timeStamp;
        Intro.last_save = "";
        Intro.type = "Introduction";
        Intro.id = 0;
        String content = "{\"content\": \"Try our new functions from the advancing AI. Basic functions can be adopted by pressing buttons below.\"," +
                "\"type\": \"text\"}";
        Log.d("notes", content);
        Intro.files = new ArrayList<>();
        Intro.files.add(content);
        noteDao.insertNote(Intro);
    }

    public void setAdapter() {
        this.adapter = new MemoAdapter(MainActivity.this, this.MemoList);
        memoRecyclerView.setAdapter(adapter);
        LinearLayoutManager layoutManager = new LinearLayoutManager(MainActivity.this);
        memoRecyclerView.setLayoutManager(layoutManager);
    }

    public static String getCSRFToken() throws IOException, JSONException {
        URI uri = null;
        try {
            uri = new URI(uri_s + "get_csrf_token");
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return null;
        }
        URL url = uri.toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");

        int responseCode = conn.getResponseCode();
        Log.d("csrf", String.valueOf(responseCode));
        if (responseCode == HttpURLConnection.HTTP_OK) {
            try(BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"))) {
                StringBuilder response = new StringBuilder();
                String responseLine = null;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                JSONObject jsonResponse = new JSONObject(response.toString());
                String csrfToken = jsonResponse.getString("csrf_token");
                System.out.println("CSRF Token: " + csrfToken);
                return csrfToken;
            }
        } else {
            throw new IOException("Failed to get CSRF token: HTTP error code : " + responseCode);
        }
    }

    class MemoAdapter extends RecyclerView.Adapter<MemoAdapter.MemoViewHolder> {
        LayoutInflater inflater;
        List<MemoItem> MemoList;
        Context context;

        public MemoAdapter(Context context, List<MemoItem> memoItems) {
            this.inflater = LayoutInflater.from(context);
            this.MemoList = memoItems;
            this.context = context;
        }
        @NonNull
        @Override
        public MemoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = inflater.inflate(R.layout.memo_item, parent, false);
            return new MemoViewHolder(view);
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void onBindViewHolder(@NonNull MemoViewHolder holder, int position) {
            MemoItem item = MemoList.get(position);
            holder.titleView.setText(item.title);
            holder.abstractView.setText(item.memo_abstract);
            holder.timeView.setText("Last edit: " + item.edit_time);
            holder.type.setText(item.type);
            holder.background.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(context, MemoContent.class);
                    intent.putExtra("TITLE", holder.titleView.getText().toString());
                    intent.putExtra("TIME", holder.timeView.getText().toString());
                    context.startActivity(intent);
                }
            });
        }

        @Override
        public int getItemCount() {
            return MemoList.size();
        }

        class MemoViewHolder extends RecyclerView.ViewHolder {
            TextView titleView;
            TextView abstractView;
            TextView timeView;
            TextView bottomBar;
            ImageView typeIcon;
            TextView type;
            TextView background;

            public MemoViewHolder(@NonNull View itemView) {
                super(itemView);
                titleView = itemView.findViewById(R.id.memo_title);
                abstractView = itemView.findViewById(R.id.memo_abstract);
                timeView = itemView.findViewById(R.id.edit_time);
                bottomBar = itemView.findViewById(R.id.item_bottom_line);
                typeIcon = itemView.findViewById(R.id.type_icon);
                type = itemView.findViewById(R.id.type);
                background = itemView.findViewById(R.id.item_background);
            }
        }
    }
}