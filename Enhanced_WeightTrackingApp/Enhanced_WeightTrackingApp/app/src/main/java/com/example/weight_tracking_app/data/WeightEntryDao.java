package com.example.weight_tracking_app.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.weight_tracking_app.model.WeightEntry;

import java.util.List;

@Dao
public interface WeightEntryDao {
    // Ordered by the real entry date (not row id), so 'latest' is truly the most
    // recent weigh-in even when an entry is back-dated.
    @Query("SELECT * FROM weight_entries WHERE userId = :userId ORDER BY entryDate DESC, createdAt DESC")
    LiveData<List<WeightEntry>> observeAll(int userId);

    @Query("SELECT * FROM weight_entries WHERE userId = :userId ORDER BY entryDate ASC, createdAt ASC")
    List<WeightEntry> getAllAscendingSync(int userId);

    @Query("SELECT * FROM weight_entries WHERE id = :id LIMIT 1")
    WeightEntry getByIdSync(int id);

    @Insert long insert(WeightEntry entry);
    @Update void update(WeightEntry entry);

    @Query("DELETE FROM weight_entries WHERE id = :id")
    void deleteById(int id);
}
