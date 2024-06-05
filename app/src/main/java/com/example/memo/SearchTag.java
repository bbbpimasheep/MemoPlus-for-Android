package com.example.memo;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SearchTag extends AppCompatActivity {
    AppDatabase db;
    NoteDao noteDao;
    ImageButton backButton;
    TagAdapter adapter;
    RecyclerView tagRecyclerView;
    List<String> tagList;
    static ExecutorService executorService;

    @SuppressLint("SetTextI18n")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search_tag);

        this.db = MemoPlus.getInstance().getAppDatabase();
        this.noteDao = db.noteDao();
        executorService = Executors.newFixedThreadPool(1);

        this.backButton = findViewById(R.id.back_button);
        this.tagRecyclerView = findViewById(R.id.recycler_view);
        this.tagList = new ArrayList<>();

        backButton.setOnClickListener(v -> {
            finish();
        });
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    protected void onResume() {
        super.onResume();
        executorService.submit(() -> {
            List<Note> notes = noteDao.getAllNotes();
            tagList.clear();
            for(Note note : notes) {
                String noteType = note.type;
                if (!Objects.equals(noteType, "Default Type") && !tagList.contains(noteType)){
                    tagList.add(noteType);
                }
            }
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(this::setAdapter);
        });
    }

    public void setAdapter() {
        this.adapter = new TagAdapter(SearchTag.this, this.tagList);
        tagRecyclerView.setAdapter(adapter);
        LinearLayoutManager layoutManager = new LinearLayoutManager(SearchTag.this);
        tagRecyclerView.setLayoutManager(layoutManager);
    }

    class TagAdapter extends RecyclerView.Adapter<TagAdapter.TagViewHolder> {
        LayoutInflater inflater;
        List<String> tagList;
        Context context;

        public TagAdapter(Context context, List<String> tagItems) {
            this.inflater = LayoutInflater.from(context);
            this.tagList = tagItems;
            this.context = context;
        }
        @NonNull
        @Override
        public TagAdapter.TagViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = inflater.inflate(R.layout.tag_item, parent, false);
            return new TagAdapter.TagViewHolder(view);
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void onBindViewHolder(@NonNull TagAdapter.TagViewHolder holder, @SuppressLint("RecyclerView") int position) {
            String tag = tagList.get(position);
            holder.tagDisplay.setText(tag);

            holder.tagDisplay.setOnClickListener(v -> {
                Intent intent = new Intent(SearchTag.this, Search.class);
                intent.putExtra("KEY", holder.tagDisplay.getText().toString());
                startActivity(intent);
            });

            holder.deleteButton.setOnClickListener(v -> {
                tagList.remove(position);
                setAdapter();
            });
        }

        @Override
        public int getItemCount() {
            return tagList.size();
        }

        class TagViewHolder extends RecyclerView.ViewHolder {
            ImageButton deleteButton;
            TextView tagDisplay;

            public TagViewHolder(@NonNull View itemView) {
                super(itemView);
                this.deleteButton = itemView.findViewById(R.id.delete_tag_button);
                this.tagDisplay = itemView.findViewById(R.id.tag_text);
            }
        }
    }
}
