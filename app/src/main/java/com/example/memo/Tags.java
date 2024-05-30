package com.example.memo;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Tags extends AppCompatActivity {
    private static AppDatabase db;
    ExecutorService executorService;
    RecyclerView tagRecyclerView;
    ImageButton setTag, backButton;
    EditText inputTag;
    TextView showTag;
    List<String> tagList;
    TagAdapter adapter;
    String memoTitle, memoTime, memoType;
    private NoteDao noteDao;
    int noteID;
    @SuppressLint("SetTextI18n")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.select_tag);

        db = MemoPlus.getInstance().getAppDatabase();
        noteDao = db.noteDao();

        Intent intent = getIntent();
        this.memoTitle = intent.getStringExtra("TITLE");
        this.memoTime = intent.getStringExtra("TIME");
        this.noteID = intent.getIntExtra("ID", 0);
        this.memoType = intent.getStringExtra("TYPE");

        this.backButton = findViewById(R.id.back_button);
        this.setTag = findViewById(R.id.set_tag_button);
        this.inputTag = findViewById(R.id.input_tag_bar);
        this.showTag = findViewById(R.id.show_tag_bar);
        this.tagRecyclerView = findViewById(R.id.recycler_view);

        this.executorService = Executors.newFixedThreadPool(1);

        showTag.setText("Tag: " + memoType);
        this.tagList = new ArrayList<>();
        tagList.add("Study");
        tagList.add("Refreshment");
        tagList.add("Travel");
        tagList.add("Food & Drink");
        tagList.add("Family");
        setAdapter();

        backButton.setOnClickListener(v -> {
            /*
            Intent intent1 = new Intent(Tags.this, MemoContent.class);
            intent1.putExtra("TITLE", memoTitle);
            intent1.putExtra("TIME", memoTime);
            intent1.putExtra("ID", noteID);
            setResult(RESULT_OK, intent);
             */
            finish();
        });

        setTag.setOnClickListener(v -> {
            if (!inputTag.getText().toString().isEmpty()) {
                executorService.submit(() -> {
                    Note note = noteDao.getNoteByID(noteID);
                    note.type = inputTag.getText().toString();
                    noteDao.updateNote(note);
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(() -> {
                        memoType = inputTag.getText().toString();
                        showTag.setText("Tag: " + inputTag.getText().toString());
                    });
                });
            }
        });
    }


    public void setAdapter() {
        this.adapter = new Tags.TagAdapter(Tags.this, this.tagList);
        tagRecyclerView.setAdapter(adapter);
        LinearLayoutManager layoutManager = new LinearLayoutManager(Tags.this);
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
        public TagViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = inflater.inflate(R.layout.tag_item, parent, false);
            return new TagViewHolder(view);
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void onBindViewHolder(@NonNull TagViewHolder holder, @SuppressLint("RecyclerView") int position) {
            String tag = tagList.get(position);
            Log.d("tags", tag);
            holder.tagDisplay.setText(tag);

            holder.tagDisplay.setOnClickListener(v -> {
                inputTag.setText(holder.tagDisplay.getText().toString());
            });

            holder.deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    tagList.remove(position);
                    setAdapter();
                }
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

