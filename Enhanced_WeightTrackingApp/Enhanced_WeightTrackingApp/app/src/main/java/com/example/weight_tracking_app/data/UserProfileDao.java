package com.example.weight_tracking_app.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.weight_tracking_app.model.UserProfile;

@Dao
public interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = :userId")
    LiveData<UserProfile> observe(int userId);

    @Query("SELECT * FROM user_profile WHERE id = :userId")
    UserProfile getSync(int userId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(UserProfile userProfile);
}
