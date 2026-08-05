package com.example.weight_tracking_app.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.weight_tracking_app.model.User;

@Dao
public interface UserDao {
    @Insert long insert(User user);
    @Update void update(User user);

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    User findByUsername(String username);

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    User findById(int id);

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    LiveData<User> observe(int id);

    @Query("SELECT COUNT(*) FROM users WHERE username = :username")
    int countByUsername(String username);

    @Query("DELETE FROM users WHERE id = :id")
    void deleteById(int id);
}
