package com.example.memo;

import static androidx.core.content.PackageManagerCompat.LOG_TAG;

import static com.example.memo.MainActivity.uri_s;

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
import android.media.browse.MediaBrowser;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
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
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MemoContent extends AppCompatActivity{
    private static AppDatabase db;
    private UserDao userDao;
    private NoteDao noteDao;
    static final int SELECT_IMAGE_REQUEST = 1;
    static final int TAKE_PHOTO_REQUEST = 2;
    static final int PICK_AUDIO_REQUEST = 3;
    static final int REQUEST_CAMERA_PERMISSION = 100;
    static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    ImageButton back2home, picture, audio, camera, recorder,
                delete, save, settags;
    EditText title, time;
    @SuppressLint("StaticFieldLeak")
    static MultiTypeAdapter adapter;
    List<RecyclerViewItem> items;
    RecyclerView recyclerView;
    File dir;
    boolean isRecording = false;
    MediaRecorder Mrecorder = null;
    String audioPath = null, lastSave2Cloud, type, memoTitle, memoTime,
            authToken, userID;
    int noteID = -1;
    Note note = null;
    ExecutorService executorService;

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

        this.recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        this.items = new ArrayList<>();
        adapter = new MultiTypeAdapter(items, MemoContent.this);
        recyclerView.setAdapter(adapter);

        db = MemoPlus.getInstance().getAppDatabase();
        userDao = db.userDao();
        noteDao = db.noteDao();

        this.executorService = Executors.newFixedThreadPool(1);

        Intent intent = getIntent();
        this.memoTitle = intent.getStringExtra("TITLE");
        this.memoTime = intent.getStringExtra("TIME");
        this.noteID = intent.getIntExtra("ID", -1);
        assert memoTitle != null;
        title.setText(memoTitle);

        delete.setOnClickListener(v -> {
            // deleteMemo();
            executorService.submit(() -> {
                noteDao.deleteById(noteID);
            });
            Intent intent13 = new Intent(MemoContent.this, MainActivity.class);
            startActivity(intent13);
        });

        save.setOnClickListener(v -> saveMemo2Local());

        back2home.setOnClickListener(v -> {
            Log.d("back", "Back button clicked!");
            Intent intent12 = new Intent(MemoContent.this, MainActivity.class);
            startActivity(intent12);
        });

        picture.setOnClickListener(this::selectPic);

        camera.setOnClickListener(v -> {
            Log.d("camera", "heard");
            openCamera();
        });

        audio.setOnClickListener(v -> openAudioFilePicker());

        recorder.setOnClickListener(v -> {
            Log.d("recorder", "heard");
            openRecorder();
        });

        settags.setOnClickListener(v -> {
            Intent intent1 = new Intent(MemoContent.this, Tags.class);
            intent1.putExtra("TITLE", title.getText().toString());
            intent1.putExtra("TIME", memoTime);
            intent1.putExtra("TYPE", type);
            intent1.putExtra("ID", noteID);
            startActivity(intent1);
        });
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    protected void onResume() {
        super.onResume();
        loadInfo();
    }

    protected void loadInfo() {
        executorService.submit(() -> {
            List<User> users = userDao.getAllUsers();
            User user = users.get(0);
            if (user != null) {
                authToken = user.userID;
                userID = user.userID;
            }

            this.note = noteDao.getNoteByID(noteID);
            Log.d("tags", note.type);
            this.lastSave2Cloud = note.last_save;
            this.type = note.type;
            Log.d("title-in", note.title);
            if (Objects.equals(lastSave2Cloud, "Cloud")) {
                try {
                    fetchFromCLoud();
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(() -> {
                this.dir = new File(MemoContent.this.getFilesDir(), String.valueOf(noteID));
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
                List<String> files = note.files;
                adapter.items.clear();
                for(String file: files) {
                    try {
                        if (file != null && !file.isEmpty()){
                            JSONObject fileJSON = new JSONObject(file);
                            String typeName = fileJSON.getString("type");
                            Log.d("type-text", file);
                            if(typeName.equals("text")) {
                                addItem(new TextItem(fileJSON.getString("content")));
                            }else if(typeName.equals("image")){
                                addItem(new ImageItem(fileJSON.getString("content")));
                            }else if(typeName.equals("audio")){
                                addItem(new AudioItem(fileJSON.getString("content")));
                            }
                        }
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                }
                if (adapter.items.isEmpty()) {
                    addItem(new TextItem("Type here."));
                }
                recyclerView.setAdapter(adapter);
            });
        });
    }

    private void fetchFromCLoud() throws JSONException {
        List<String> files_from_cloud = note.files, files_local = new ArrayList<>();
        for(String file: files_from_cloud) {
            if (file != null && !file.isEmpty()) {
                JSONObject fileJSON = new JSONObject(file);
                String type = fileJSON.getString("type");
                if (!type.equals("text")) {
                    sendPOST_syncDownload(type, fileJSON.getString("content"), new OnHttpCallback(){
                        @Override
                        public void onSuccess(String feedBack) {
                            String localPath = feedBack;
                            if (type.equals("image")) {
                                String image = "{\"content\": \"" + localPath + "\"," + "\"type\": \"image\"}";
                                files_local.add(image);
                            } else if (type.equals("audio")) {
                                String audio = "{\"content\": \"" + localPath + "\"," + "\"type\": \"image\"}";
                                files_local.add(audio);
                            }
                        }
                        @Override
                        public void onFailure(Exception e) {
                            e.printStackTrace();
                        }
                    });
                } else {
                    files_local.add(file);
                }
            }
        }
    }

    private void sendPOST_syncDownload(String type, String path, OnHttpCallback callback) {
        executorService.submit(() -> {
            try {
                String localPath = performDownloadRequest(type, path); // 假设这是获取到的 userID
                callback.onSuccess(localPath);
            } catch (Exception e) {
                callback.onFailure(e);
            }
        });
    }

    private String performDownloadRequest(String type, String path) throws IOException, JSONException {
        URI uri = null;
        try {
            uri = new URI(uri_s + "syncDownload");
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return "";
        }
        URL url = uri.toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; utf-8");
        conn.setRequestProperty("Accept", "application/json");
        conn.setDoOutput(true);
        conn.setRequestProperty("Authorization", authToken);

        JSONObject jsonInputString = new JSONObject();
        jsonInputString.put("path", path);

        try(OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonInputString.toString().getBytes("utf-8");
            os.write(input, 0, input.length);
        }
        
        String filePath = "";
        int responseCode = conn.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            try(BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"))) {
                Path p = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    p = Paths.get(path);
                }
                String filename = "";
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    filename = p.getFileName().toString();
                }
                if (!filename.equals("")) {
                    File file = new File(dir, filename);
                    FileOutputStream output = new FileOutputStream(file);
                    filePath = file.getPath();
                    byte[] buffer = new byte[4096];
                    int bytesRead = -1;
                    while ((bytesRead = conn.getInputStream().read(buffer)) != -1) {
                        output.write(buffer, 0, bytesRead);
                    }
                    output.close();
                }
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
        return filePath;
    }
    @SuppressLint("NotifyDataSetChanged")
    private void saveMemo2Local() {
        adapter.notifyDataSetChanged();
        Note updated = new Note();
        updated.id = noteID;
        updated.title = title.getText().toString();
        Log.d("title", updated.title);

        List<String> contentJSON = new ArrayList<>();
        for (RecyclerViewItem item : adapter.items) {
            if (item.getType() == RecyclerViewItem.TYPE_TEXT) {
                TextItem textItem = (TextItem) item;
                String text = null;
                if (!textItem.getText().isEmpty()) {
                    text = "{\"content\": \"" + textItem.getText() + "\"," +
                            "\"type\": \"text\"}";
                    contentJSON.add(text);
                }
            } else if (item.getType() == RecyclerViewItem.TYPE_IMAGE) {
                ImageItem imageItem = (ImageItem) item;
                String path = imageItem.getImagePath();
                Log.d("note-type", path);
                String image = "{\"content\": \"" + path + "\"," +
                        "\"type\": \"image\"}";
                contentJSON.add(image);
            } else if (item.getType() == RecyclerViewItem.TYPE_AUDIO) {
                AudioItem audioItem = (AudioItem) item;
                String path = audioItem.getAudioPath();
                String audio = "{\"content\": \"" + path + "\"," +
                        "\"type\": \"audio\"}";
                contentJSON.add(audio);
            }

            updated.files = contentJSON;
            String timeStamp = new SimpleDateFormat("MM.dd HH:mm").format(new Date());
            updated.last_edit = timeStamp;
            updated.last_save = timeStamp;
            updated.type = type;
        }
        executorService.submit(() -> {
            // 执行后台任务
            noteDao.updateNote(updated);
        });
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
            Log.d("audio", audioPath);
            addItem(new AudioItem(audioPath));
            addItem(new TextItem(""));
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
                String local = saveImageToDirectory(selectedImageUri, dir);
                addItem(new ImageItem(local));
                addItem(new TextItem(""));
                adapter.notifyDataSetChanged();
            } else if (requestCode == TAKE_PHOTO_REQUEST) {
                if (data != null && data.getExtras() != null) {
                    Bitmap imageBitmap = (Bitmap) data.getExtras().get("data");
                    if (imageBitmap != null) {
                        // 保存照片到文件中
                        String path = saveBitmapToDirectory(imageBitmap, dir);
                        // 添加照片项到列表中
                        addItem(new ImageItem(path));
                        addItem(new TextItem(""));
                    }
                }
            } else if (requestCode == PICK_AUDIO_REQUEST) {
                assert data != null;
                Uri audioUri = data.getData();
                String local = saveAudioToLocalDirectory(audioUri, dir);
                addItem(new AudioItem(local));
                addItem(new TextItem(""));
            }
        }
    }

    private String saveBitmapToDirectory(Bitmap imageBitmap, File directory) {
        File outputFile = null;
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            outputFile = new File(directory, "image-" + title.getText().toString() + "-" + timeStamp + ".jpg");
            FileOutputStream outputStream = new FileOutputStream(outputFile);
            imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return outputFile.getPath();
    }

    private String saveImageToDirectory(Uri imageUri, File directory) {
        if (imageUri == null) return null;
        File outputFile = null;
        try {
            ContentResolver contentResolver = getContentResolver();
            // 从URI获取输入流
            InputStream inputStream = contentResolver.openInputStream(imageUri);
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            outputFile = new File(directory, "image-" + title.getText().toString() + "-" + timeStamp + ".jpg");
            assert inputStream != null;
            writeFile(inputStream, outputFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return outputFile.getPath();
    }

    private String saveAudioToLocalDirectory(Uri audioUri, File directory) {
        if (audioUri == null) return null;
        File outputFile = null;
        try {
            // 获取ContentResolver
            ContentResolver contentResolver = getContentResolver();
            // 从URI获取输入流
            InputStream inputStream = contentResolver.openInputStream(audioUri);
            // 确定输出文件
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            outputFile = new File(directory, "audio-" + title.getText().toString() + "-" + timeStamp + ".3pg");
            // 将输入流写入文件输出流
            assert inputStream != null;
            writeFile(inputStream, outputFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        String path = outputFile.getPath();
        return outputFile.getPath();
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

    private void deleteMemo() {
        executorService.submit(() -> {
            noteDao.deleteById(noteID);
        });
    }

    private File createImageFile(String photoDirectory) throws IOException {
        Log.d("image", "filepath");
        // 创建图片文件名
        @SuppressLint("SimpleDateFormat")
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String path = photoDirectory + "/" + title.getText().toString() + "-" + timeStamp + ".jpg";
        Log.d("photo", path);
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
        private String text;

        public TextItem(String text) {
            super(TYPE_TEXT);
            this.text = text;
        }

        public String getText() {
            return text;
        }
        public void updateText(String s) { this.text = s;}
    }

    public static class ImageItem extends RecyclerViewItem {
        private final String imagePath;

        public ImageItem(String imagePath) {
            super(TYPE_IMAGE);
            this.imagePath = imagePath;
        }

        public String getImagePath() {
            return imagePath;
        }
    }

    public static class AudioItem extends RecyclerViewItem {
        private final String audioPath;
        private final Uri audioUri;

        public AudioItem(String audioPath) {
            super(TYPE_AUDIO);
            this.audioPath = audioPath;
            this.audioUri = Uri.parse(audioPath);
        }

        public Uri getAudioUri() { return audioUri; }
        public String getAudioPath() { return audioPath; }
    }

    public static class TextViewHolder extends RecyclerView.ViewHolder {
        EditText editText;

        public TextViewHolder(View itemView) {
            super(itemView);
            editText = itemView.findViewById(R.id.edit_text);
        }

        public void bind(TextItem textItem) {
            editText.setText(textItem.getText());
            editText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable s) {
                    textItem.updateText(s.toString());
                }
            });
        }
    }

    public static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        ImageButton delete;

        public ImageViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.image_view);
            delete = itemView.findViewById(R.id.delete);
        }

        public void bind(ImageItem imageItem, Context context) {
            try {
                File imageFile = new File(imageItem.getImagePath());
                InputStream inputStream = new FileInputStream(imageFile);
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                Bitmap scaledBitmap = scaleBitmapToFitImageView(bitmap, imageView);
                imageView.setImageBitmap(scaledBitmap);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            delete.setOnClickListener(v -> {
                int pos = getBindingAdapterPosition();
                adapter.deleteItem(pos);
            });
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
        Context context;
        ImageButton playButton;
        MediaPlayer mediaPlayer;
        ImageButton delete;
        boolean playing = false;

        public AudioViewHolder(View itemView, Context context) {
            super(itemView);
            this.playButton = itemView.findViewById(R.id.play_button);
            this.mediaPlayer = new MediaPlayer();
            this.delete = itemView.findViewById(R.id.delete);
            this.context = context;
        }

        public void bind(AudioItem audioItem) {
            playButton.setOnClickListener(v -> {
                try {
                    /*
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
                    */

                    if (!playing) {
                        Log.d("audio", "play");
                        try {
                            mediaPlayer.reset(); // 重置MediaPlayer以确保不会影响之前的播放
                            mediaPlayer.setDataSource(context, Uri.parse(audioItem.getAudioUri().toString()));
                            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                                @Override
                                public void onPrepared(MediaPlayer mp) {
                                    mediaPlayer.start();
                                }
                            });
                            mediaPlayer.setOnCompletionListener(mp -> {
                                playing = false; // 更新播放状态
                                playButton.setImageResource(R.drawable.ic_play); // 更改按钮图标
                                mediaPlayer.reset(); // 重置MediaPlayer
                                Log.d("audio", "completed");
                            });
                            mediaPlayer.prepareAsync(); // 异步准备MediaPlayer
                            playing = true; // 更新播放状态
                            Log.d("audio", String.valueOf(playing));
                            playButton.setImageResource(R.drawable.ic_pause); // 更改按钮图标
                        } catch (IOException e) {
                            e.printStackTrace();
                            Log.e("audioerror", "IOException during prepare or start: ", e);
                            if (mediaPlayer != null) mediaPlayer.reset();
                            playButton.setImageResource(R.drawable.ic_play);
                        } catch (IllegalStateException e) {
                            Log.e("audioerror", "IllegalStateException during prepare or start: ", e);
                            if (mediaPlayer != null) {
                                mediaPlayer.reset();
                                mediaPlayer.release();
                            }
                            playButton.setImageResource(R.drawable.ic_play);
                        }
                    } else {
                        Log.d("audio", "pause");
                        if (mediaPlayer != null) {
                            mediaPlayer.pause(); // 暂停播放
                            mediaPlayer.seekTo(0); // 可选：回到音频开始位置
                        }
                        playing = false; // 更新播放状态
                        playButton.setImageResource(R.drawable.ic_play); // 更改按钮图标
                    }

                } catch (IllegalStateException e) {
                    Log.e("audioerror", "IllegalStateException during prepare or start: ", e);
                    if (mediaPlayer != null) mediaPlayer.reset();
                    if (mediaPlayer != null) mediaPlayer.release();
                    playButton.setImageResource(R.drawable.ic_play);
                }
            });

            delete.setOnClickListener(v -> {
                int pos = getBindingAdapterPosition();
                adapter.deleteItem(pos);
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
                return new AudioViewHolder(view, context);
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

        public void deleteItem(int position) {
            items.remove(position);
            notifyItemRemoved(position);
        }
    }

    protected void onPause() {
        super.onPause();
        // 保存数据
        saveMemo2Local();
        // saveMemo2Cloud();

    }
}
