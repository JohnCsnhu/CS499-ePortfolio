package com.example.weight_tracking_app.model;

public class WeightSummary {
    public final int entryCount;
    public final double startingWeightKg;
    public final double latestWeightKg;
    public final double goalWeightKg;
    public final double bmi;
    public final String bmiCategory;
    public final double progressPercent;
    public final String milestoneText;
    public final double changeFromStartKg;
    // Analytics
    public final double movingAverageKg;
    public final double minWeightKg;
    public final double maxWeightKg;
    public final double trendSlopeKgPerDay;
    public final String trendText;
    // Projection
    public final double weeklyRateKg;      // signed kg/week from the regression
    public final int projectedDaysToGoal;  // -1 when not projectable
    public final String projectedGoalDate; // "" when not projectable

    public WeightSummary(int entryCount, double startingWeightKg, double latestWeightKg, double goalWeightKg,
                         double bmi, String bmiCategory, double progressPercent, String milestoneText,
                         double changeFromStartKg, double movingAverageKg, double minWeightKg,
                         double maxWeightKg, double trendSlopeKgPerDay, String trendText,
                         double weeklyRateKg, int projectedDaysToGoal, String projectedGoalDate) {
        this.entryCount = entryCount;
        this.startingWeightKg = startingWeightKg;
        this.latestWeightKg = latestWeightKg;
        this.goalWeightKg = goalWeightKg;
        this.bmi = bmi;
        this.bmiCategory = bmiCategory;
        this.progressPercent = progressPercent;
        this.milestoneText = milestoneText;
        this.changeFromStartKg = changeFromStartKg;
        this.movingAverageKg = movingAverageKg;
        this.minWeightKg = minWeightKg;
        this.maxWeightKg = maxWeightKg;
        this.trendSlopeKgPerDay = trendSlopeKgPerDay;
        this.trendText = trendText;
        this.weeklyRateKg = weeklyRateKg;
        this.projectedDaysToGoal = projectedDaysToGoal;
        this.projectedGoalDate = projectedGoalDate;
    }
}
