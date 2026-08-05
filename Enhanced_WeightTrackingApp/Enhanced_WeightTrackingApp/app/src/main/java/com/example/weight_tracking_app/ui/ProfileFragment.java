package com.example.weight_tracking_app.ui;

import android.Manifest;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.example.weight_tracking_app.R;
import com.example.weight_tracking_app.databinding.FragmentProfileBinding;
import com.example.weight_tracking_app.model.UserProfile;
import com.example.weight_tracking_app.notifications.NotificationHelper;
import com.example.weight_tracking_app.notifications.SmsHelper;
import com.example.weight_tracking_app.util.HealthUtils;

public class ProfileFragment extends Fragment {
    private FragmentProfileBinding binding;
    private WeightViewModel viewModel;
    private UserProfile profile;

    private final ActivityResultLauncher<String> smsPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                updateSmsStatus();
                if (granted) sendTestMessage();
                else Toast.makeText(requireContext(), "SMS permission denied — using notifications instead.", Toast.LENGTH_LONG).show();
            });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(requireActivity()).get(WeightViewModel.class);
        ArrayAdapter<CharSequence> unitAdapter = ArrayAdapter.createFromResource(requireContext(), R.array.weight_units, android.R.layout.simple_spinner_item);
        unitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerUnit.setAdapter(unitAdapter);
        ArrayAdapter<CharSequence> genderAdapter = ArrayAdapter.createFromResource(requireContext(), R.array.gender_options, android.R.layout.simple_spinner_item);
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerGender.setAdapter(genderAdapter);

        binding.buttonSaveProfile.setOnClickListener(v -> saveProfile());
        binding.buttonSendTest.setOnClickListener(v -> onSendTestClicked());
        binding.buttonChangePassword.setOnClickListener(v -> changePassword());
        binding.buttonDeleteAccount.setOnClickListener(v -> confirmDeleteAccount());
        binding.buttonLogout.setOnClickListener(v -> {
            viewModel.logout();
            NavHostFragment.findNavController(this).navigate(R.id.action_global_logout);
        });

        viewModel.getProfile().observe(getViewLifecycleOwner(), this::bindProfile);
        viewModel.getCurrentUser().observe(getViewLifecycleOwner(), user -> {
            if (user != null && TextUtils.isEmpty(binding.editPhone.getText())) {
                binding.editPhone.setText(user.phoneNumber);
            }
        });
        updateSmsStatus();
    }

    private void updateSmsStatus() {
        if (binding == null) return;
        boolean granted = SmsHelper.canSendSms(requireContext());
        String phone = binding.editPhone.getText().toString().trim();
        boolean validPhone = SmsHelper.isValidNumber(phone);
        String status;
        if (granted && validPhone) status = "SMS alerts: ready ✓";
        else if (!validPhone) status = "SMS alerts: add a valid phone number above";
        else status = "SMS alerts: permission needed (tap test to grant)";
        binding.textSmsStatus.setText(status);
    }

    private void onSendTestClicked() {
        updateSmsStatus();
        if (!SmsHelper.isValidNumber(binding.editPhone.getText().toString().trim())) {
            Toast.makeText(requireContext(), "Enter a valid phone number first, then Save profile.", Toast.LENGTH_LONG).show();
            return;
        }
        // Persist the current phone so the test uses it, then send.
        viewModel.updatePhone(binding.editPhone.getText().toString().trim(), null);
        if (SmsHelper.canSendSms(requireContext())) {
            sendTestMessage();
        } else {
            smsPermissionLauncher.launch(Manifest.permission.SEND_SMS);
        }
    }

    private void sendTestMessage() {
        viewModel.sendTestMessage(smsSent -> requireActivity().runOnUiThread(() ->
                Toast.makeText(requireContext(),
                        smsSent ? "Test SMS sent." : "SMS unavailable — sent a test notification instead.",
                        Toast.LENGTH_LONG).show()));
    }

    private void changePassword() {
        String current = binding.editCurrentPassword.getText().toString();
        String next = binding.editNewPassword.getText().toString();
        if (TextUtils.isEmpty(current) || TextUtils.isEmpty(next)) {
            binding.textError.setText("Enter your current and new password.");
            return;
        }
        if (next.length() < 6) {
            binding.textError.setText("New password must be at least 6 characters.");
            return;
        }
        binding.textError.setText("");
        viewModel.changePassword(current, next, ok -> requireActivity().runOnUiThread(() -> {
            if (ok) {
                binding.editCurrentPassword.setText("");
                binding.editNewPassword.setText("");
                Toast.makeText(requireContext(), "Password changed.", Toast.LENGTH_SHORT).show();
            } else {
                binding.textError.setText("Current password is incorrect.");
            }
        }));
    }

    private void confirmDeleteAccount() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete account")
                .setMessage("This permanently deletes your account and all your weight entries. Continue?")
                .setPositiveButton("Delete", (d, w) -> viewModel.deleteAccount(() -> requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Account deleted.", Toast.LENGTH_SHORT).show();
                    NavHostFragment.findNavController(this).navigate(R.id.action_global_logout);
                })))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void bindProfile(UserProfile userProfile) {
        if (userProfile == null) return;
        profile = userProfile;
        binding.editName.setText(userProfile.name);
        binding.editAge.setText(userProfile.age > 0 ? String.valueOf(userProfile.age) : "");
        binding.editHeightCm.setText(userProfile.heightCm > 0 ? String.valueOf(userProfile.heightCm) : "");
        binding.editStartingWeight.setText(userProfile.startingWeightKg > 0 ? String.valueOf(HealthUtils.fromKg(userProfile.startingWeightKg, userProfile.preferredUnit)) : "");
        binding.editGoalWeight.setText(userProfile.goalWeightKg > 0 ? String.valueOf(HealthUtils.fromKg(userProfile.goalWeightKg, userProfile.preferredUnit)) : "");
        binding.switchReminders.setChecked(userProfile.remindersEnabled);
        binding.editReminderHour.setText(String.valueOf(userProfile.reminderHour));
        binding.editReminderMinute.setText(String.valueOf(userProfile.reminderMinute));
        binding.spinnerUnit.setSelection("kg".equalsIgnoreCase(userProfile.preferredUnit) ? 1 : 0);
        if (!TextUtils.isEmpty(userProfile.gender)) {
            String[] genders = getResources().getStringArray(R.array.gender_options);
            for (int i = 0; i < genders.length; i++) if (genders[i].equalsIgnoreCase(userProfile.gender)) binding.spinnerGender.setSelection(i);
        }
    }

    private void saveProfile() {
        if (profile == null) profile = new UserProfile();
        String name = binding.editName.getText().toString().trim();
        String ageText = binding.editAge.getText().toString().trim();
        String heightText = binding.editHeightCm.getText().toString().trim();
        String startingText = binding.editStartingWeight.getText().toString().trim();
        String goalText = binding.editGoalWeight.getText().toString().trim();
        String unit = binding.spinnerUnit.getSelectedItem().toString();
        String gender = binding.spinnerGender.getSelectedItem().toString();
        String hourText = binding.editReminderHour.getText().toString().trim();
        String minuteText = binding.editReminderMinute.getText().toString().trim();
        String phone = binding.editPhone.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(heightText) || TextUtils.isEmpty(startingText) || TextUtils.isEmpty(goalText)) {
            binding.textError.setText("Name, height, starting weight, and goal weight are required.");
            return;
        }
        try {
            int age = TextUtils.isEmpty(ageText) ? 0 : Integer.parseInt(ageText);
            double heightCm = Double.parseDouble(heightText);
            double starting = Double.parseDouble(startingText);
            double goal = Double.parseDouble(goalText);
            int hour = TextUtils.isEmpty(hourText) ? 8 : Integer.parseInt(hourText);
            int minute = TextUtils.isEmpty(minuteText) ? 0 : Integer.parseInt(minuteText);
            if (heightCm <= 0 || starting <= 0 || goal <= 0) { binding.textError.setText("Height and weights must be > 0."); return; }
            if (hour < 0 || hour > 23 || minute < 0 || minute > 59) { binding.textError.setText("Reminder time must be valid."); return; }
            profile.name = name;
            profile.age = age;
            profile.heightCm = heightCm;
            profile.gender = gender;
            profile.preferredUnit = unit;
            profile.startingWeightKg = HealthUtils.toKg(starting, unit);
            profile.goalWeightKg = HealthUtils.toKg(goal, unit);
            profile.remindersEnabled = binding.switchReminders.isChecked();
            profile.reminderHour = hour;
            profile.reminderMinute = minute;
            binding.textError.setText("");
            viewModel.updatePhone(phone, null);
            viewModel.saveProfile(profile, () -> requireActivity().runOnUiThread(() -> {
                if (profile.remindersEnabled) NotificationHelper.scheduleDailyReminder(requireContext(), profile.reminderHour, profile.reminderMinute);
                else NotificationHelper.cancelDailyReminder(requireContext());
                updateSmsStatus();
                Toast.makeText(requireContext(), "Profile saved", Toast.LENGTH_SHORT).show();
            }));
        } catch (NumberFormatException ex) { binding.textError.setText("Please enter valid numeric values."); }
    }

    @Override
    public void onDestroyView() { super.onDestroyView(); binding = null; }
}
