package com.example.memo;

import static androidx.core.content.PackageManagerCompat.LOG_TAG;

import android.Manifest;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
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
import android.widget.Toast;

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
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MemoContent extends AppCompatActivity{
    static final int SELECT_IMAGE_REQUEST = 1;
    static final int TAKE_PHOTO_REQUEST = 2;
    static final int PICK_AUDIO_REQUEST = 3;
    static final int REQUEST_CAMERA_PERMISSION = 100;
    static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    ImageButton back2home, picture, audio, camera, recorder,
                delete, save, settags;
    EditText title, time;
    MultiTypeAdapter adapter;
    List<RecyclerViewItem> items;
    File dir;
    boolean isRecording = false;
    MediaRecorder Mrecorder = null;
    String audioPath = null;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.memo_content);

        this.title = findViewById(R.id.memo_title);
        this.back2home = findViewById(R.id.back_button);
        this.picture = findViewById(R.id.picture_button);
        this.audio = findViewById(R.id.audio_button);
        this.camera = findViewById(R.id.camera_button);
        this.recorder = findViewById(R.id.microphone_button);
        this.delete = findViewById(R.id.delete_memo_button);
        this.save = findViewById(R.id.save_button);
        this.settags = findViewById(R.id.set_tag_button);

        Intent intent = getIntent();
        String memoTitle = intent.getStringExtra("TITLE");
        title.setText(memoTitle);
        String memoTime = intent.getStringExtra("TIME");

        assert memoTitle != null;
        this.dir = new File(MemoContent.this.getFilesDir(), memoTitle);
        Log.d("Files directory", MemoContent.this.getFilesDir().toString());
        if (!dir.exists()) {
            // 创建文件夹
            boolean isDirCreated = dir.mkdir();
            if (isDirCreated) {
                Log.d("Directory", "Created Successfully");
            } else {
                Log.d("Directory", "Already Exists");
            }
        }

        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // deleteMemo();
                Intent intent = new Intent(MemoContent.this, MainActivity.class);
                startActivity(intent);
            }
        });

        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // saveMemo2Local();
            }
        });

        back2home.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("RestrictedApi")
            @Override
            public void onClick(View v) {
                Log.d("back", "Back button clicked!");
                Intent intent = new Intent(MemoContent.this, MainActivity.class);
                startActivity(intent);
            }
        });

        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        this.items = new ArrayList<>();
        this.adapter = new MultiTypeAdapter(items, MemoContent.this);
        recyclerView.setAdapter(adapter);

        picture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectPic(v);
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

        audio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openAudioFilePicker();
            }
        });

        recorder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("recorder", "heard");
                openRecorder();
            }
        });

        settags.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MemoContent.this, Tags.class);
                intent.putExtra("TITLE", title.getText().toString());
                intent.putExtra("TIME", memoTime);
                startActivity(intent);
            }
        });

        addItem(new TextItem("This is a text item"));
        // addItem(new AudioItem("path_to_audio_file"));
    }

    private void addItem(RecyclerViewItem item) { adapter.addItem(item); }

    public void selectPic(View view) {
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
        } else if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openRecorder();
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

    private void openRecorder() {
        if (isRecording) {
            stopRecording();
            File audioFile = new File(audioPath);
            Uri audioUri = Uri.fromFile(audioFile);
            addItem(new AudioItem(audioUri));
            addItem(new TextItem("This is a text item"));
            recorder.setImageResource(R.drawable.ic_microphone);
        } else {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            this.audioPath = dir.getAbsolutePath() + "/" + "audio-" + title.getText().toString() + "-" + timeStamp + ".3gp";
            startRecording(audioPath);
            recorder.setImageResource(R.drawable.ic_microphone_on);
        }
        isRecording = !isRecording;
    }

    private void startRecording(String path) {
        if (ContextCompat.checkSelfPermission(MemoContent.this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d("audio", "no permission");
            ActivityCompat.requestPermissions(MemoContent.this, new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_RECORD_AUDIO_PERMISSION);
        } else {
            // Permission already granted, proceed with camera-related task
            Mrecorder = new MediaRecorder();
            try {
                Mrecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            } catch (Exception e) {
                Log.e("MemoContent", "setAudioSource failed: " + e.getMessage());
                return;
            }
            Mrecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            Mrecorder.setOutputFile(path);
            Mrecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

            try {
                Mrecorder.prepare();
            } catch (IOException e) {
                Toast.makeText(this, "Recording failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }

            Mrecorder.start();
            Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopRecording() {
        Mrecorder.stop();
        Mrecorder.release();
        Mrecorder = null;
        Toast.makeText(this, "Recording stopped", Toast.LENGTH_SHORT).show();
    }

    private void openAudioFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("audio/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, PICK_AUDIO_REQUEST);
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
                saveImageToDirectory(selectedImageUri, dir);
                adapter.notifyDataSetChanged();
            } else if (requestCode == TAKE_PHOTO_REQUEST) {
                assert data != null;
                Bundle extras = data.getExtras();
                assert extras != null;
                Bitmap imageBitmap = (Bitmap) extras.get("data");
                try {
                    assert imageBitmap != null;
                    String path = dir.getAbsolutePath();
                    Uri imageUri = Uri.fromFile(createImageFile(path));
                    saveImageToDirectory(imageUri, dir);
                    Log.d("save", String.valueOf(imageUri));
                    addItem(new ImageItem(imageUri));
                    addItem(new TextItem("This is a text item"));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else if (requestCode == PICK_AUDIO_REQUEST) {
                assert data != null;
                Uri audioUri = data.getData();
                addItem(new AudioItem(audioUri));
                addItem(new TextItem("This is a text item"));
                saveAudioToLocalDirectory(audioUri, dir);
            }
        }
    }

    private void saveImageToDirectory(Uri imageUri, File directory) {
        if (imageUri == null) return;
        try {
            ContentResolver contentResolver = getContentResolver();
            // 从URI获取输入流
            InputStream inputStream = contentResolver.openInputStream(imageUri);
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            File outputFile = new File(directory, "image-" + title.getText().toString() + "-" + timeStamp + ".3gp");
            assert inputStream != null;
            writeFile(inputStream, outputFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveAudioToLocalDirectory(Uri audioUri, File directory) {
        if (audioUri == null) return;
        try {
            // 获取ContentResolver
            ContentResolver contentResolver = getContentResolver();
            // 从URI获取输入流
            InputStream inputStream = contentResolver.openInputStream(audioUri);
            // 确定输出文件
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            File outputFile = new File(directory, "audio-" + title.getText().toString() + "-" + timeStamp + ".jpg");
            // 将输入流写入文件输出流
            assert inputStream != null;
            writeFile(inputStream, outputFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeFile(InputStream inputStream, File outputFile) throws IOException {
        OutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(outputFile);
            byte[] buffer = new byte[4 * 1024];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            outputStream.flush();
        } finally {
            if (outputStream != null) {
                outputStream.close();
            }
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }

    /*
    private void saveImageToStorage(Bitmap bitmapImage, String photoDirectory) throws IOException {
        File photoFile = createImageFile(photoDirectory);
        try (FileOutputStream fos = new FileOutputStream(photoFile)) {
            bitmapImage.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    */
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
        private final Uri audioUri;

        public AudioItem(Uri audioUri) {
            super(TYPE_AUDIO);
            this.audioUri = audioUri;
        }

        public Uri getAudioUri() {
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
        ImageButton playButton;
        MediaPlayer mediaPlayer;
        boolean playing = false;

        public AudioViewHolder(View itemView) {
            super(itemView);
            playButton = itemView.findViewById(R.id.play_button);
            mediaPlayer = new MediaPlayer();
        }

        public void bind(AudioItem audioItem) {
            playButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        if (!playing) {
                            Log.d("audio", "play");
                            mediaPlayer.setDataSource(audioItem.getAudioUri().toString());
                            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                                @Override
                                public void onPrepared(MediaPlayer mp) {
                                    mediaPlayer.start();
                                }
                            });
                            mediaPlayer.setOnCompletionListener(mp -> {
                                mediaPlayer.reset();
                            });
                            playing = !playing;
                            Log.d("audio", String.valueOf(playing));
                            playButton.setImageResource(R.drawable.ic_pause);
                        } else {
                            Log.d("audio", "pause");
                            if (mediaPlayer != null) mediaPlayer.reset();
                            playing = !playing;
                            playButton.setImageResource(R.drawable.ic_play);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e("audioerror", "IOException during prepare or start: ", e);
                        if (mediaPlayer != null) mediaPlayer.reset();
                        playButton.setImageResource(R.drawable.ic_play);
                    } catch (IllegalStateException e) {
                        Log.e("audioerror", "IllegalStateException during prepare or start: ", e);
                        if (mediaPlayer != null) mediaPlayer.reset();
                        if (mediaPlayer != null) mediaPlayer.release();
                        playButton.setImageResource(R.drawable.ic_play);
                    }
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

    protected void onPause() {
        super.onPause();
        // 保存数据
        // saveMemo2Cloud();
        int text_count = 0;
        for (RecyclerViewItem item : adapter.items) {
            if (item.getType() == RecyclerViewItem.TYPE_TEXT) {
                TextItem textItem = (TextItem) item;
                String content = textItem.getText();
                FileOutputStream fos = null;
                try {
                    File file = new File(this.dir, "text_" + text_count);
                    // 获取应用的文件目录
                    fos = new FileOutputStream(file);
                    // 将字符串写入文件
                    fos.write(content.getBytes());
                    Log.d("Text saved", file.getAbsolutePath());
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (fos != null) {
                        try {
                            fos.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                text_count += 1;
            }
        }
    }
}
