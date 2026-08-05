package com.example.weight_tracking_app.notifications;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import com.example.weight_tracking_app.R;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public final class NotificationHelper {
    public static final String CHANNEL_ID = "weight_tracker_reminders";
    private static final String UNIQUE_WORK_NAME = "daily_weight_reminder";
    private NotificationHelper() {}

    public static void createChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Weight reminders", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Daily reminders to log your weight");
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    public static void showNow(Context context, String title, String body) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);
        NotificationManagerCompat.from(context).notify(1001, builder.build());
    }

    /** A guaranteed on-device celebration when the goal is reached. */
    public static void showGoalReached(Context context, String body) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Goal reached! \ud83c\udf89")
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);
        NotificationManagerCompat.from(context).notify(2001, builder.build());
    }

    public static void scheduleDailyReminder(Context context, int hour, int minute) {
        cancelDailyReminder(context);
        Calendar now = Calendar.getInstance();
        Calendar firstRun = Calendar.getInstance();
        firstRun.set(Calendar.HOUR_OF_DAY, hour);
        firstRun.set(Calendar.MINUTE, minute);
        firstRun.set(Calendar.SECOND, 0);
        if (firstRun.before(now)) firstRun.add(Calendar.DAY_OF_YEAR, 1);
        long initialDelay = firstRun.getTimeInMillis() - now.getTimeInMillis();
        Data inputData = new Data.Builder().putString("title", "Time to log your weight").putString("body", "Open the app and record today’s entry.").build();
        PeriodicWorkRequest work = new PeriodicWorkRequest.Builder(ReminderWorker.class, 24, TimeUnit.HOURS)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .setInputData(inputData)
                .setConstraints(new Constraints.Builder().build())
                .build();
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(UNIQUE_WORK_NAME, ExistingPeriodicWorkPolicy.REPLACE, work);
    }

    public static void cancelDailyReminder(Context context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME);
    }
}
