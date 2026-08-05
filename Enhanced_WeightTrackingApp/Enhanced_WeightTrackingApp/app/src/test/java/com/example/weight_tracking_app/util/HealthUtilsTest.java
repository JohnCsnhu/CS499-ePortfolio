package com.example.weight_tracking_app.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class HealthUtilsTest {

    @Test
    public void kgLbRoundTrip_isStable() {
        double kg = 80.0;
        double lb = HealthUtils.fromKg(kg, "lb");
        assertEquals(kg, HealthUtils.toKg(lb, "lb"), 1e-6);
    }

    @Test
    public void kgUnit_isIdentity() {
        assertEquals(72.5, HealthUtils.toKg(72.5, "kg"), 1e-9);
        assertEquals(72.5, HealthUtils.fromKg(72.5, "kg"), 1e-9);
    }

    @Test
    public void bmiCategory_bucketsCorrectly() {
        assertEquals("Underweight", HealthUtils.bmiCategory(17));
        assertEquals("Healthy", HealthUtils.bmiCategory(22));
        assertEquals("Overweight", HealthUtils.bmiCategory(27));
        assertEquals("Obese", HealthUtils.bmiCategory(33));
    }

    @Test
    public void calculateBmi_isCorrect() {
        // 80kg at 200cm => BMI 20.0
        assertEquals(20.0, HealthUtils.calculateBmi(80, 200), 1e-6);
    }
}
