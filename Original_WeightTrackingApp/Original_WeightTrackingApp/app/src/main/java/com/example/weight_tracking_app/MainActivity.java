package com.example.weight_tracking_app;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Locale;

/**
 * MainActivity
 *
 * Project Three requirements implemented:
 *  1) Login (validate credentials against SQLite) + Create Account (insert into SQLite)
 *  2) Persistent SQLite "shell" with CRUD for daily weights, displayed as a grid-style list
 *  3) SMS permission prompt + app behavior based on grant/deny
 *  4) Best practices: clear naming, small methods, and inline comments
 */
@SuppressWarnings("SetTextI18n")
public class MainActivity extends AppCompatActivity {

    // ViewFlipper "screens"
    // 0 = Login, 1 = Dashboard, 2 = History, 3 = SMS
    private ViewFlipper viewFlipper;

    // Login views
    private EditText edtUsername, edtPassword;
    private Button btnLogin, btnCreateAccount;
    private TextView txtLoginStatus;

    // Dashboard views
    private TextView txtWelcome, txtCurrentWeight, txtGoalWeight, txtProgress, txtDashboardStatus;
    private EditText edtDate, edtWeight, edtGoal;
    private Button btnAddEntry, btnSaveGoal, btnGoHistory, btnGoSms, btnLogout;

    // History views
    private RecyclerView rvHistory;
    private Button btnBackFromHistory;
    private Button btnAddFromHistory;

    // SMS views
    private Button btnCheckSmsPermission, btnBackFromSms, btnSavePhone;
    private EditText edtPhone;
    private TextView txtSmsStatus;

    // Persistent DB + in-memory list used by RecyclerView
    private AppDatabaseHelper db;
    private final ArrayList<WeightEntry> weightEntries = new ArrayList<>();
    private WeightAdapter weightAdapter;

    // Logged in user session
    private long currentUserId = -1;
    private String currentUsername = "User";

    // Permissions
    private static final int REQ_SEND_SMS = 2001;

    // SharedPreferences (session convenience only)
    private static final String PREFS = "weight_tracking_prefs";
    private static final String KEY_LOGGED_IN_USER_ID = "logged_in_user_id";
    private static final String KEY_LOGGED_IN_USERNAME = "logged_in_username";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = new AppDatabaseHelper(this);

        bindViews();
        setupHistoryRecycler();
        wireUpClicks();
        refreshSmsStatusText();

