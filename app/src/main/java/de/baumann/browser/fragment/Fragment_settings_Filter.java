package de.baumann.browser.fragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Objects;

import de.baumann.browser.R;

public class Fragment_settings_Filter extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preference_filter, rootKey);
        final SharedPreferences sp = getPreferenceScreen().getSharedPreferences();

        final Preference filter_01 = findPreference("filter_01");
        Objects.requireNonNull(filter_01).setTitle(sp.getString("icon_01", requireActivity().getResources().getString(R.string.color_red)));
        final Preference filter_02 = findPreference("filter_02");
        Objects.requireNonNull(filter_02).setTitle(sp.getString("icon_02", requireActivity().getResources().getString(R.string.color_pink)));
        final Preference filter_03 = findPreference("filter_03");
        Objects.requireNonNull(filter_03).setTitle(sp.getString("icon_03", requireActivity().getResources().getString(R.string.color_purple)));
        final Preference filter_04 = findPreference("filter_04");
        Objects.requireNonNull(filter_04).setTitle(sp.getString("icon_04", requireActivity().getResources().getString(R.string.color_blue)));
        final Preference filter_05 = findPreference("filter_05");
        Objects.requireNonNull(filter_05).setTitle(sp.getString("icon_05", requireActivity().getResources().getString(R.string.color_teal)));
        final Preference filter_06 = findPreference("filter_06");
        Objects.requireNonNull(filter_06).setTitle(sp.getString("icon_06", requireActivity().getResources().getString(R.string.color_green)));
        final Preference filter_07 = findPreference("filter_07");
        Objects.requireNonNull(filter_07).setTitle(sp.getString("icon_07", requireActivity().getResources().getString(R.string.color_lime)));
        final Preference filter_08 = findPreference("filter_08");
        Objects.requireNonNull(filter_08).setTitle(sp.getString("icon_08", requireActivity().getResources().getString(R.string.color_yellow)));
        final Preference filter_09 = findPreference("filter_09");
        Objects.requireNonNull(filter_09).setTitle(sp.getString("icon_09", requireActivity().getResources().getString(R.string.color_orange)));
        final Preference filter_10 = findPreference("filter_10");
        Objects.requireNonNull(filter_10).setTitle(sp.getString("icon_10", requireActivity().getResources().getString(R.string.color_brown)));
        final Preference filter_11 = findPreference("filter_11");
        Objects.requireNonNull(filter_11).setTitle(sp.getString("icon_11", requireActivity().getResources().getString(R.string.color_grey)));

        filter_01.setOnPreferenceChangeListener((preference, newValue) -> {
            if (!sp.getBoolean("filter_01", true)) {
                editFilterNames("icon_01", getString(R.string.color_red), filter_01);
                return true;
            }
            return true;
        });
        filter_02.setOnPreferenceChangeListener((preference, newValue) -> {
            if (!sp.getBoolean("filter_02", true)) {
                editFilterNames("icon_02", getString(R.string.color_pink), filter_02);
                return true;
            }
            return true;
        });
        filter_03.setOnPreferenceChangeListener((preference, newValue) -> {
            if (!sp.getBoolean("filter_03", true)) {
                editFilterNames("icon_03", getString(R.string.color_purple), filter_03);
                return true;
            }
            return true;
        });
        filter_04.setOnPreferenceChangeListener((preference, newValue) -> {
            if (!sp.getBoolean("filter_04", true)) {
                editFilterNames("icon_04", getString(R.string.color_blue), filter_04);
                return true;
            }
            return true;
        });
        filter_05.setOnPreferenceChangeListener((preference, newValue) -> {
            if (!sp.getBoolean("filter_05", true)) {
                editFilterNames("icon_05", getString(R.string.color_teal), filter_05);
                return true;
            }
            return true;
        });
        filter_06.setOnPreferenceChangeListener((preference, newValue) -> {
            if (!sp.getBoolean("filter_06", true)) {
                editFilterNames("icon_06", getString(R.string.color_green), filter_06);
                return true;
            }
            return true;
        });
        filter_07.setOnPreferenceChangeListener((preference, newValue) -> {
            if (!sp.getBoolean("filter_07", true)) {
                editFilterNames("icon_07", getString(R.string.color_lime), filter_07);
                return true;
            }
            return true;
        });
        filter_08.setOnPreferenceChangeListener((preference, newValue) -> {
            if (!sp.getBoolean("filter_08", true)) {
                editFilterNames("icon_08", getString(R.string.color_yellow), filter_08);
                return true;
            }
            return true;
        });
        filter_09.setOnPreferenceChangeListener((preference, newValue) -> {
            if (!sp.getBoolean("filter_09", true)) {
                editFilterNames("icon_09", getString(R.string.color_orange), filter_09);
                return true;
            }
            return true;
        });
        filter_10.setOnPreferenceChangeListener((preference, newValue) -> {
            if (!sp.getBoolean("filter_10", true)) {
                editFilterNames("icon_10", getString(R.string.color_brown), filter_10);
                return true;
            }
            return true;
        });
        filter_11.setOnPreferenceChangeListener((preference, newValue) -> {
            if (!sp.getBoolean("filter_11", true)) {
                editFilterNames("icon_11", getString(R.string.color_grey), filter_11);
                return true;
            }
            return true;
        });
    }

    @SuppressWarnings("ConstantConditions")
    private void editFilterNames (final String filter, final String filterDefault, final Preference preference) {
        SharedPreferences sp = getPreferenceScreen().getSharedPreferences();
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder (getActivity());
        View dialogView = View.inflate(getActivity(), R.layout.dialog_edit_title, null);

        TextInputLayout edit_title_layout = dialogView.findViewById(R.id.edit_title_layout);
        TextInputLayout edit_userName_layout = dialogView.findViewById(R.id.edit_userName_layout);
        TextInputLayout edit_PW_layout = dialogView.findViewById(R.id.edit_PW_layout);
        ImageView ib_icon = dialogView.findViewById(R.id.edit_icon);
        ib_icon.setVisibility(View.GONE);
        edit_title_layout.setVisibility(View.VISIBLE);
        edit_userName_layout.setVisibility(View.GONE);
        edit_PW_layout.setVisibility(View.GONE);

        EditText edit_title = dialogView.findViewById(R.id.edit_title);
        edit_title.setText(sp.getString(filter, filterDefault));

        builder.setView(dialogView);
        builder.setTitle(getString(R.string.menu_edit));
        builder.setPositiveButton(R.string.app_ok, (dialog, whichButton) -> {
            String text = edit_title.getText().toString().trim();
            sp.edit().putString(filter, text).apply();
            Objects.requireNonNull(preference).setTitle(sp.getString(filter, filterDefault));
            dialog.cancel();
        });
        builder.setNegativeButton(R.string.app_cancel, (dialog, whichButton) -> dialog.cancel());

        AlertDialog dialog = builder.create();
        dialog.show();
    }
}