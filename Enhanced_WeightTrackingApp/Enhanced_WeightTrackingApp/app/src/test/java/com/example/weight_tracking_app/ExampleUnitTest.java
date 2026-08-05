package com.example.weight_tracking_app;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import com.example.weight_tracking_app.util.HealthUtils;
import org.junit.Test;
public class ExampleUnitTest {
@Test public void bmi_isCalculated() { double bmi = HealthUtils.calculateBmi(90, 180); assertTrue(bmi > 27); }
@Test public void conversion_roundTrips() { double kg = HealthUtils.toKg(220.0, "lb"); double lb = HealthUtils.fromKg(kg, "lb"); assertEquals(220.0, lb, 0.2); }
}