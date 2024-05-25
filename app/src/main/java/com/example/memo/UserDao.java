package com.example.memo;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface UserDao {
    /*
    @Query("SELECT * FROM users")
    List<User> getAllUsers();
    @Query("SELECT * FROM users WHERE userid = :userId")
    User getUserById(int userId);
     */
    @Insert
    void insertUser(User user);
    @Update
    void updateUser(User user);
    @Delete
    void deleteUser(User user);
}

