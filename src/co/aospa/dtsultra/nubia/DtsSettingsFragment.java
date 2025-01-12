/*
 * Copyright (C) 2025 Paranoid Android
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.dtsultra.nubia;

import android.os.Bundle;
import android.util.Log;
import android.widget.CompoundButton;

import androidx.annotation.NonNull;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.PreferenceFragment;

import com.android.settingslib.widget.MainSwitchPreference;

import java.util.Arrays;

public class DtsSettingsFragment extends PreferenceFragment implements
        OnPreferenceChangeListener, CompoundButton.OnCheckedChangeListener, Preference.SummaryProvider<ListPreference>, EqualizerPreference.OnEqBandChangeListener {
    private static final String TAG = "DtsSettingsFragment";

    public static final String PREF_ENABLE = "dtsultra_enable";
    public static final String PREF_PROFILE = "dtsultra_profile";
    public static final String PREF_PRESET = "dtsultra_preset";
    public static final String PREF_EQ = "dtsultra_custom_eq";

    private MainSwitchPreference mSwitchBar;
    private ListPreference mProfilePref, mPresetPref;
    private EqualizerPreference mEqPref;
    private DtsUtils mDtsUtils;
    private int mCurrentProfile = -1;
    private int mCurrentPreset = -1;
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.dts_settings);

        mDtsUtils = DtsUtils.getInstance(getActivity());
        final boolean dtsOn = mDtsUtils.getDtsOn();

        mSwitchBar = findPreference(PREF_ENABLE);
        mSwitchBar.addOnSwitchChangeListener(this);
        mSwitchBar.setChecked(dtsOn);

        mProfilePref = findPreference(PREF_PROFILE);
        mProfilePref.setOnPreferenceChangeListener(this);
        mProfilePref.setEnabled(dtsOn);
        mProfilePref.setSummaryProvider(this);
        mCurrentProfile = Integer.parseInt(mProfilePref.getValue());

        mPresetPref = findPreference(PREF_PRESET);
        mPresetPref.setOnPreferenceChangeListener(this);
        mPresetPref.setEnabled(dtsOn);
        mPresetPref.setSummaryProvider(this);
        mCurrentPreset = Integer.parseInt(mPresetPref.getValue());

        mEqPref = findPreference(PREF_EQ);
        mEqPref.setOnPreferenceChangeListener(this);
        mEqPref.setEqBandChangeListener(this);
        mEqPref.setEnabled(dtsOn);

        updateProfileSpecificPrefs();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        switch (preference.getKey()) {
            case PREF_PROFILE:
                mCurrentProfile = Integer.parseInt(newValue.toString());
                if (mCurrentProfile != DtsConstants.PROFILE_MUSIC) {
                    mCurrentPreset = -1;
                } else {
                    mCurrentPreset = Integer.parseInt(mPresetPref.getValue());
                }
                updateProfileSpecificPrefs();
                return true;
            case PREF_PRESET:
                mCurrentPreset = Integer.parseInt(newValue.toString());
                updateProfileSpecificPrefs();
                return true;
            case PREF_EQ:
                updateProfileSpecificPrefs();
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton switchView, boolean isChecked) {
        mDtsUtils.setDtsOn(isChecked);
        mProfilePref.setEnabled(isChecked);
        mPresetPref.setEnabled(isChecked);
        mEqPref.setEnabled(isChecked);

        if (isChecked) {
            DtsUtils.reset();
            mDtsUtils = DtsUtils.getInstance(getContext());
        }
    }

    private void updateProfileSpecificPrefs() {
        int[] eq = null;
        if (mCurrentProfile == DtsConstants.PROFILE_MUSIC && mCurrentPreset == DtsConstants.PRESET_CUSTOM) {
            mEqPref.setVisible(true);
            eq = mEqPref.getEq();
        } else {
            mEqPref.setVisible(false);
        }

        mDtsUtils.setProfile(mCurrentProfile, mCurrentPreset, eq);
        mPresetPref.setVisible(mCurrentProfile == DtsConstants.PROFILE_MUSIC);
    }

    @Override
    public CharSequence provideSummary(@NonNull ListPreference preference) {
        return preference.getEntry();
    }

    @Override
    public void onEqBandChange(int band, int gain) {
        dlog(band + " -> " + gain);
        mDtsUtils.setEqBand(band, gain);
    }

    @Override
    public void onEqChange(int[] gains) {
        dlog("New gains: " + Arrays.toString(gains));
        mDtsUtils.setEq(gains);
    }

    private static void dlog(String msg) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, msg);
        }
    }
}
