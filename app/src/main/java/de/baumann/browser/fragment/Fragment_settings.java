package de.baumann.browser.fragment;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceFragmentCompat;

import de.baumann.browser.activity.Settings_ClearActivity;
import de.baumann.browser.activity.Settings_DataActivity;
import de.baumann.browser.activity.Settings_FilterActivity;
import de.baumann.browser.activity.Settings_GestureActivity;
import de.baumann.browser.activity.Settings_StartActivity;
import de.baumann.browser.activity.Settings_UIActivity;
import de.baumann.browser.unit.HelperUnit;
import de.baumann.browser.R;

public class Fragment_settings extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preference_setting, rootKey);

       findPreference("settings_filter").setOnPreferenceClickListener(preference -> {
           Intent intent = new Intent(getActivity(), Settings_FilterActivity.class);
           requireActivity().startActivity(intent);
           return false;
       });
        findPreference("settings_data").setOnPreferenceClickListener(preference -> {
            Intent intent = new Intent(getActivity(), Settings_DataActivity.class);
            requireActivity().startActivity(intent);
            return false;
        });
        findPreference("settings_ui").setOnPreferenceClickListener(preference -> {
            Intent intent = new Intent(getActivity(), Settings_UIActivity.class);
            requireActivity().startActivity(intent);
            return false;
        });
        findPreference("settings_gesture").setOnPreferenceClickListener(preference -> {
            Intent intent = new Intent(getActivity(), Settings_GestureActivity.class);
            requireActivity().startActivity(intent);
            return false;
        });
        findPreference("settings_start").setOnPreferenceClickListener(preference -> {
            Intent intent = new Intent(getActivity(), Settings_StartActivity.class);
            requireActivity().startActivity(intent);
            return false;
        });
        findPreference("settings_clear").setOnPreferenceClickListener(preference -> {
            Intent intent = new Intent(getActivity(), Settings_ClearActivity.class);
            requireActivity().startActivity(intent);
            return false;
        });
        findPreference("settings_info").setOnPreferenceClickListener(preference -> {
            showLicenseDialog(getString(R.string.menu_other_info), getString(R.string.changelog_dialog));
            return false;
        });
        findPreference("settings_help").setOnPreferenceClickListener(preference -> {
            showLicenseDialog(getString(R.string.dialogHelp_tipTitle), getString(R.string.dialogHelp_tipText));
            return false;
        });
        findPreference("settings_appSettings").setOnPreferenceClickListener(preference -> {
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", requireActivity().getPackageName(), null);
            intent.setData(uri);
            getActivity().startActivity(intent);
            return false;
        });
    }

    @Override
    public void onSharedPreferenceChanged(final SharedPreferences sp, String key) {
        if (key.equals("userAgent") || key.equals("sp_search_engine_custom") || key.equals("@string/sp_search_engine")) {
            sp.edit().putInt("restart_changed", 1).apply();
        }
    }

    @SuppressWarnings("ConstantConditions")
    private void showLicenseDialog(String title, String text) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getContext());
        builder.setTitle(title);
        builder.setMessage(HelperUnit.textSpannable(text));
        AlertDialog dialog = builder.create();
        dialog.show();
    }
}