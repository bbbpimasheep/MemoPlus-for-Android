package com.example.memo;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
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

import java.util.ArrayList;
import java.util.List;

public class Tags extends AppCompatActivity {
    RecyclerView tagRecyclerView;
    ImageButton addTag, backButton;
    EditText inputTag;
    TextView showTag;
    List<String> tagList;
    TagAdapter adapter;
    String memoTitle, memoTime;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.select_tag);

        Intent intent = getIntent();
        this.memoTitle = intent.getStringExtra("TITLE");
        this.memoTime = intent.getStringExtra("TIME");

        this.backButton = findViewById(R.id.back_button);
        this.addTag = findViewById(R.id.add_tag_button);
        this.inputTag = findViewById(R.id.input_tag_bar);
        this.showTag = findViewById(R.id.show_tag_bar);
        this.tagRecyclerView = findViewById(R.id.recycler_view);

        tagList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            tagList.add("Tag " + i);
        }
        setAdapter();

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Tags.this, MemoContent.class);
                intent.putExtra("TITLE", memoTitle);
                intent.putExtra("TIME", memoTime);
                startActivity(intent);
            }
        });

        addTag.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!inputTag.getText().toString().isEmpty()) {
                    tagList.add(inputTag.getText().toString());
                    setAdapter();
                    inputTag.setText("");
                }
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

            holder.tagDisplay.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    /*
                    Intent intent = new Intent(context, MemoContent.class);
                    intent.putExtra("TITLE", holder.titleView.getText().toString());
                    intent.putExtra("TIME", holder.timeView.getText().toString());
                    context.startActivity(intent);
                    */
                    // setTag();
                    showTag.setText("Tag: " + holder.tagDisplay.getText().toString());
                }
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

