package de.baumann.browser.fragment;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Gravity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import java.util.Objects;

import de.baumann.browser.R;

public class Fragment_settings_Delete extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

        setPreferencesFromResource(R.xml.preference_delete, rootKey);
        Activity activity = getActivity();
        assert activity != null;

        Preference sp_deleteDatabase = findPreference("sp_deleteDatabase");
        assert sp_deleteDatabase != null;
        sp_deleteDatabase.setOnPreferenceClickListener(preference -> {
            final SharedPreferences sp = getPreferenceScreen().getSharedPreferences();
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity);
            builder.setMessage(R.string.hint_database);
            builder.setPositiveButton(R.string.app_ok, (dialog, whichButton) -> {
                dialog.cancel();
                activity.deleteDatabase("Ninja4.db");
                activity.deleteDatabase("faviconView.db");
                sp.edit().putInt("restart_changed", 1).apply();
                activity.finish();
            });
            builder.setNegativeButton(R.string.app_cancel, (dialog, whichButton) -> dialog.cancel());
            AlertDialog dialog = builder.create();
            dialog.show();
            Objects.requireNonNull(dialog.getWindow()).setGravity(Gravity.BOTTOM);
            return false;
        });
    }
}