        // If a user was previously logged in (optional convenience), resume session.
        restoreSessionOrShowLogin();
    }

    private void restoreSessionOrShowLogin() {
        SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
        long savedUserId = sp.getLong(KEY_LOGGED_IN_USER_ID, -1);
        String savedUsername = sp.getString(KEY_LOGGED_IN_USERNAME, null);

        if (savedUserId > 0 && !TextUtils.isEmpty(savedUsername)) {
            currentUserId = savedUserId;
            currentUsername = savedUsername;
            loadWeightsForCurrentUser();
            showScreen(1);
        } else {
            showScreen(0);
        }
    }

    private void bindViews() {
        viewFlipper = findViewById(R.id.viewFlipper);

        // Login
        edtUsername = findViewById(R.id.edtUsername);
        edtPassword = findViewById(R.id.edtPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnCreateAccount = findViewById(R.id.btnCreateAccount);
        txtLoginStatus = findViewById(R.id.txtLoginStatus);

        // Dashboard
        txtWelcome = findViewById(R.id.txtWelcome);
        txtCurrentWeight = findViewById(R.id.txtCurrentWeight);
        txtGoalWeight = findViewById(R.id.txtGoalWeight);
        txtProgress = findViewById(R.id.txtProgress);
        txtDashboardStatus = findViewById(R.id.txtDashboardStatus);

        edtDate = findViewById(R.id.edtDate);
        edtWeight = findViewById(R.id.edtWeight);
        edtGoal = findViewById(R.id.edtGoal);

        btnAddEntry = findViewById(R.id.btnAddEntry);
        btnSaveGoal = findViewById(R.id.btnSaveGoal);
        btnGoHistory = findViewById(R.id.btnGoHistory);
        btnGoSms = findViewById(R.id.btnGoSms);
        btnLogout = findViewById(R.id.btnLogout);

        // History
        rvHistory = findViewById(R.id.rvHistory);
        btnBackFromHistory = findViewById(R.id.btnBackFromHistory);
        btnAddFromHistory = findViewById(R.id.btnAddFromHistory);

        // SMS
        btnCheckSmsPermission = findViewById(R.id.btnCheckSmsPermission);
        btnBackFromSms = findViewById(R.id.btnBackFromSms);
        btnSavePhone = findViewById(R.id.btnSavePhone);
        edtPhone = findViewById(R.id.edtPhone);
        txtSmsStatus = findViewById(R.id.txtSmsStatus);
    }

    private void setupHistoryRecycler() {
        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        weightAdapter = new WeightAdapter(weightEntries);
        rvHistory.setAdapter(weightAdapter);
    }

    private void wireUpClicks() {
        // LOGIN
        btnCreateAccount.setOnClickListener(v -> handleCreateAccount());
        btnLogin.setOnClickListener(v -> handleLogin());

        // DASHBOARD
        btnAddEntry.setOnClickListener(v -> handleAddEntry());
        btnSaveGoal.setOnClickListener(v -> handleSaveGoal());
        btnGoHistory.setOnClickListener(v -> showScreen(2));
        btnGoSms.setOnClickListener(v -> showScreen(3));
        btnLogout.setOnClickListener(v -> handleLogout());

        // HISTORY
        btnBackFromHistory.setOnClickListener(v -> showScreen(1));
        btnAddFromHistory.setOnClickListener(v -> showScreen(1));

        // SMS
        btnBackFromSms.setOnClickListener(v -> showScreen(1));
        btnCheckSmsPermission.setOnClickListener(v -> handleCheckSmsPermission());
        btnSavePhone.setOnClickListener(v -> handleSavePhone());
    }

    // --------------------------
    // Screen Navigation
    // --------------------------
    private void showScreen(int index) {
        viewFlipper.setDisplayedChild(index);

        // Screen-specific refresh
        if (index == 1) {
            refreshDashboard();
        } else if (index == 3) {
            refreshSmsStatusText();
            preloadPhoneForScreen();
        }
    }

    // --------------------------
    // Login / Create Account
    // --------------------------
    private void handleCreateAccount() {
        String user = safeTrim(edtUsername);
        String pass = safeTrim(edtPassword);

        if (TextUtils.isEmpty(user) || TextUtils.isEmpty(pass)) {
            txtLoginStatus.setText("Create Account failed: username and password are required.");
            return;
        }

        long userId = db.createUser(user, pass);
        if (userId <= 0) {
            txtLoginStatus.setText("Account creation failed: username already exists.");
            return;
        }

        txtLoginStatus.setText("Account created successfully. You can now log in.");
        Toast.makeText(this, "Account created.", Toast.LENGTH_SHORT).show();
    }

    private void handleLogin() {
        String user = safeTrim(edtUsername);
        String pass = safeTrim(edtPassword);

        if (TextUtils.isEmpty(user) || TextUtils.isEmpty(pass)) {
            txtLoginStatus.setText("Login failed: username and password are required.");
            return;
        }

        long userId = db.validateLogin(user, pass);
        if (userId <= 0) {
            txtLoginStatus.setText("Login failed: incorrect username or password (or user not found).");
            return;
        }

        // Save session convenience
        currentUserId = userId;
        currentUsername = user;

        SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
        sp.edit()
                .putLong(KEY_LOGGED_IN_USER_ID, currentUserId)
                .putString(KEY_LOGGED_IN_USERNAME, currentUsername)
                .apply();

        txtLoginStatus.setText("");
        edtPassword.setText("");

        loadWeightsForCurrentUser();
        Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show();
        showScreen(1);
    }

    private void handleLogout() {
        SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
        sp.edit()
                .remove(KEY_LOGGED_IN_USER_ID)
                .remove(KEY_LOGGED_IN_USERNAME)
                .apply();

        currentUserId = -1;
        currentUsername = "User";

        // Clear UI fields
        edtPassword.setText("");
        txtDashboardStatus.setText("");
        weightEntries.clear();
        weightAdapter.notifyDataSetChanged();

        Toast.makeText(this, "Logged out.", Toast.LENGTH_SHORT).show();
        showScreen(0);
    }

    // --------------------------
    // Database: Read and Refresh
    // --------------------------
    private void loadWeightsForCurrentUser() {
        weightEntries.clear();
        if (currentUserId > 0) {
            weightEntries.addAll(db.getWeightsForUser(currentUserId));
        }
        weightAdapter.notifyDataSetChanged();
    }

    // --------------------------
    // Dashboard: Create + Goal
    // --------------------------
    private void handleAddEntry() {
        if (currentUserId <= 0) {
            txtDashboardStatus.setText("Please log in first.");
            return;
        }

        String date = safeTrim(edtDate);
        String weightStr = safeTrim(edtWeight);

        if (TextUtils.isEmpty(date) || TextUtils.isEmpty(weightStr)) {
            txtDashboardStatus.setText("Add failed: date and weight are required.");
            return;
        }

        double weightVal;
        try {
            weightVal = Double.parseDouble(weightStr);
        } catch (NumberFormatException ex) {
            txtDashboardStatus.setText("Add failed: weight must be a number.");
            return;
        }

        long newId = db.insertWeight(currentUserId, date, weightVal);
        if (newId <= 0) {
            txtDashboardStatus.setText("Add failed: could not insert into database.");
            return;
        }

        // Update UI list (insert at top because DB query sorts DESC)
        weightEntries.add(0, new WeightEntry(newId, date, weightVal));
        weightAdapter.notifyItemInserted(0);
        rvHistory.scrollToPosition(0);

        edtDate.setText("");
        edtWeight.setText("");
        txtDashboardStatus.setText("Added entry to history.");

        refreshDashboard();
        maybeNotifyGoalReached(weightVal);
    }

    private void handleSaveGoal() {
        if (currentUserId <= 0) {
            txtDashboardStatus.setText("Please log in first.");
            return;
        }

        String goalStr = safeTrim(edtGoal);
        if (TextUtils.isEmpty(goalStr)) {
            txtDashboardStatus.setText("Goal save failed: goal weight is required.");
            return;
        }

        double goalVal;
        try {
            goalVal = Double.parseDouble(goalStr);
        } catch (NumberFormatException ex) {
            txtDashboardStatus.setText("Goal save failed: goal must be a number.");
            return;
        }

        boolean ok = db.updateGoalWeight(currentUserId, goalVal);
        txtDashboardStatus.setText(ok ? "Goal saved." : "Goal save failed: database update error.");
        refreshDashboard();
    }

    private void refreshDashboard() {
        txtWelcome.setText("Welcome, " + currentUsername + "!");

        double goal = (currentUserId > 0) ? db.getGoalWeight(currentUserId) : -1;
        txtGoalWeight.setText(goal > 0 ? String.format(Locale.US, "Goal Weight: %.1f", goal) : "Goal Weight: —");

        if (weightEntries.isEmpty()) {
            txtCurrentWeight.setText("Current Weight: —");
            txtProgress.setText("Progress: —");
            return;
        }

        double latest = weightEntries.get(0).weight;
        txtCurrentWeight.setText(String.format(Locale.US, "Current Weight: %.1f", latest));

        if (goal > 0) {
            double diff = latest - goal;
            String progressText = (diff <= 0)
                    ? "Progress: Goal reached or below!"
                    : String.format(Locale.US, "Progress: %.1f lbs above goal", diff);
            txtProgress.setText(progressText);
        } else {
            txtProgress.setText("Progress: Set a goal to track progress.");
        }
    }

    // --------------------------
    // SMS Permission + Behavior
    // --------------------------
    private void handleCheckSmsPermission() {
        if (hasSmsPermission()) {
            Toast.makeText(this, "SMS permission already granted.", Toast.LENGTH_SHORT).show();
            refreshSmsStatusText();
            return;
        }

        // Ask for SEND_SMS permission. If denied, the rest of the app still functions.
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.SEND_SMS},
                REQ_SEND_SMS
        );
    }

    private boolean hasSmsPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void refreshSmsStatusText() {
        if (txtSmsStatus == null) return;
        txtSmsStatus.setText(hasSmsPermission()
                ? "SMS Permission Status: Granted ✅"
                : "SMS Permission Status: Not granted ❌");
    }

    private void preloadPhoneForScreen() {
        if (currentUserId <= 0 || edtPhone == null) return;
        String phone = db.getPhone(currentUserId);
        if (phone != null) edtPhone.setText(phone);
    }

    private void handleSavePhone() {
        if (currentUserId <= 0) {
            Toast.makeText(this, "Please log in first.", Toast.LENGTH_SHORT).show();
            return;
        }

        String phone = safeTrim(edtPhone);
        if (TextUtils.isEmpty(phone)) {
            Toast.makeText(this, "Please enter a phone number.", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean ok = db.updatePhone(currentUserId, phone);
        Toast.makeText(this, ok ? "Phone saved." : "Could not save phone.", Toast.LENGTH_SHORT).show();
    }

    /**
     * Trigger notification when user reaches their goal weight (weight <= goal).
     * If SMS permission is granted AND a phone is saved, we send an SMS.
     * If denied, we show a toast and keep the rest of the app functional.
     */
    private void maybeNotifyGoalReached(double latestWeight) {
        if (currentUserId <= 0) return;

        double goal = db.getGoalWeight(currentUserId);
        if (goal <= 0) return;

        if (latestWeight <= goal) {
            if (!hasSmsPermission()) {
                Toast.makeText(this,
                        "Goal reached! SMS permission not granted, so no text was sent.",
                        Toast.LENGTH_LONG).show();
                return;
            }

            String phone = db.getPhone(currentUserId);
            if (TextUtils.isEmpty(phone)) {
                Toast.makeText(this,
                        "Goal reached! Add a phone number on the SMS screen to receive texts.",
                        Toast.LENGTH_LONG).show();
                return;
            }

            // Send the SMS (best effort; emulator/device limitations may apply).
            try {
                String msg = "Weight Tracking App: Goal reached! Latest weight: " +
                        String.format(Locale.US, "%.1f", latestWeight) + " lbs.";
                SmsManager sms = SmsManager.getDefault();
                sms.sendTextMessage(phone, null, msg, null, null);

                Toast.makeText(this, "Goal reached! SMS sent.", Toast.LENGTH_LONG).show();
            } catch (Exception ex) {
                Toast.makeText(this,
                        "Goal reached, but SMS failed to send on this device/emulator.",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    // --------------------------
    // Permission callback
    // --------------------------
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQ_SEND_SMS) {
            boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;

            if (granted) {
                Toast.makeText(this, "SMS permission granted.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this,
                        "SMS permission denied. App will still function without SMS.",
                        Toast.LENGTH_LONG).show();
            }
            refreshSmsStatusText();
        }
    }

    private String safeTrim(EditText et) {
        if (et == null || et.getText() == null) return "";
        return et.getText().toString().trim();
    }

    // --------------------------
    // RecyclerView Adapter (Grid-like list with Update + Delete actions)
    // --------------------------
    private class WeightAdapter extends RecyclerView.Adapter<WeightAdapter.WeightVH> {

        private final ArrayList<WeightEntry> items;

        WeightAdapter(ArrayList<WeightEntry> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public WeightVH onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
            // Build a row programmatically (keeps project small; no extra XML needed).
            android.widget.LinearLayout row = new android.widget.LinearLayout(parent.getContext());
            row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
            row.setPadding(8, 16, 8, 16);

            TextView tvDate = new TextView(parent.getContext());
            tvDate.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                    0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 2));
            tvDate.setMaxLines(1);

            TextView tvWeight = new TextView(parent.getContext());
            tvWeight.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                    0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            tvWeight.setGravity(Gravity.START);

            // Action container so we can fit Update + Delete under the "Action" column.
            android.widget.LinearLayout actions = new android.widget.LinearLayout(parent.getContext());
            actions.setOrientation(android.widget.LinearLayout.HORIZONTAL);
            actions.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                    0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1));

            Button btnUpdate = new Button(parent.getContext());
            btnUpdate.setText("Update");
            btnUpdate.setAllCaps(false);

            Button btnDelete = new Button(parent.getContext());
            btnDelete.setText("Delete");
            btnDelete.setAllCaps(false);

            actions.addView(btnUpdate);
            actions.addView(btnDelete);

            row.addView(tvDate);
            row.addView(tvWeight);
            row.addView(actions);

            return new WeightVH(row, tvDate, tvWeight, btnUpdate, btnDelete);
        }

        @Override
        public void onBindViewHolder(@NonNull WeightVH holder, int position) {
            WeightEntry item = items.get(position);
            holder.tvDate.setText(item.date);
            holder.tvWeight.setText(String.format(Locale.US, "%.1f", item.weight));

            holder.btnDelete.setOnClickListener(v -> {
                int pos = holder.getBindingAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return;

                WeightEntry entry = items.get(pos);
                boolean ok = db.deleteWeight(entry.id);
                if (ok) {
                    items.remove(pos);
                    notifyItemRemoved(pos);
                    refreshDashboard();
                    Toast.makeText(MainActivity.this, "Row deleted.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Delete failed (DB error).", Toast.LENGTH_SHORT).show();
                }
            });

            holder.btnUpdate.setOnClickListener(v -> showUpdateDialog(holder.getBindingAdapterPosition()));
        }

        private void showUpdateDialog(int pos) {
            if (pos == RecyclerView.NO_POSITION) return;

            WeightEntry entry = items.get(pos);

            // Simple dialog with two fields (date + weight) for UPDATE requirement.
            View dialogView = getLayoutInflater().inflate(android.R.layout.simple_list_item_2, null);
            // We'll ignore the inflated layout contents and build our own for clarity:
            android.widget.LinearLayout container = new android.widget.LinearLayout(MainActivity.this);
            container.setOrientation(android.widget.LinearLayout.VERTICAL);
            container.setPadding(40, 20, 40, 10);

            EditText etDate = new EditText(MainActivity.this);
            etDate.setHint("Date (e.g., 2026-02-04)");
            etDate.setText(entry.date);

            EditText etWeight = new EditText(MainActivity.this);
            etWeight.setHint("Weight (lbs)");
            etWeight.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
            etWeight.setText(String.valueOf(entry.weight));

            container.addView(etDate);
            container.addView(etWeight);

            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Update Entry")
                    .setView(container)
                    .setPositiveButton("Save", (d, which) -> {
                        String newDate = etDate.getText().toString().trim();
                        String newWeightStr = etWeight.getText().toString().trim();

                        if (TextUtils.isEmpty(newDate) || TextUtils.isEmpty(newWeightStr)) {
                            Toast.makeText(MainActivity.this, "Update failed: date and weight required.", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        double newWeight;
                        try {
                            newWeight = Double.parseDouble(newWeightStr);
                        } catch (NumberFormatException ex) {
                            Toast.makeText(MainActivity.this, "Update failed: weight must be a number.", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        boolean ok = db.updateWeight(entry.id, newDate, newWeight);
                        if (ok) {
                            // Replace the item in-memory and refresh that row
                            items.set(pos, new WeightEntry(entry.id, newDate, newWeight));
                            notifyItemChanged(pos);
                            refreshDashboard();
                            Toast.makeText(MainActivity.this, "Row updated.", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(MainActivity.this, "Update failed (DB error).", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class WeightVH extends RecyclerView.ViewHolder {
            final TextView tvDate;
            final TextView tvWeight;
            final Button btnUpdate;
            final Button btnDelete;

            WeightVH(@NonNull View itemView, TextView tvDate, TextView tvWeight, Button btnUpdate, Button btnDelete) {
                super(itemView);
                this.tvDate = tvDate;
                this.tvWeight = tvWeight;
                this.btnUpdate = btnUpdate;
                this.btnDelete = btnDelete;
            }
        }
    }
}
