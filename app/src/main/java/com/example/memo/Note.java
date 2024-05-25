package com.example.memo;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.List;

@Entity(tableName = "notes")
public class Note {
    @PrimaryKey
    @ColumnInfo(name = "title")
    @NonNull
    public String title;

    @ColumnInfo(name = "type")
    public String type;

    @ColumnInfo(name = "files")
    public List<String> files;

    @ColumnInfo(name = "last_edit")
    public String last_edit;

    @ColumnInfo(name = "last_save")
    public String last_save;
}