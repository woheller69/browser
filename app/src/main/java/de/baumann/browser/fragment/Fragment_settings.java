package de.baumann.browser.fragment;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.Gravity;
import android.widget.TextView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceManager;

import java.util.Objects;

import de.baumann.browser.activity.Settings_Delete;
import de.baumann.browser.activity.Settings_Backup;
import de.baumann.browser.activity.Settings_Filter;
import de.baumann.browser.activity.Settings_Gesture;
import de.baumann.browser.activity.Settings_StartActivity;
import de.baumann.browser.activity.Settings_UI;
import de.baumann.browser.unit.HelperUnit;
import de.baumann.browser.R;

@SuppressWarnings("ConstantConditions")
public class Fragment_settings extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preference_setting, rootKey);
        PreferenceManager.setDefaultValues(getContext(), R.xml.preference_setting, false);
        initSummary(getPreferenceScreen());

       findPreference("settings_filter").setOnPreferenceClickListener(preference -> {
           Intent intent = new Intent(getActivity(), Settings_Filter.class);
           requireActivity().startActivity(intent);
           return false;
       });
        findPreference("settings_data").setOnPreferenceClickListener(preference -> {
            Intent intent = new Intent(getActivity(), Settings_Backup.class);
            requireActivity().startActivity(intent);
            return false;
        });
        findPreference("settings_ui").setOnPreferenceClickListener(preference -> {
            Intent intent = new Intent(getActivity(), Settings_UI.class);
            requireActivity().startActivity(intent);
            return false;
        });
        findPreference("settings_gesture").setOnPreferenceClickListener(preference -> {
            Intent intent = new Intent(getActivity(), Settings_Gesture.class);
            requireActivity().startActivity(intent);
            return false;
        });
        findPreference("settings_start").setOnPreferenceClickListener(preference -> {
            Intent intent = new Intent(getActivity(), Settings_StartActivity.class);
            requireActivity().startActivity(intent);
            return false;
        });
        findPreference("settings_clear").setOnPreferenceClickListener(preference -> {
            Intent intent = new Intent(getActivity(), Settings_Delete.class);
            requireActivity().startActivity(intent);
            return false;
        });
        findPreference("settings_info").setOnPreferenceClickListener(preference -> {MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getContext());
            builder.setTitle(getString(R.string.menu_other_info));
            builder.setMessage(HelperUnit.textSpannable(getString(R.string.changelog_dialog)));
            AlertDialog dialog = builder.create();
            dialog.show();
            ((TextView)dialog.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
            Objects.requireNonNull(dialog.getWindow()).setGravity(Gravity.BOTTOM);
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
            if (p.getTitle().toString().toLowerCase().contains("password"))
            {
                p.setSummary("******");
            } else {
                p.setSummary(editTextPref.getText());
            }
        }
        if (p instanceof MultiSelectListPreference) {
            EditTextPreference editTextPref = (EditTextPreference) p;
            p.setSummary(editTextPref.getText());
        }
    }

    @Override
    public void onSharedPreferenceChanged(final SharedPreferences sp, String key) {
        if (key.equals("userAgent") || key.equals("sp_search_engine_custom") || key.equals("@string/sp_search_engine")) {
            sp.edit().putInt("restart_changed", 1).apply();
        }
        updatePrefSummary(findPreference(key));
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }
}