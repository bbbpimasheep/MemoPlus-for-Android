package com.example.memo;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "users")
public class User {
    @PrimaryKey
    @ColumnInfo(name = "userID")
    @NonNull
    public String userID;

    @ColumnInfo(name = "username")
    public String username;

    @ColumnInfo(name = "password")
    public String password;

    @ColumnInfo(name = "signature")
    public String signature;

    @ColumnInfo(name = "token")
    public String token;

    @ColumnInfo(name = "last_login")
    public String last_login;

    @ColumnInfo(name = "avatar")
    public String avatar;

    public User() {
        userID = "";
    }
}