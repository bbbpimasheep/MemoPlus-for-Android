package com.example.memo;

import android.app.Application;

import androidx.room.Room;

public class MemoPlus extends Application {
    private static MemoPlus instance;
    private AppDatabase appDatabase;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        appDatabase = Room.databaseBuilder(getApplicationContext(),
                AppDatabase.class, "my-database").build();
    }

    public static MemoPlus getInstance() {
        return instance;
    }

    public AppDatabase getAppDatabase() {
        return appDatabase;
    }
}
