package com.example.weight_tracking_app.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PasswordUtilsTest {

    @Test
    public void verify_succeedsForCorrectPassword() {
        String salt = PasswordUtils.generateSalt();
        String hash = PasswordUtils.hash("s3cret!", salt);
        assertTrue(PasswordUtils.verify("s3cret!", salt, hash));
    }

    @Test
    public void verify_failsForWrongPassword() {
        String salt = PasswordUtils.generateSalt();
        String hash = PasswordUtils.hash("s3cret!", salt);
        assertFalse(PasswordUtils.verify("wrong", salt, hash));
    }

    @Test
    public void hash_isNotThePlainTextPassword() {
        String salt = PasswordUtils.generateSalt();
        String hash = PasswordUtils.hash("password123", salt);
        assertNotEquals("password123", hash);
    }

    @Test
    public void differentSalts_produceDifferentHashesForSamePassword() {
        String hashA = PasswordUtils.hash("same", PasswordUtils.generateSalt());
        String hashB = PasswordUtils.hash("same", PasswordUtils.generateSalt());
        assertNotEquals(hashA, hashB);
    }

    @Test
    public void hash_isDeterministicForSameSalt() {
        String salt = PasswordUtils.generateSalt();
        assertEquals(PasswordUtils.hash("abc", salt), PasswordUtils.hash("abc", salt));
    }
}
