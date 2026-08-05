package com.example.weight_tracking_app.notifications;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
public class ReminderReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) { NotificationHelper.showNow(context, "Weight reminder", "Log your weight today."); }
}
