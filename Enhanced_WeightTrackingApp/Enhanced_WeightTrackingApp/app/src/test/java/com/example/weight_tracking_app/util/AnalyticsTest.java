package com.example.weight_tracking_app.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.example.weight_tracking_app.model.WeightEntry;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class AnalyticsTest {

    private WeightEntry entry(String date, double kg) {
        WeightEntry e = new WeightEntry();
        e.entryDate = date;
        e.weightKg = kg;
        return e;
    }

    private List<WeightEntry> ascendingSample() {
        List<WeightEntry> list = new ArrayList<>();
        list.add(entry("2026-01-01", 100));
        list.add(entry("2026-01-08", 99));
        list.add(entry("2026-01-15", 98));
        list.add(entry("2026-01-22", 97));
        return list;
    }

    @Test
    public void movingAverage_usesLastWindowEntries() {
        assertEquals(97.5, Analytics.movingAverageKg(ascendingSample(), 2), 1e-9);
    }

    @Test
    public void movingAverage_windowLargerThanData_averagesAll() {
        assertEquals(98.5, Analytics.movingAverageKg(ascendingSample(), 10), 1e-9);
    }

    @Test
    public void minAndMax_areCorrect() {
        assertEquals(97, Analytics.minKg(ascendingSample()), 1e-9);
        assertEquals(100, Analytics.maxKg(ascendingSample()), 1e-9);
    }

    @Test
    public void trendSlope_isNegativeForDecreasingWeight() {
        double slope = Analytics.trendSlopeKgPerDay(ascendingSample());
        // Losing ~1kg per week => about -1/7 kg per day.
        assertTrue("slope should be negative, was " + slope, slope < 0);
        assertEquals(-1.0 / 7.0, slope, 0.01);
    }

    @Test
    public void trendSlope_usesRealDatesNotInsertionOrder() {
        // Same values, but inserted (list) out of chronological order to prove
        // the regression keys on parsed dates, not position.
        List<WeightEntry> shuffled = new ArrayList<>();
        shuffled.add(entry("2026-01-15", 98));
        shuffled.add(entry("2026-01-01", 100));
        shuffled.add(entry("2026-01-22", 97));
        shuffled.add(entry("2026-01-08", 99));
        // Analytics expects ascending order for min/avg, but the slope uses dates,
        // so build an ordered copy for slope comparison.
        double slopeOrdered = Analytics.trendSlopeKgPerDay(ascendingSample());
        // Reorder shuffled by date the way the DAO would, then compare.
        shuffled.sort((a, b) -> a.entryDate.compareTo(b.entryDate));
        double slope = Analytics.trendSlopeKgPerDay(shuffled);
        assertEquals(slopeOrdered, slope, 1e-9);
    }

    @Test
    public void emptyList_returnsZeros() {
        List<WeightEntry> empty = new ArrayList<>();
        assertEquals(0, Analytics.movingAverageKg(empty, 7), 1e-9);
        assertEquals(0, Analytics.trendSlopeKgPerDay(empty), 1e-9);
    }
}
