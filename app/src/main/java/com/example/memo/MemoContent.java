package com.example.memo;

import static androidx.core.content.PackageManagerCompat.LOG_TAG;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class MemoContent extends AppCompatActivity{
    static final int SELECT_IMAGE_REQUEST = 1;
    ImageButton back2home, picture, audio, camera, recoder;
    EditText title, time;
    MultiTypeAdapter adapter;
    List<RecyclerViewItem> items;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.memo_content);

        this.back2home = findViewById(R.id.back_button);
        this.picture = findViewById(R.id.picture_button);
        this.camera = findViewById(R.id.camera_button);
        this.title = findViewById(R.id.memo_title);

        back2home.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("RestrictedApi")
            @Override
            public void onClick(View v) {
                Log.d(LOG_TAG, "Back button clicked!");
                Intent intent = new Intent(MemoContent.this, MainActivity.class);
                startActivity(intent);
            }
        });

        Intent intent = getIntent();
        String memoTitle = intent.getStringExtra("TITLE");
        title.setText(memoTitle);
        String memoTime = intent.getStringExtra("TIME");

        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        this.items = new ArrayList<>();
        this.adapter = new MultiTypeAdapter(items, MemoContent.this);
        recyclerView.setAdapter(adapter);

        picture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectIcon(v);
            }
        });
        /*
        camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
        /*
         */
        addItem(new TextItem("This is a text item"));
        // addItem(new AudioItem("path_to_audio_file"));
    }

    private void addItem(RecyclerViewItem item) {
        adapter.addItem(item);
    }

    public void selectIcon(View view) {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, SELECT_IMAGE_REQUEST);
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == SELECT_IMAGE_REQUEST) {
                assert data != null;
                Uri selectedImageUri = data.getData();
                addItem(new ImageItem(selectedImageUri));
                adapter.notifyDataSetChanged();
            }
        }
    }

    public abstract static class RecyclerViewItem {
        public static final int TYPE_TEXT = 1;
        public static final int TYPE_IMAGE = 2;
        public static final int TYPE_AUDIO = 3;

        private final int type;

        public RecyclerViewItem(int type) {
            this.type = type;
        }

        public int getType() {
            return type;
        }
    }

    public static class TextItem extends RecyclerViewItem {
        private final String text;

        public TextItem(String text) {
            super(TYPE_TEXT);
            this.text = text;
        }

        public String getText() {
            return text;
        }
    }

    public static class ImageItem extends RecyclerViewItem {
        private final Uri imageUri;

        public ImageItem(Uri imageUri) {
            super(TYPE_IMAGE);
            this.imageUri = imageUri;
        }

        public Uri getImageUri() {
            return imageUri;
        }
    }

    public static class AudioItem extends RecyclerViewItem {
        private final String audioUri;

        public AudioItem(String audioUri) {
            super(TYPE_AUDIO);
            this.audioUri = audioUri;
        }

        public String getAudioUri() {
            return audioUri;
        }
    }

    public static class TextViewHolder extends RecyclerView.ViewHolder {
        EditText editText;

        public TextViewHolder(View itemView) {
            super(itemView);
            editText = itemView.findViewById(R.id.edit_text);
        }

        public void bind(TextItem textItem) {
            editText.setText(textItem.getText());
        }
    }

    public static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public ImageViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.image_view);
        }

        public void bind(ImageItem imageItem, Context context) {
            try {
                InputStream inputStream = context.getContentResolver().openInputStream(imageItem.getImageUri());
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                imageView.setImageBitmap(bitmap);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    public static class AudioViewHolder extends RecyclerView.ViewHolder {
        Button playButton;
        MediaPlayer mediaPlayer;

        public AudioViewHolder(View itemView) {
            super(itemView);
            playButton = itemView.findViewById(R.id.play_button);
            mediaPlayer = new MediaPlayer();
        }

        public void bind(AudioItem audioItem) {
            playButton.setOnClickListener(v -> {
                try {
                    mediaPlayer.reset();
                    mediaPlayer.setDataSource(audioItem.getAudioUri());
                    mediaPlayer.prepare();
                    mediaPlayer.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    public static class MultiTypeAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private List<RecyclerViewItem> items;
        private Context context;

        public MultiTypeAdapter(List<RecyclerViewItem> items, Context context) {
            this.items = items;
            this.context = context;
        }

        @Override
        public int getItemViewType(int position) {
            return items.get(position).getType();
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == RecyclerViewItem.TYPE_TEXT) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_text, parent, false);
                return new TextViewHolder(view);
            } else if (viewType == RecyclerViewItem.TYPE_IMAGE) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_image, parent, false);
                return new ImageViewHolder(view);
            } else if (viewType == RecyclerViewItem.TYPE_AUDIO) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_audio, parent, false);
                return new AudioViewHolder(view);
            }
            return null;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            int viewType = getItemViewType(position);
            if (viewType == RecyclerViewItem.TYPE_TEXT) {
                ((TextViewHolder) holder).bind((TextItem) items.get(position));
            } else if (viewType == RecyclerViewItem.TYPE_IMAGE) {
                ((ImageViewHolder) holder).bind((ImageItem) items.get(position), context);
            } else if (viewType == RecyclerViewItem.TYPE_AUDIO) {
                ((AudioViewHolder) holder).bind((AudioItem) items.get(position));
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        public void addItem(RecyclerViewItem item) {
            items.add(item);
            notifyItemInserted(items.size() - 1);
        }
    }

}
