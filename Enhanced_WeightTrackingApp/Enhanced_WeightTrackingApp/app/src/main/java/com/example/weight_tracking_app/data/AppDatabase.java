package com.example.weight_tracking_app.data;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.weight_tracking_app.model.User;
import com.example.weight_tracking_app.model.UserProfile;
import com.example.weight_tracking_app.model.WeightEntry;

@Database(entities = {User.class, UserProfile.class, WeightEntry.class}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract UserDao userDao();
    public abstract UserProfileDao userProfileDao();
    public abstract WeightEntryDao weightEntryDao();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "weight_tracking.db")
                            .fallbackToDestructiveMigration()
                            .addCallback(FK_CALLBACK)
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    // Explicitly enable foreign-key enforcement on every connection so the
    // schema's integrity rules (cascade delete of a user's rows) are applied.
    private static final Callback FK_CALLBACK = new Callback() {
        @Override
        public void onOpen(@NonNull SupportSQLiteDatabase db) {
            super.onOpen(db);
            db.execSQL("PRAGMA foreign_keys=ON;");
        }
    };
}
