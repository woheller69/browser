package de.baumann.browser.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceManager;

import de.baumann.browser.activity.Whitelist_Cookie;
import de.baumann.browser.activity.Whitelist_Javascript;
import de.baumann.browser.R;
import de.baumann.browser.activity.Whitelist_Remote;
import de.baumann.browser.browser.AdBlock;

public class Fragment_settings_Start extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener{

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

        setPreferencesFromResource(R.xml.preference_start, rootKey);
        Context context = getContext();
        assert context != null;

        PreferenceManager.setDefaultValues(getContext(), R.xml.preference_start, false);
        initSummary(getPreferenceScreen());

        Preference sp_ad_block = findPreference("sp_ad_block");
        assert sp_ad_block != null;
        sp_ad_block.setSummary(AdBlock.getHostsDate(getContext()));

        Preference start_java = findPreference("start_java");
        assert start_java != null;
        start_java.setOnPreferenceClickListener(preference -> {
            Intent intent = new Intent(getActivity(), Whitelist_Javascript.class);
            requireActivity().startActivity(intent);
            return false;
        });
        Preference start_cookie = findPreference("start_cookie");
        assert start_cookie != null;
        start_cookie.setOnPreferenceClickListener(preference -> {
            Intent intent = new Intent(getActivity(), Whitelist_Cookie.class);
            requireActivity().startActivity(intent);
            return false;
        });
        Preference start_remote = findPreference("start_remote");
        assert start_remote != null;
        start_remote.setOnPreferenceClickListener(preference -> {
            Intent intent = new Intent(getActivity(), Whitelist_Remote.class);
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
            if (p.getTitle().toString().toLowerCase().contains("password")) {
                p.setSummary("******");
            } else {
                if (p.getSummaryProvider()==null)   p.setSummary(editTextPref.getText());
            }
        }
    }

    @Override
    public void onSharedPreferenceChanged(final SharedPreferences sp, String key) {
        if (key.equals("ab_hosts")) {
            AdBlock.downloadHosts(getActivity());
        } else if (key.equals("sp_userAgent") ||
                key.equals("sp_search_engine_custom") ||
                key.equals("searchEngineSwitch") ||
                key.equals("userAgentSwitch") ||
                key.equals("sp_search_engine")) {
            sp.edit().putInt("restart_changed", 1).apply();
            updatePrefSummary(findPreference(key));
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
