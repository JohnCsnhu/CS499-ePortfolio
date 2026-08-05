package com.example.weight_tracking_app.util;

import com.example.weight_tracking_app.model.WeightEntry;

import java.util.List;

/**
 * Lightweight analytics over a user's weight history. Entries are expected in
 * ascending chronological order (oldest first). All weights are in kilograms.
 */
public final class Analytics {
    /** Default window (number of most-recent entries) for the moving average. */
    public static final int DEFAULT_WINDOW = 7;
    private static final double TREND_TOLERANCE_KG_PER_DAY = 0.005; // ~flat threshold

    private Analytics() {}

    /** Simple moving average over the last {@code window} entries. */
    public static double movingAverageKg(List<WeightEntry> ascending, int window) {
        if (ascending == null || ascending.isEmpty()) return 0;
        int count = Math.min(window, ascending.size());
        double sum = 0;
        for (int i = ascending.size() - count; i < ascending.size(); i++) {
            sum += ascending.get(i).weightKg;
        }
        return sum / count;
    }

    public static double minKg(List<WeightEntry> ascending) {
        if (ascending == null || ascending.isEmpty()) return 0;
        double min = Double.MAX_VALUE;
        for (WeightEntry e : ascending) min = Math.min(min, e.weightKg);
        return min;
    }

    public static double maxKg(List<WeightEntry> ascending) {
        if (ascending == null || ascending.isEmpty()) return 0;
        double max = -Double.MAX_VALUE;
        for (WeightEntry e : ascending) max = Math.max(max, e.weightKg);
        return max;
    }

    /**
     * Least-squares linear regression of weight against time. X is the number of
     * days since the first entry (from the parsed date, not row order), Y is kg.
     * Returns the slope in kg/day; 0 if the trend can't be computed.
     */
    public static double trendSlopeKgPerDay(List<WeightEntry> ascending) {
        if (ascending == null || ascending.size() < 2) return 0;
        Long baseDay = DateUtils.toEpochDay(ascending.get(0).entryDate);
        if (baseDay == null) return 0;
        int n = 0;
        double sumX = 0, sumY = 0, sumXY = 0, sumXX = 0;
        for (WeightEntry e : ascending) {
            Long day = DateUtils.toEpochDay(e.entryDate);
            if (day == null) continue;
            double x = day - baseDay;
            double y = e.weightKg;
            sumX += x; sumY += y; sumXY += x * y; sumXX += x * x;
            n++;
        }
        if (n < 2) return 0;
        double denominator = (n * sumXX) - (sumX * sumX);
        if (Math.abs(denominator) < 1e-9) return 0;
        return ((n * sumXY) - (sumX * sumY)) / denominator;
    }

    /** Human-readable trend direction based on the regression slope. */
    public static String trendText(double slopeKgPerDay, String displayUnit) {
        if (Math.abs(slopeKgPerDay) < TREND_TOLERANCE_KG_PER_DAY) return "Trend: holding steady";
        double perWeek = Math.abs(HealthUtils.fromKg(slopeKgPerDay * 7.0, displayUnit));
        String direction = slopeKgPerDay < 0 ? "down" : "up";
        return String.format(java.util.Locale.US, "Trend: %.2f %s/week %s", perWeek, displayUnit, direction);
    }
}
