package com.example.weight_tracking_app.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * A registered account. Passwords are never stored directly: only a PBKDF2
 * hash and the per-user salt are persisted (see util.PasswordUtils).
 */
@Entity(tableName = "users", indices = {@Index(value = "username", unique = true)})
public class User {
    @PrimaryKey(autoGenerate = true)
    public int id;
    @NonNull public String username = "";
    @NonNull public String passwordHash = "";
    @NonNull public String salt = "";
    @NonNull public String phoneNumber = "";
}
