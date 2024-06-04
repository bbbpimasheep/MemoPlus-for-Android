package com.example.memo;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.ArrayList;
import java.util.List;

@Entity(tableName = "notes")
public class Note {
    @PrimaryKey
    @ColumnInfo(name = "id")
    public int id;

    @ColumnInfo(name = "title")
    public String title;

    @ColumnInfo(name = "type")
    public String type;

    @ColumnInfo(name = "files")
    public List<String> files;

    @ColumnInfo(name = "last_edit")
    public String last_edit;

    @ColumnInfo(name = "last_save")
    public String last_save;

    public Note() {
        files = new ArrayList<>();
    }
}