package com.example.memo;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Entity(tableName = "user_schema")
public class User {
    public String name;
    @PrimaryKey
    @NonNull
    public String ID;
    public String password;
    public String signature;
    public String iconPath;

    public User() {
        ID = "furina_defontaine@gmail.com";
    }
}

