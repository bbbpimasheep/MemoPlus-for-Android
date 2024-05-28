package com.example.memo;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import org.json.JSONObject;

import java.util.List;

@Dao
public interface NoteDao {
    @Query("SELECT * FROM notes")
    List<Note> getAllNotes();

    @Query("DELETE FROM notes")
    void deleteAllNotes();

    @Query("SELECT * FROM notes WHERE title = :title")
    Note getNoteByTitle(String title);

    @Query("SELECT * FROM notes WHERE id = :id")
    Note getNoteByID(int id);

    @Insert
    void insertNote(Note note);

    @Update
    void updateNote(Note note);

    @Delete
    void deleteNote(Note note);

    @Query("UPDATE notes SET files = :files WHERE title = :title")
    void updateFiles(String title, List<String> files);
}
