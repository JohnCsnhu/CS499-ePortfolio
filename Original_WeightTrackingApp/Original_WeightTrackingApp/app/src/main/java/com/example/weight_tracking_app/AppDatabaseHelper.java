package com.example.weight_tracking_app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import java.util.ArrayList;

/**
 * AppDatabaseHelper
 *
 * Project Three requirement: persistent SQLite database.
 * This helper creates a small DB with:
 *  - users: stores login credentials + goal + phone number
 *  - weights: stores daily weight entries for a specific user
 *
 * Notes:
 *  - Passwords are stored as plain text ONLY for course/demo purposes.
 *    (In production, always hash+salt with a strong KDF.)
 */
public class AppDatabaseHelper extends SQLiteOpenHelper {

    public static final String DB_NAME = "weight_tracking.db";
    public static final int DB_VERSION = 1;

    // ----- users table -----
    public static final String T_USERS = "users";
    public static final String C_USER_ID = "_id";
    public static final String C_USERNAME = "username";
    public static final String C_PASSWORD = "password";
    public static final String C_GOAL_WEIGHT = "goal_weight";
    public static final String C_PHONE = "phone";

    // ----- weights table -----
    public static final String T_WEIGHTS = "weights";
    public static final String C_WEIGHT_ID = "_id";
    public static final String C_WEIGHT_USER_ID = "user_id";
    public static final String C_DATE = "date";
    public static final String C_WEIGHT = "weight";

    public AppDatabaseHelper(@Nullable Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createUsers = "CREATE TABLE " + T_USERS + " (" +
                C_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                C_USERNAME + " TEXT NOT NULL UNIQUE, " +
                C_PASSWORD + " TEXT NOT NULL, " +
                C_GOAL_WEIGHT + " REAL, " +
                C_PHONE + " TEXT" +
                ");";

        String createWeights = "CREATE TABLE " + T_WEIGHTS + " (" +
                C_WEIGHT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                C_WEIGHT_USER_ID + " INTEGER NOT NULL, " +
                C_DATE + " TEXT NOT NULL, " +
                C_WEIGHT + " REAL NOT NULL, " +
                "FOREIGN KEY(" + C_WEIGHT_USER_ID + ") REFERENCES " + T_USERS + "(" + C_USER_ID + ")" +
                ");";

        db.execSQL(createUsers);
        db.execSQL(createWeights);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Simple strategy for a course project:
        db.execSQL("DROP TABLE IF EXISTS " + T_WEIGHTS);
        db.execSQL("DROP TABLE IF EXISTS " + T_USERS);
        onCreate(db);
    }

    // -------------------------
    // Users: Create + Login
    // -------------------------

    /**
     * Create a new user.
     * @return new userId, or -1 if insert failed (e.g., username already exists)
     */
    public long createUser(String username, String password) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(C_USERNAME, username);
        cv.put(C_PASSWORD, password);
        return db.insert(T_USERS, null, cv);
    }

    /**
     * Validate username+password and return userId if correct, else -1.
     */
    public long validateLogin(String username, String password) {
        SQLiteDatabase db = getReadableDatabase();

        try (Cursor c = db.query(
                T_USERS,
                new String[]{C_USER_ID},
                C_USERNAME + "=? AND " + C_PASSWORD + "=?",
                new String[]{username, password},
                null, null, null
        )) {
            if (c.moveToFirst()) {
                return c.getLong(0);
            }
        }
        return -1;
    }

    /**
     * Get goal weight for user. Returns -1 if not set.
     */
    public double getGoalWeight(long userId) {
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.query(
                T_USERS,
                new String[]{C_GOAL_WEIGHT},
                C_USER_ID + "=?",
                new String[]{String.valueOf(userId)},
                null, null, null
        )) {
            if (c.moveToFirst() && !c.isNull(0)) {
                return c.getDouble(0);
            }
        }
        return -1;
    }

    public boolean updateGoalWeight(long userId, double goalWeight) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(C_GOAL_WEIGHT, goalWeight);
        int rows = db.update(T_USERS, cv, C_USER_ID + "=?", new String[]{String.valueOf(userId)});
        return rows > 0;
    }

    public String getPhone(long userId) {
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.query(
                T_USERS,
                new String[]{C_PHONE},
                C_USER_ID + "=?",
                new String[]{String.valueOf(userId)},
                null, null, null
        )) {
            if (c.moveToFirst() && !c.isNull(0)) {
                return c.getString(0);
            }
        }
        return null;
    }

    public boolean updatePhone(long userId, String phone) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(C_PHONE, phone);
        int rows = db.update(T_USERS, cv, C_USER_ID + "=?", new String[]{String.valueOf(userId)});
        return rows > 0;
    }

    // -------------------------
    // Weights: CRUD
    // -------------------------

    public long insertWeight(long userId, String date, double weight) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(C_WEIGHT_USER_ID, userId);
        cv.put(C_DATE, date);
        cv.put(C_WEIGHT, weight);
        return db.insert(T_WEIGHTS, null, cv);
    }

    public boolean updateWeight(long weightId, String date, double weight) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(C_DATE, date);
        cv.put(C_WEIGHT, weight);
        int rows = db.update(T_WEIGHTS, cv, C_WEIGHT_ID + "=?", new String[]{String.valueOf(weightId)});
        return rows > 0;
    }

    public boolean deleteWeight(long weightId) {
        SQLiteDatabase db = getWritableDatabase();
        int rows = db.delete(T_WEIGHTS, C_WEIGHT_ID + "=?", new String[]{String.valueOf(weightId)});
        return rows > 0;
    }

    /**
     * Read all weights for a user. Returned in reverse-chronological order (latest first).
     */
    public ArrayList<WeightEntry> getWeightsForUser(long userId) {
        ArrayList<WeightEntry> out = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        try (Cursor c = db.query(
                T_WEIGHTS,
                new String[]{C_WEIGHT_ID, C_DATE, C_WEIGHT},
                C_WEIGHT_USER_ID + "=?",
                new String[]{String.valueOf(userId)},
                null, null,
                C_WEIGHT_ID + " DESC"
        )) {
            while (c.moveToNext()) {
                long id = c.getLong(0);
                String date = c.getString(1);
                double weight = c.getDouble(2);
                out.add(new WeightEntry(id, date, weight));
            }
        }
        return out;
    }
}
