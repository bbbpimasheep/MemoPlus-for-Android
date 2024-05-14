package com.example.memo;

import android.app.Application;

import androidx.room.Room;

public class MemoPlus extends Application {
    private AppDatabase database;

    @Override
    public void onCreate() {
        super.onCreate();
        database = Room.databaseBuilder(getApplicationContext(),
                AppDatabase.class, "database-name").build();
    }

    public AppDatabase getDb() {
        return database;
    }
}
