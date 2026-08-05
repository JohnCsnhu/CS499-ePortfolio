package com.example.weight_tracking_app.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "weight_entries",
        foreignKeys = @ForeignKey(entity = User.class, parentColumns = "id",
                childColumns = "userId", onDelete = ForeignKey.CASCADE),
        indices = {@Index("userId")})
public class WeightEntry {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public int userId;
    @NonNull public String entryDate = "";
    public double weightKg = 0;
    @NonNull public String notes = "";
    public long createdAt = System.currentTimeMillis();
}
