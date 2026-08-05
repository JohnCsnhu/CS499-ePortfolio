package com.example.weight_tracking_app;

/**
 * Small data model used by RecyclerView grid.
 */
public class WeightEntry {
    public final long id;
    public final String date;
    public final double weight;

    public WeightEntry(long id, String date, double weight) {
        this.id = id;
        this.date = date;
        this.weight = weight;
    }
}
