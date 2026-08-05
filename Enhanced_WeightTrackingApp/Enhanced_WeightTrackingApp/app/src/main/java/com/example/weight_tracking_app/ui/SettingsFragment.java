package com.example.weight_tracking_app.ui;
import android.os.Bundle;
import androidx.preference.PreferenceFragmentCompat;
import com.example.weight_tracking_app.R;
public class SettingsFragment extends PreferenceFragmentCompat {
    @Override public void onCreatePreferences(Bundle savedInstanceState, String rootKey) { setPreferencesFromResource(R.xml.preferences, rootKey); }
}
