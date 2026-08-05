package com.example.weight_tracking_app.util;

import android.content.Context;
import android.content.SharedPreferences;

/** Remembers which user is currently logged in across app launches. */
public final class SessionManager {
    private static final String PREFS = "session";
    private static final String KEY_USER_ID = "current_user_id";
    public static final int NO_USER = -1;

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void setUserId(int userId) { prefs.edit().putInt(KEY_USER_ID, userId).apply(); }
    public int getUserId() { return prefs.getInt(KEY_USER_ID, NO_USER); }
    public boolean isLoggedIn() { return getUserId() != NO_USER; }
    public void clear() { prefs.edit().remove(KEY_USER_ID).apply(); }
}
