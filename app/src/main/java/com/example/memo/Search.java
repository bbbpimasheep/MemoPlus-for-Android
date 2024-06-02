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
import android.widget.ImageView;
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

public class Search extends AppCompatActivity {
    AppDatabase db;
    private UserDao userDao;
    private NoteDao noteDao;
    ImageButton back2Home;
    TextView showKeyword;
    List<MemoItem> ResList;
    String keyword;
    RecyclerView memoRecyclerView;
    Search.MemoAdapter adapter;
    static ExecutorService executorService;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        db = MemoPlus.getInstance().getAppDatabase();
        userDao = db.userDao();
        noteDao = db.noteDao();
        executorService = Executors.newFixedThreadPool(1);

        this.memoRecyclerView = findViewById(R.id.recycler_view);
        this.back2Home = findViewById(R.id.back_button);
        this.showKeyword = findViewById(R.id.search_naiyo);
        this.ResList = new ArrayList<>();

        Intent intent = getIntent();
        this.keyword = intent.getStringExtra("KEY");
        showKeyword.setText(keyword);

        back2Home.setOnClickListener(v -> {
            Log.d("back", "Back button clicked!");
            finish();
        });
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    protected void onResume() {
        super.onResume();
        selectItems();
    }

    @SuppressLint("NotifyDataSetChanged")
    private void selectItems() {
        executorService.submit(() -> {
            List<Note> allNotes = noteDao.getAllNotes();
            List<Note> thatFits = new ArrayList<>();
            for(Note note : allNotes) {
                if (note.title.contains(keyword) || note.type.contains(keyword)) {
                    thatFits.add(note);
                }
            }
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(() -> {

                ResList.clear();
                for(Note OKnote : thatFits) {
                    MemoItem item = new MemoItem();
                    item.title = OKnote.title;
                    item.edit_time = OKnote.last_edit;
                    String abs = "";
                    try {
                        if (!OKnote.files.isEmpty()) {
                            JSONObject jsonAbs = new JSONObject(OKnote.files.get(0));
                            abs = jsonAbs.getString("content");
                        }
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                    if (abs.length() > 36) {
                        abs = abs.substring(0,36) + "...";
                    }
                    item.memo_abstract = abs;
                    item.type = OKnote.type;
                    item.labelNoteID = OKnote.id;
                    ResList.add(item);
                }
                setAdapter();
                adapter.notifyDataSetChanged();
            });
        });
    }

    public void setAdapter() {
        this.adapter = new Search.MemoAdapter(Search.this, this.ResList);
        memoRecyclerView.setAdapter(adapter);
        LinearLayoutManager layoutManager = new LinearLayoutManager(Search.this);
        memoRecyclerView.setLayoutManager(layoutManager);
    }

    static class MemoAdapter extends RecyclerView.Adapter<Search.MemoAdapter.MemoViewHolder> {
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
            return new  Search.MemoAdapter.MemoViewHolder(view);
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void onBindViewHolder(@NonNull Search.MemoAdapter.MemoViewHolder holder, int position) {
            MemoItem item = MemoList.get(position);
            holder.titleView.setText(item.title);
            holder.abstractView.setText(item.memo_abstract);
            holder.timeView.setText("Last edit: " + item.edit_time);
            holder.type.setText(item.type);
            holder.noteID = item.labelNoteID;
            holder.background.setOnClickListener(v -> {
                Intent intent = new Intent(context, MemoContent.class);
                intent.putExtra("TITLE", holder.titleView.getText().toString());
                intent.putExtra("TIME", holder.timeView.getText().toString());
                intent.putExtra("TAG", holder.type.getText().toString());
                Log.d("type-show", holder.type.getText().toString());
                intent.putExtra("ID", item.labelNoteID);
                context.startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return MemoList.size();
        }

        static class MemoViewHolder extends RecyclerView.ViewHolder {
            TextView titleView;
            TextView abstractView;
            TextView timeView;
            TextView bottomBar;
            ImageView typeIcon;
            TextView type;
            TextView background;
            int noteID = -1;

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
