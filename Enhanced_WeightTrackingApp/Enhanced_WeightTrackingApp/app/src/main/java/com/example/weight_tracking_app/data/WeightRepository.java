package com.example.weight_tracking_app.data;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.weight_tracking_app.model.User;
import com.example.weight_tracking_app.model.UserProfile;
import com.example.weight_tracking_app.model.WeightEntry;
import com.example.weight_tracking_app.model.WeightSummary;
import com.example.weight_tracking_app.notifications.NotificationHelper;
import com.example.weight_tracking_app.notifications.SmsHelper;
import com.example.weight_tracking_app.util.Analytics;
import com.example.weight_tracking_app.util.DateUtils;
import com.example.weight_tracking_app.util.HealthUtils;
import com.example.weight_tracking_app.util.PasswordUtils;

import java.io.File;
import java.io.FileWriter;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WeightRepository {
    public interface Callback<T> { void onComplete(T value); }

    /** Result of an add-entry operation, so the UI can react to a goal being reached. */
    public static class AddResult {
        public final boolean goalJustReached;
        public final boolean smsSent;
        public AddResult(boolean goalJustReached, boolean smsSent) {
            this.goalJustReached = goalJustReached;
            this.smsSent = smsSent;
        }
    }

    private final Context appContext;
    private final UserDao userDao;
    private final UserProfileDao userProfileDao;
    private final WeightEntryDao weightEntryDao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public WeightRepository(Context context) {
        appContext = context.getApplicationContext();
        AppDatabase db = AppDatabase.getInstance(appContext);
        userDao = db.userDao();
        userProfileDao = db.userProfileDao();
        weightEntryDao = db.weightEntryDao();
    }

    // ---------------- Authentication ----------------

    /** Creates an account with a hashed+salted password. Returns the new user id, or -1 if the username is taken. */
    public void register(String username, String password, String phoneNumber, Callback<Integer> callback) {
        executor.execute(() -> {
            int result = -1;
            if (username != null && !username.trim().isEmpty()
                    && password != null && !password.isEmpty()
                    && userDao.countByUsername(username.trim()) == 0) {
                User user = new User();
                user.username = username.trim();
                user.salt = PasswordUtils.generateSalt();
                user.passwordHash = PasswordUtils.hash(password, user.salt);
                user.phoneNumber = phoneNumber == null ? "" : phoneNumber.trim();
                int userId = (int) userDao.insert(user);
                // Seed an empty profile for the new user.
                userProfileDao.insert(new UserProfile(userId));
                result = userId;
            }
            if (callback != null) callback.onComplete(result);
        });
    }

    /** Validates credentials by hashing the entered password. Returns user id, or -1 on failure. */
    public void login(String username, String password, Callback<Integer> callback) {
        executor.execute(() -> {
            int result = -1;
            if (username != null && password != null) {
                User user = userDao.findByUsername(username.trim());
                if (user != null && PasswordUtils.verify(password, user.salt, user.passwordHash)) {
                    result = user.id;
                }
            }
            if (callback != null) callback.onComplete(result);
        });
    }

    public LiveData<User> observeUser(int userId) { return userDao.observe(userId); }

    public void updatePhoneNumber(int userId, String phoneNumber, Runnable onDone) {
        executor.execute(() -> {
            User user = userDao.findById(userId);
            if (user != null) {
                user.phoneNumber = phoneNumber == null ? "" : phoneNumber.trim();
                userDao.update(user);
            }
            if (onDone != null) onDone.run();
        });
    }

    // ---------------- Per-user data ----------------

    public LiveData<UserProfile> observeProfile(int userId) { return userProfileDao.observe(userId); }
    public LiveData<List<WeightEntry>> observeEntries(int userId) { return weightEntryDao.observeAll(userId); }

    public void saveProfile(UserProfile profile, Runnable onDone) {
        executor.execute(() -> { userProfileDao.insert(profile); if (onDone != null) onDone.run(); });
    }

    /** Inserts an entry and, if this weigh-in newly reaches the goal, sends the congratulations SMS. */
    public void addEntry(WeightEntry entry, Callback<AddResult> callback) {
        executor.execute(() -> {
            int userId = entry.userId;
            UserProfile profile = userProfileDao.getSync(userId);
            double goal = profile != null ? profile.goalWeightKg : 0;

            List<WeightEntry> before = weightEntryDao.getAllAscendingSync(userId);
            Double prevLatest = before.isEmpty() ? null : before.get(before.size() - 1).weightKg;

            weightEntryDao.insert(entry);

            boolean goalJustReached = false;
            boolean smsSent = false;
            if (goal > 0) {
                List<WeightEntry> after = weightEntryDao.getAllAscendingSync(userId);
                double newLatest = after.get(after.size() - 1).weightKg;
                boolean reachedNow = newLatest <= goal;
                boolean reachedBefore = prevLatest != null && prevLatest <= goal;
                if (reachedNow && !reachedBefore) {
                    goalJustReached = true;
                    User user = userDao.findById(userId);
                    String unit = profile.preferredUnit;
                    String message = "Congratulations! You reached your goal weight of "
                            + HealthUtils.formatWeight(HealthUtils.fromKg(goal, unit), unit) + ". Keep it up!";
                    // Guaranteed on-device celebration, regardless of SMS availability.
                    NotificationHelper.showGoalReached(appContext, message);
                    if (user != null) {
                        smsSent = SmsHelper.sendCongrats(appContext, user.phoneNumber, message);
                    }
                }
            }
            if (callback != null) callback.onComplete(new AddResult(goalJustReached, smsSent));
        });
    }

    public void updateEntry(WeightEntry entry, Runnable onDone) {
        executor.execute(() -> { weightEntryDao.update(entry); if (onDone != null) onDone.run(); });
    }
    public void deleteEntry(int entryId, Runnable onDone) {
        executor.execute(() -> { weightEntryDao.deleteById(entryId); if (onDone != null) onDone.run(); });
    }
    public void getEntry(int entryId, Callback<WeightEntry> callback) {
        executor.execute(() -> { if (callback != null) callback.onComplete(weightEntryDao.getByIdSync(entryId)); });
    }

    /** Verifies the current password, then re-hashes and stores the new one. Returns true on success. */
    public void changePassword(int userId, String currentPassword, String newPassword, Callback<Boolean> callback) {
        executor.execute(() -> {
            boolean ok = false;
            User user = userDao.findById(userId);
            if (user != null && newPassword != null && newPassword.length() >= 6
                    && PasswordUtils.verify(currentPassword, user.salt, user.passwordHash)) {
                user.salt = PasswordUtils.generateSalt();
                user.passwordHash = PasswordUtils.hash(newPassword, user.salt);
                userDao.update(user);
                ok = true;
            }
            if (callback != null) callback.onComplete(ok);
        });
    }

    /** Deletes the account. Foreign-key cascade removes the profile and all weight rows. */
    public void deleteAccount(int userId, Runnable onDone) {
        executor.execute(() -> { userDao.deleteById(userId); if (onDone != null) onDone.run(); });
    }

    /** Sends a test message so the user can confirm SMS setup before the goal is reached. */
    public void sendTestMessage(int userId, Callback<Boolean> callback) {
        executor.execute(() -> {
            User user = userDao.findById(userId);
            String phone = user != null ? user.phoneNumber : "";
            String message = "Weight Tracking App: this is a test message. Your goal alerts are set up correctly.";
            boolean smsSent = SmsHelper.sendCongrats(appContext, phone, message);
            if (!smsSent) NotificationHelper.showGoalReached(appContext, "Test alert (SMS unavailable): notifications are working.");
            if (callback != null) callback.onComplete(smsSent);
        });
    }

    // ---------------- Summary + analytics ----------------

    public void buildSummary(int userId, Callback<WeightSummary> callback) {
        executor.execute(() -> {
            UserProfile profile = userProfileDao.getSync(userId);
            List<WeightEntry> ascending = weightEntryDao.getAllAscendingSync(userId);
            if (profile == null) profile = new UserProfile(userId);
            int entryCount = ascending.size();
            double starting = profile.startingWeightKg;
            double latest = entryCount > 0 ? ascending.get(ascending.size() - 1).weightKg : starting;
            if (starting <= 0 && entryCount > 0) starting = ascending.get(0).weightKg;
            double goal = profile.goalWeightKg;
            double bmi = profile.heightCm > 0 ? HealthUtils.calculateBmi(latest, profile.heightCm) : 0;
            String bmiCategory = HealthUtils.bmiCategory(bmi);
            double progressPercent = 0;
            if (starting > 0 && goal > 0 && starting != goal) {
                progressPercent = ((starting - latest) / (starting - goal)) * 100.0;
                progressPercent = Math.max(0, Math.min(100, progressPercent));
            }
            double changeFromStartKg = latest - starting;
            String milestone = HealthUtils.milestoneText(starting, latest, goal, profile.preferredUnit);

            double movingAvg = Analytics.movingAverageKg(ascending, Analytics.DEFAULT_WINDOW);
            double minKg = Analytics.minKg(ascending);
            double maxKg = Analytics.maxKg(ascending);
            double slope = Analytics.trendSlopeKgPerDay(ascending);
            String trend = Analytics.trendText(slope, profile.preferredUnit);
            double weeklyRateKg = slope * 7.0;

            // Project a goal date only when the trend is heading toward the goal.
            int projectedDays = -1;
            String projectedDate = "";
            if (goal > 0 && latest > goal && slope < 0 && entryCount >= 2) {
                double days = (goal - latest) / slope; // both negative -> positive
                if (days > 0 && days < 3650) {
                    projectedDays = (int) Math.ceil(days);
                    String from = ascending.get(ascending.size() - 1).entryDate;
                    String computed = DateUtils.plusDays(from, projectedDays);
                    if (computed != null) projectedDate = computed;
                }
            }

            if (callback != null) callback.onComplete(new WeightSummary(entryCount, starting, latest, goal, bmi,
                    bmiCategory, progressPercent, milestone, changeFromStartKg,
                    movingAvg, minKg, maxKg, slope, trend,
                    weeklyRateKg, projectedDays, projectedDate));
        });
    }

    public void exportEntriesCsv(int userId, Callback<String> callback) {
        executor.execute(() -> {
            try {
                List<WeightEntry> ascending = weightEntryDao.getAllAscendingSync(userId);
                UserProfile profile = userProfileDao.getSync(userId);
                File exportsDir = new File(appContext.getExternalFilesDir(null), "exports");
                if (!exportsDir.exists()) exportsDir.mkdirs();
                File out = new File(exportsDir, "weight_history.csv");
                try (FileWriter writer = new FileWriter(out, false)) {
                    writer.append("date,weight_kg,preferred_unit,notes\n");
                    String unit = profile != null ? profile.preferredUnit : "lb";
                    for (WeightEntry entry : ascending) {
                        writer.append(entry.entryDate).append(",")
                                .append(String.valueOf(entry.weightKg)).append(",")
                                .append(unit).append(",")
                                .append(entry.notes.replace(",", ";")).append("\n");
                    }
                }
                if (callback != null) callback.onComplete(out.getAbsolutePath());
            } catch (Exception ex) {
                if (callback != null) callback.onComplete(null);
            }
        });
    }
}
