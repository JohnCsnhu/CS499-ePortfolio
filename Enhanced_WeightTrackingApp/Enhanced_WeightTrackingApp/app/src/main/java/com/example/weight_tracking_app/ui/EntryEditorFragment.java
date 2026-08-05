package com.example.weight_tracking_app.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.example.weight_tracking_app.databinding.FragmentEntryEditorBinding;
import com.example.weight_tracking_app.model.UserProfile;
import com.example.weight_tracking_app.model.WeightEntry;
import com.example.weight_tracking_app.util.DateUtils;
import com.example.weight_tracking_app.util.HealthUtils;

public class EntryEditorFragment extends Fragment {
    // Realistic bounds for a body-weight entry, expressed in the display unit.
    private static final double MAX_WEIGHT_LB = 1500;
    private static final double MAX_WEIGHT_KG = 700;

    private FragmentEntryEditorBinding binding;
    private WeightViewModel viewModel;
    private int entryId = 0;
    private UserProfile profile;
    private WeightEntry editingEntry;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentEntryEditorBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(requireActivity()).get(WeightViewModel.class);
        entryId = getArguments() != null ? getArguments().getInt("entryId", 0) : 0;
        binding.editEntryDate.setText(DateUtils.today());
        binding.buttonSaveEntry.setOnClickListener(v -> saveEntry());
        viewModel.getProfile().observe(getViewLifecycleOwner(), userProfile -> profile = userProfile);
        if (entryId > 0) {
            binding.textTitle.setText("Edit weight entry");
            viewModel.getEntry(entryId, entry -> requireActivity().runOnUiThread(() -> bindEntry(entry)));
        }
    }

    private void bindEntry(WeightEntry entry) {
        if (entry == null) return;
        editingEntry = entry;
        binding.editEntryDate.setText(entry.entryDate);
        binding.editWeight.setText(String.valueOf(HealthUtils.fromKg(entry.weightKg, profile != null ? profile.preferredUnit : "lb")));
        binding.editNotes.setText(entry.notes);
    }

    private void saveEntry() {
        String date = binding.editEntryDate.getText().toString().trim();
        String weightText = binding.editWeight.getText().toString().trim();
        String notes = binding.editNotes.getText().toString().trim();
        String unit = profile != null ? profile.preferredUnit : "lb";
        double maxAllowed = "kg".equalsIgnoreCase(unit) ? MAX_WEIGHT_KG : MAX_WEIGHT_LB;

        if (!DateUtils.isValidDate(date)) { binding.textError.setText("Use yyyy-MM-dd."); return; }
        if (TextUtils.isEmpty(weightText)) { binding.textError.setText("Weight is required."); return; }
        try {
            double weight = Double.parseDouble(weightText);
            if (weight <= 0) { binding.textError.setText("Weight must be greater than zero."); return; }
            if (weight > maxAllowed) {
                binding.textError.setText(String.format(java.util.Locale.US,
                        "Enter a realistic weight (0–%.0f %s).", maxAllowed, unit));
                return;
            }
            binding.textError.setText("");
            WeightEntry target = editingEntry == null ? new WeightEntry() : editingEntry;
            target.entryDate = date;
            target.weightKg = HealthUtils.toKg(weight, unit);
            target.notes = notes;
            Runnable finish = () -> requireActivity().runOnUiThread(() -> { Toast.makeText(requireContext(), "Entry saved", Toast.LENGTH_SHORT).show(); NavHostFragment.findNavController(this).navigateUp(); });
            if (editingEntry == null) viewModel.addEntry(target, finish); else viewModel.updateEntry(target, finish);
        } catch (NumberFormatException ex) { binding.textError.setText("Enter a valid number."); }
    }

    @Override
    public void onDestroyView() { super.onDestroyView(); binding = null; }
}
