package com.example.memo;

import static androidx.core.content.PackageManagerCompat.LOG_TAG;

import android.Manifest;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MemoContent extends AppCompatActivity{
    static final int SELECT_IMAGE_REQUEST = 1;
    static final int TAKE_PHOTO_REQUEST = 2;
    static final int REQUEST_CAMERA_PERMISSION = 3;
    ImageButton back2home, picture, audio, camera, recoder;
    EditText title, time;
    MultiTypeAdapter adapter;
    List<RecyclerViewItem> items;
    File imagesDir;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.memo_content);

        this.back2home = findViewById(R.id.back_button);
        this.picture = findViewById(R.id.picture_button);
        this.camera = findViewById(R.id.camera_button);
        this.title = findViewById(R.id.memo_title);
        this.imagesDir = new File(MemoContent.this.getFilesDir(), "memopics");
        if (!imagesDir.exists()) {
            // 创建文件夹
            boolean isDirCreated = imagesDir.mkdir();
            if (isDirCreated) {
                Log.d("Directory", "Created Successfully");
            } else {
                Log.d("Directory", "Already Exists");
            }
        }

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

        camera.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("QueryPermissionsNeeded")
            @Override
            public void onClick(View v) {
                Log.d("camera", "heard");
                openCamera();
            }
        });

        addItem(new TextItem("This is a text item"));
        // addItem(new AudioItem("path_to_audio_file"));
    }

    private void addItem(RecyclerViewItem item) { adapter.addItem(item); }

    public void selectIcon(View view) {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, SELECT_IMAGE_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            }
        }
    }

    @SuppressLint("QueryPermissionsNeeded")
    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (ContextCompat.checkSelfPermission(MemoContent.this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d("camera", "no permission");
            ActivityCompat.requestPermissions(MemoContent.this, new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION);
        } else {
            // Permission already granted, proceed with camera-related task
            startActivityForResult(takePictureIntent, TAKE_PHOTO_REQUEST);
        }
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
                addItem(new TextItem("This is a text item"));
                adapter.notifyDataSetChanged();
            } else if (requestCode == TAKE_PHOTO_REQUEST) {
                assert data != null;
                Bundle extras = data.getExtras();
                assert extras != null;
                Bitmap imageBitmap = (Bitmap) extras.get("data");
                try {
                    assert imageBitmap != null;
                    String path = imagesDir.getAbsolutePath();
                    saveImageToStorage(imageBitmap, path);
                    Uri imageUri = Uri.fromFile(createImageFile(path));
                    Log.d("save", String.valueOf(imageUri));
                    addItem(new ImageItem(imageUri));
                    addItem(new TextItem("This is a text item"));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private void saveImageToStorage(Bitmap bitmapImage, String photoDirectory) throws IOException {
        File photoFile = createImageFile(photoDirectory);
        try (FileOutputStream fos = new FileOutputStream(photoFile)) {
            bitmapImage.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private File createImageFile(String photoDirectory) throws IOException {
        Log.d("image", "filepath");
        // 创建图片文件名
        @SuppressLint("SimpleDateFormat")
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String path = photoDirectory + "/" + title.getText().toString() + "-" + timeStamp + ".jpg";
        return new File(path);
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
                Bitmap scaledBitmap = scaleBitmapToFitImageView(bitmap, imageView);
                imageView.setImageBitmap(scaledBitmap);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        private Bitmap scaleBitmapToFitImageView(Bitmap bitmap, ImageView imageView) {
            Log.d("image", "scaling");
            // 计算缩放比例
            float scale = ((float) 720) / bitmap.getWidth();

            // 创建一个新的位图并按比例缩放
            int scaledWidth = Math.round(bitmap.getWidth() * scale);
            int scaledHeight = Math.round(bitmap.getHeight() * scale);

            imageView.getLayoutParams().width = scaledWidth;
            imageView.getLayoutParams().height = scaledHeight;
            imageView.requestLayout();

            return Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true);
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
        private final List<RecyclerViewItem> items;
        private final Context context;

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
