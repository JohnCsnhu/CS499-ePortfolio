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

import com.example.weight_tracking_app.R;
import com.example.weight_tracking_app.databinding.FragmentDashboardBinding;
import com.example.weight_tracking_app.model.UserProfile;
import com.example.weight_tracking_app.model.WeightSummary;
import com.example.weight_tracking_app.util.HealthUtils;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DashboardFragment extends Fragment {
    private FragmentDashboardBinding binding;
    private WeightViewModel viewModel;
    private UserProfile profile;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(requireActivity()).get(WeightViewModel.class);
        binding.buttonAddEntry.setOnClickListener(v -> NavHostFragment.findNavController(this).navigate(R.id.action_dashboardFragment_to_entryEditorFragment));
        binding.buttonEditProfile.setOnClickListener(v -> NavHostFragment.findNavController(this).navigate(R.id.action_dashboardFragment_to_profileFragment));
        binding.swipeRefresh.setOnRefreshListener(() -> { viewModel.refreshSummary(); binding.swipeRefresh.setRefreshing(false); });

        viewModel.getProfile().observe(getViewLifecycleOwner(), userProfile -> { profile = userProfile; renderProfile(userProfile); });
        viewModel.getSummary().observe(getViewLifecycleOwner(), this::renderSummary);
        viewModel.getEntries().observe(getViewLifecycleOwner(), entries -> drawChart(entries, profile != null ? profile.preferredUnit : "lb"));
        viewModel.getEvents().observe(getViewLifecycleOwner(), message -> {
            if (!TextUtils.isEmpty(message)) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
                viewModel.clearEvent();
            }
        });
    }

    private void renderProfile(UserProfile userProfile) {
        if (userProfile == null) return;
        binding.textGreeting.setText(getString(R.string.dashboard_greeting, TextUtils.isEmpty(userProfile.name) ? "there" : userProfile.name));
        binding.textPreferredUnit.setText(getString(R.string.preferred_unit_value, userProfile.preferredUnit));
    }

    private void renderSummary(WeightSummary summary) {
        if (summary == null) return;
        String unit = profile != null ? profile.preferredUnit : "lb";
        binding.textCurrentWeight.setText(HealthUtils.formatWeight(HealthUtils.fromKg(summary.latestWeightKg, unit), unit));
        binding.textGoalWeight.setText(summary.goalWeightKg > 0 ? HealthUtils.formatWeight(HealthUtils.fromKg(summary.goalWeightKg, unit), unit) : getString(R.string.not_set));
        binding.textStartingWeight.setText(summary.startingWeightKg > 0 ? HealthUtils.formatWeight(HealthUtils.fromKg(summary.startingWeightKg, unit), unit) : getString(R.string.not_set));
        binding.textBmi.setText(summary.bmi > 0 ? String.format(Locale.US, "%.1f (%s)", summary.bmi, summary.bmiCategory) : getString(R.string.not_set));
        binding.textProgressPercent.setText(String.format(Locale.US, "%.0f%%", summary.progressPercent));
        binding.progressGoal.setProgress((int) summary.progressPercent);
        binding.textMilestone.setText(summary.milestoneText);
        binding.textEntriesCount.setText(summary.entryCount + " entries");
        double change = HealthUtils.fromKg(Math.abs(summary.changeFromStartKg), unit);
        String prefix = summary.changeFromStartKg <= 0 ? "Down" : "Up";
        binding.textChangeFromStart.setText(String.format(Locale.US, "%s %.1f %s", prefix, change, unit));

        // Analytics card
        if (summary.entryCount > 0) {
            binding.textMovingAverage.setText(String.format(Locale.US, "%s (last %d)",
                    HealthUtils.formatWeight(HealthUtils.fromKg(summary.movingAverageKg, unit), unit),
                    Math.min(summary.entryCount, com.example.weight_tracking_app.util.Analytics.DEFAULT_WINDOW)));
            binding.textMinMax.setText(String.format(Locale.US, "%s – %s",
                    HealthUtils.formatWeight(HealthUtils.fromKg(summary.minWeightKg, unit), unit),
                    HealthUtils.formatWeight(HealthUtils.fromKg(summary.maxWeightKg, unit), unit)));
            binding.textTrend.setText(summary.trendText);
        } else {
            binding.textMovingAverage.setText(getString(R.string.not_set));
            binding.textMinMax.setText(getString(R.string.not_set));
            binding.textTrend.setText("Trend: add entries to see it");
        }
    }

    private void drawChart(List<com.example.weight_tracking_app.model.WeightEntry> entries, String unit) {
        List<Entry> points = new ArrayList<>();
        if (entries != null) {
            int x = 0;
            for (int i = entries.size() - 1; i >= 0; i--) {
                points.add(new Entry(x++, (float) HealthUtils.fromKg(entries.get(i).weightKg, unit)));
            }
        }
        LineDataSet dataSet = new LineDataSet(points, "Weight");
        dataSet.setCircleRadius(4f);
        dataSet.setLineWidth(2f);
        dataSet.setDrawValues(false);
        binding.lineChart.setData(new LineData(dataSet));
        Description d = new Description();
        d.setText("");
        binding.lineChart.setDescription(d);
        binding.lineChart.getAxisRight().setEnabled(false);
        binding.lineChart.getXAxis().setGranularity(1f);
        binding.lineChart.invalidate();
    }

    @Override
    public void onDestroyView() { super.onDestroyView(); binding = null; }
}
