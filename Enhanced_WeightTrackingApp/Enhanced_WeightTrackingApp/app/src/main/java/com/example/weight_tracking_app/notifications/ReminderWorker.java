package com.example.weight_tracking_app.notifications;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class ReminderWorker extends Worker {
    public ReminderWorker(@NonNull Context context, @NonNull WorkerParameters params) { super(context, params); }
    @NonNull @Override public Result doWork() {
        String title = getInputData().getString("title");
        String body = getInputData().getString("body");
        NotificationHelper.createChannel(getApplicationContext());
        NotificationHelper.showNow(getApplicationContext(), title != null ? title : "Weight reminder", body != null ? body : "Log your weight today.");
        return Result.success();
    }
}
