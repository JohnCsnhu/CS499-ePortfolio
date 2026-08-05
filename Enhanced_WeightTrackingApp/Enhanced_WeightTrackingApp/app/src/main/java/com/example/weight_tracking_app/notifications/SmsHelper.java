package com.example.weight_tracking_app.notifications;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.telephony.SmsManager;

import androidx.core.content.ContextCompat;

/** Sends the goal-reached congratulations text, honoring least-privilege. */
public final class SmsHelper {
    private SmsHelper() {}

    /** True if the app currently holds SEND_SMS permission. */
    public static boolean canSendSms(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS)
                == PackageManager.PERMISSION_GRANTED;
    }

    /** Basic sanity check: at least 7 digits after stripping formatting. */
    public static boolean isValidNumber(String phoneNumber) {
        if (phoneNumber == null) return false;
        String digits = phoneNumber.replaceAll("[^0-9]", "");
        return digits.length() >= 7 && digits.length() <= 15;
    }

    /**
     * Sends {@code message} to {@code phoneNumber}. Degrades gracefully: if the
     * number is invalid, permission is missing, or telephony is unavailable, it
     * simply returns false instead of throwing.
     */
    public static boolean sendCongrats(Context context, String phoneNumber, String message) {
        if (!isValidNumber(phoneNumber)) return false;
        if (!canSendSms(context)) return false;
        try {
            SmsManager smsManager;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                smsManager = context.getSystemService(SmsManager.class);
            } else {
                smsManager = SmsManager.getDefault();
            }
            if (smsManager == null) return false;
            smsManager.sendTextMessage(phoneNumber.trim(), null, message, null, null);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}
