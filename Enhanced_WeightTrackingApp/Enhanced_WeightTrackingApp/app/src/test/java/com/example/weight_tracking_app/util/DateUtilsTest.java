package com.example.weight_tracking_app.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DateUtilsTest {

    @Test
    public void isValidDate_acceptsWellFormedDate() {
        assertTrue(DateUtils.isValidDate("2026-07-06"));
    }

    @Test
    public void isValidDate_rejectsGarbageAndBadMonths() {
        assertFalse(DateUtils.isValidDate("not-a-date"));
        assertFalse(DateUtils.isValidDate("2026-13-01"));
        assertFalse(DateUtils.isValidDate("07/06/2026"));
        assertFalse(DateUtils.isValidDate(null));
    }

    @Test
    public void toEpochDay_isOrderedAndConsistent() {
        Long earlier = DateUtils.toEpochDay("2026-01-01");
        Long later = DateUtils.toEpochDay("2026-01-08");
        assertNotNull(earlier);
        assertNotNull(later);
        assertEquals(7L, (long) (later - earlier));
    }

    @Test
    public void plusDays_advancesAcrossMonthBoundary() {
        assertEquals("2026-02-01", DateUtils.plusDays("2026-01-25", 7));
        assertEquals("2026-01-01", DateUtils.plusDays("2025-12-25", 7));
    }

    @Test
    public void toEpochDay_returnsNullForBadInput() {
        assertNull(DateUtils.toEpochDay("bad"));
    }
}
