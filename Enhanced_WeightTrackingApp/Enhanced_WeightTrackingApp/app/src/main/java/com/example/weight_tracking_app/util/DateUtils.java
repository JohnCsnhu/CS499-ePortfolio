package com.example.weight_tracking_app.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public final class DateUtils {
    private static final String PATTERN = "yyyy-MM-dd";
    private DateUtils() {}

    private static SimpleDateFormat formatter() {
        SimpleDateFormat fmt = new SimpleDateFormat(PATTERN, Locale.US);
        fmt.setLenient(false);
        return fmt;
    }

    public static String today() { return formatter().format(new Date()); }

    public static boolean isValidDate(String text) {
        if (text == null) return false;
        try {
            formatter().parse(text.trim());
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    /** Returns the yyyy-MM-dd string {@code days} after the given date, or null. */
    public static String plusDays(String isoDate, long days) {
        try {
            Date base = formatter().parse(isoDate.trim());
            if (base == null) return null;
            long ms = base.getTime() + TimeUnit.DAYS.toMillis(days);
            return formatter().format(new Date(ms));
        } catch (Exception ex) {
            return null;
        }
    }

    /** Days since the Unix epoch for a yyyy-MM-dd date, or null if unparseable. */
    public static Long toEpochDay(String text) {
        if (text == null) return null;
        try {
            Date date = formatter().parse(text.trim());
            if (date == null) return null;
            return TimeUnit.MILLISECONDS.toDays(date.getTime());
        } catch (Exception ex) {
            return null;
        }
    }
}
