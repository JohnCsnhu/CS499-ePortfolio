package com.example.weight_tracking_app.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.weight_tracking_app.R;
import com.example.weight_tracking_app.databinding.FragmentInsightsBinding;
import com.example.weight_tracking_app.model.UserProfile;
import com.example.weight_tracking_app.model.WeightSummary;
import com.example.weight_tracking_app.util.HealthUtils;

import java.util.Locale;

public class InsightsFragment extends Fragment {
    private FragmentInsightsBinding binding;
    private WeightViewModel viewModel;
    private UserProfile profile;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentInsightsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(requireActivity()).get(WeightViewModel.class);
        viewModel.getProfile().observe(getViewLifecycleOwner(), p -> { profile = p; });
        viewModel.getSummary().observe(getViewLifecycleOwner(), this::render);
    }

    private void render(WeightSummary s) {
        if (s == null) return;
        String unit = profile != null ? profile.preferredUnit : "lb";

        if (s.entryCount == 0) {
            binding.textEntriesTracked.setText("0");
            binding.textMovingAvg.setText(getString(R.string.not_set));
            binding.textMinMax.setText(getString(R.string.not_set));
            binding.textWeeklyRate.setText(getString(R.string.not_set));
            binding.textTrendLine.setText("Add entries to see your trend.");
            binding.textProjection.setText("Log at least two entries to project a goal date.");
            return;
        }

        binding.textEntriesTracked.setText(String.valueOf(s.entryCount));
        binding.textMovingAvg.setText(HealthUtils.formatWeight(HealthUtils.fromKg(s.movingAverageKg, unit), unit));
        binding.textMinMax.setText(String.format(Locale.US, "%s – %s",
                HealthUtils.formatWeight(HealthUtils.fromKg(s.minWeightKg, unit), unit),
                HealthUtils.formatWeight(HealthUtils.fromKg(s.maxWeightKg, unit), unit)));

        double weekly = HealthUtils.fromKg(Math.abs(s.weeklyRateKg), unit);
        String dir = s.weeklyRateKg < -0.0005 ? "losing" : (s.weeklyRateKg > 0.0005 ? "gaining" : "holding");
        binding.textWeeklyRate.setText("holding".equals(dir)
                ? "Holding steady"
                : String.format(Locale.US, "%s %.2f %s/week", dir, weekly, unit));

        binding.textTrendLine.setText(s.trendText);

        if (s.projectedDaysToGoal > 0 && !s.projectedGoalDate.isEmpty()) {
            binding.textProjection.setText(String.format(Locale.US,
                    "At this rate you'll reach your goal around %s (~%d days).",
                    s.projectedGoalDate, s.projectedDaysToGoal));
        } else if (s.goalWeightKg > 0 && s.latestWeightKg <= s.goalWeightKg) {
            binding.textProjection.setText("You've reached your goal. 🎉");
        } else {
            binding.textProjection.setText("No downward trend yet — keep logging to project a goal date.");
        }
    }

    @Override
    public void onDestroyView() { super.onDestroyView(); binding = null; }
}
