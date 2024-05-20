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
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    @SuppressLint("StaticFieldLeak")
    private static MainActivity instance;

    // 单例模式获取MainActivity实例
    public static MainActivity getInstance() {
        return instance;
    }
    RecyclerView memoRecyclerView;
    MemoAdapter adapter;
    TextView bottomSum;
    ImageButton homeButton;
    boolean login = false;

    public static String uri_s = "http://localhost:8000/NotepadServer/register";

    @SuppressLint({"SetTextI18n", "RestrictedApi"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.memoRecyclerView = findViewById(R.id.recycler_view);
        this.bottomSum = findViewById(R.id.bottom_bar);
        this.homeButton = findViewById(R.id.home_button);

        Intent getIntent = getIntent();
        this.login = getIntent.getBooleanExtra("login", false);

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

        List<MemoItem> MemoList = new ArrayList<>();
        MemoItem intro = new MemoItem();
        intro.title = "Intro: Start your own Memo+";
        intro.memo_abstract = "Try our new functions from the advancing AI...";
        intro.edit_time = "大明崇禎十七年五月一日";
        MemoList.add(intro);
        for (int i = 1; i < 10; i++) {
            MemoItem item = new MemoItem();
            item.title = "New Title " + i;
            item.memo_abstract = "Default abstract " + i;
            item.edit_time = "default time " + i;
            MemoList.add(item);
        }
        bottomSum.setText("Total: " + MemoList.size() + " memos");
        this.adapter = new MemoAdapter(MainActivity.this, MemoList);
        memoRecyclerView.setAdapter(adapter);
        LinearLayoutManager layoutManager = new LinearLayoutManager(MainActivity.this);
        memoRecyclerView.setLayoutManager(layoutManager);
    }

    public static String getCSRFToken() throws IOException {
        URI uri = null;
        try {
            uri = new URI("http://localhost:8000/NotepadServer/get_csrf_token");
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return null;
        }
        URL url = uri.toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");

        int responseCode = conn.getResponseCode();
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
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new IOException("Failed to get CSRF token: HTTP error code : " + responseCode);
        }
    }

    class MemoAdapter extends RecyclerView.Adapter<MemoAdapter.MemoViewHolder> {
        LayoutInflater inflater;
        List<MemoItem> MemoList;
        Context context;

        public MemoAdapter(Context context, List<MemoItem> memoItem) {
            this.inflater = LayoutInflater.from(context);
            this.MemoList = memoItem;
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