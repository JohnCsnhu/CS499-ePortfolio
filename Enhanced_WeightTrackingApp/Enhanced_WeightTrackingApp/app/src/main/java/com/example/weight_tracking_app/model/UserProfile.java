package com.example.weight_tracking_app.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

/** One profile per user. The primary key {@code id} equals the owning user id. */
@Entity(tableName = "user_profile",
        foreignKeys = @ForeignKey(entity = User.class, parentColumns = "id",
                childColumns = "id", onDelete = ForeignKey.CASCADE))
public class UserProfile {
    @PrimaryKey
    public int id; // == owning User.id
    @NonNull public String name = "";
    public int age = 0;
    public double heightCm = 0;
    @NonNull public String gender = "";
    public double startingWeightKg = 0;
    public double goalWeightKg = 0;
    @NonNull public String preferredUnit = "lb";
    public boolean remindersEnabled = false;
    public int reminderHour = 8;
    public int reminderMinute = 0;

    public UserProfile() {}
    public UserProfile(int userId) { this.id = userId; }
}
