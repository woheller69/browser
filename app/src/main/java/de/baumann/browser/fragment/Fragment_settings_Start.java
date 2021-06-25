package de.baumann.browser.fragment;

import android.content.Intent;
import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

import de.baumann.browser.activity.Whitelist_Cookie;
import de.baumann.browser.activity.Whitelist_Javascript;
import de.baumann.browser.R;
import de.baumann.browser.activity.Whitelist_Remote;
import de.baumann.browser.browser.AdBlock;

@SuppressWarnings("ConstantConditions")
public class Fragment_settings_Start extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preference_start, rootKey);

        findPreference("sp_ad_block").setSummary(getString(R.string.setting_summary_adblock)+"\n\n"+AdBlock.getHostsDate(getContext()));

        findPreference("start_java").setOnPreferenceClickListener(preference -> {
            Intent intent = new Intent(getActivity(), Whitelist_Javascript.class);
            requireActivity().startActivity(intent);
            return false;
        });
        findPreference("start_cookie").setOnPreferenceClickListener(preference -> {
            Intent intent = new Intent(getActivity(), Whitelist_Cookie.class);
            requireActivity().startActivity(intent);
            return false;
        });
        findPreference("start_remote").setOnPreferenceClickListener(preference -> {
            Intent intent = new Intent(getActivity(), Whitelist_Remote.class);
            requireActivity().startActivity(intent);
            return false;
        });
    }
}
