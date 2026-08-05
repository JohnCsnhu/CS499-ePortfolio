package com.example.weight_tracking_app.util;

import java.util.Locale;

public final class HealthUtils {
    private static final double POUNDS_PER_KG = 2.2046226218;
    private HealthUtils() {}
    public static double toKg(double value, String unit) { return "lb".equalsIgnoreCase(unit) ? value / POUNDS_PER_KG : value; }
    public static double fromKg(double value, String unit) { return "lb".equalsIgnoreCase(unit) ? value * POUNDS_PER_KG : value; }
    public static String formatWeight(double value, String unit) { return String.format(Locale.US, "%.1f %s", value, unit); }
    public static double calculateBmi(double weightKg, double heightCm) {
        double heightM = heightCm / 100.0;
        if (heightM <= 0) return 0;
        return weightKg / (heightM * heightM);
    }
    public static String bmiCategory(double bmi) {
        if (bmi <= 0) return "Unknown";
        if (bmi < 18.5) return "Underweight";
        if (bmi < 25) return "Healthy";
        if (bmi < 30) return "Overweight";
        return "Obese";
    }
    public static String milestoneText(double startKg, double currentKg, double goalKg, String displayUnit) {
        double lostKg = Math.max(0, startKg - currentKg);
        double lostInDisplay = fromKg(lostKg, displayUnit);
        if (goalKg > 0 && currentKg <= goalKg) return "Goal reached 🎉";
        if (lostInDisplay >= 25) return "Amazing: 25+ " + displayUnit + " lost";
        if (lostInDisplay >= 10) return "Milestone: 10+ " + displayUnit + " lost";
        if (lostInDisplay >= 5) return "Milestone: 5+ " + displayUnit + " lost";
        return "Keep going — your next milestone is 5 " + displayUnit;
    }
}
