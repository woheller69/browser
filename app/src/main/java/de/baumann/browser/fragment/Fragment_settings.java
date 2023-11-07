package de.baumann.browser.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceManager;

import de.baumann.browser.activity.Manage_UserScripts;
import de.baumann.browser.activity.Settings_Delete;
import de.baumann.browser.activity.Settings_Backup;
import de.baumann.browser.activity.Settings_Filter;
import de.baumann.browser.activity.Settings_StartActivity;
import de.baumann.browser.activity.Settings_UI;
import de.baumann.browser.R;

public class Fragment_settings extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

        setPreferencesFromResource(R.xml.preference_setting, rootKey);
        Context context = getContext();
        assert context != null;
        PreferenceManager.setDefaultValues(context, R.xml.preference_setting, false);
        initSummary(getPreferenceScreen());

        Preference settings_filter = findPreference("settings_filter");
        assert settings_filter != null;
        settings_filter.setOnPreferenceClickListener(preference -> {
           Intent intent = new Intent(getActivity(), Settings_Filter.class);
           requireActivity().startActivity(intent);
           return false;
        });

        Preference settings_data = findPreference("settings_data");
        assert settings_data != null;
        settings_data.setOnPreferenceClickListener(preference -> {
            Intent intent = new Intent(getActivity(), Settings_Backup.class);
            requireActivity().startActivity(intent);
            return false;
        });

        Preference settings_ui = findPreference("settings_ui");
        assert settings_ui != null;
        settings_ui.setOnPreferenceClickListener(preference -> {
            Intent intent = new Intent(getActivity(), Settings_UI.class);
            requireActivity().startActivity(intent);
            return false;
        });

        Preference settings_start = findPreference("settings_start");
        assert settings_start != null;
        settings_start.setOnPreferenceClickListener(preference -> {
            Intent intent = new Intent(getActivity(), Settings_StartActivity.class);
            requireActivity().startActivity(intent);
            return false;
        });

        Preference settings_clear = findPreference("settings_clear");
        assert settings_clear != null;
        settings_clear.setOnPreferenceClickListener(preference -> {
            Intent intent = new Intent(getActivity(), Settings_Delete.class);
            requireActivity().startActivity(intent);
            return false;
        });

        Preference scripts = findPreference("scripts");
        assert scripts != null;
        scripts.setOnPreferenceClickListener(preference -> {
            Intent intent = new Intent(getActivity(), Manage_UserScripts.class);
            requireActivity().startActivity(intent);
            return false;
        });
    }

    private void initSummary(Preference p) {
        if (p instanceof PreferenceGroup) {
            PreferenceGroup pGrp = (PreferenceGroup) p;
            for (int i = 0; i < pGrp.getPreferenceCount(); i++) {
                initSummary(pGrp.getPreference(i));
            }
        } else {
            updatePrefSummary(p);
        }
    }

    private void updatePrefSummary(Preference p) {
        if (p instanceof ListPreference) {
            ListPreference listPref = (ListPreference) p;
            p.setSummary(listPref.getEntry());
        }
        if (p instanceof EditTextPreference) {
            EditTextPreference editTextPref = (EditTextPreference) p;
                if (p.getSummaryProvider()==null)   p.setSummary(editTextPref.getText());
        }
    }

}