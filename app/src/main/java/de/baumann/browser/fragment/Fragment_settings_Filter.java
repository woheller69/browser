package de.baumann.browser.fragment;
import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

import de.baumann.browser.R;

public class Fragment_settings_Filter extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preference_filter, rootKey);
    }
}