/*
 * Copyright (C) 2023-25 Paranoid Android
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.dtsultra.nubia;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioDeviceAttributes;
import android.media.AudioDeviceCallback;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.AudioManager.AudioPlaybackCallback;
import android.media.AudioPlaybackConfiguration;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.preference.PreferenceManager;

import static android.media.AudioDeviceInfo.TYPE_BLE_HEADSET;
import static android.media.AudioDeviceInfo.TYPE_BLE_SPEAKER;
import static android.media.AudioDeviceInfo.TYPE_BLUETOOTH_A2DP;
import static android.media.AudioDeviceInfo.TYPE_BUILTIN_EARPIECE;
import static android.media.AudioDeviceInfo.TYPE_BUILTIN_SPEAKER;
import static android.media.AudioDeviceInfo.TYPE_LINE_ANALOG;
import static android.media.AudioDeviceInfo.TYPE_LINE_DIGITAL;
import static android.media.AudioDeviceInfo.TYPE_USB_HEADSET;
import static android.media.AudioDeviceInfo.TYPE_WIRED_HEADPHONES;
import static android.media.AudioDeviceInfo.TYPE_WIRED_HEADSET;

import static co.aospa.dtsultra.nubia.DtsConstants.AudioRoute.BLUETOOTH;
import static co.aospa.dtsultra.nubia.DtsConstants.AudioRoute.INTERNAL_SPEAKERS;
import static co.aospa.dtsultra.nubia.DtsConstants.AudioRoute.LINE_OUT;
import static co.aospa.dtsultra.nubia.DtsConstants.AudioRoute.UNKNOWN;
import static co.aospa.dtsultra.nubia.DtsConstants.AudioRoute.USB;

public final class DtsUtils {
    private static final String TAG = "DtsUtils";
    private static final int EFFECT_PRIORITY = 100;
    private static DtsUtils mInstance;
    private DtsAudio mDtsAudio;
    private final Context mContext;
    private final AudioManager mAudioManager;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private boolean mCallbacksRegistered = false;
    private static final AudioAttributes ATTRIBUTES_MEDIA = new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .build();
    private int mCurrentDeviceType = -1;

    /* Restore current profile on every media session */
    private final AudioPlaybackCallback mPlaybackCallback = new AudioPlaybackCallback() {
        @Override
        public void onPlaybackConfigChanged(List<AudioPlaybackConfiguration> configs) {
            boolean isPlaying = configs.stream().anyMatch(c -> c.getPlayerState() == AudioPlaybackConfiguration.PLAYER_STATE_STARTED);
            dlog("onPlaybackConfigChanged isPlaying=" + isPlaying);
            if (mDtsAudio != null && isPlaying) {
                setCurrentProfile();
            }
        }
    };

    /* Restore current profile on audio device change */
    private final AudioDeviceCallback mAudioDeviceCallback = new AudioDeviceCallback() {
        public void onAudioDevicesAdded(AudioDeviceInfo[] addedDevices) {
            dlog("onAudioDevicesAdded");
            if (mDtsAudio != null) {
                setCurrentProfile();
            }
        }

        public void onAudioDevicesRemoved(AudioDeviceInfo[] removedDevices) {
            dlog("onAudioDevicesRemoved");
            if (mDtsAudio != null) {
                setCurrentProfile();
            }
        }
    };

    private DtsUtils(Context context) {
        mContext = context;
        mDtsAudio = new DtsAudio(EFFECT_PRIORITY, 0);
        mAudioManager = context.getSystemService(AudioManager.class);
        dlog("initialized");

        boolean licensed = mDtsAudio.isLicenseValid();

        Log.i(TAG, "DtsAudio license valid: " + licensed);
        if (!licensed) {
            Toast.makeText(mContext, R.string.dtsultra_license_invalid, Toast.LENGTH_LONG).show();
        }
        Log.i(TAG, "DtsAudio tokens: " + mDtsAudio.getDSECTokens());
        Log.i(TAG, "DtsAudio versions: " + mDtsAudio.getVersions());
        Log.i(TAG, "DtsAudio configs: " + mDtsAudio.getCustomerConfigs());
    }

    public static synchronized DtsUtils getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new DtsUtils(context);
        }
        return mInstance;
    }

    public static synchronized void reset() {
        if (mInstance != null) {
            mInstance.registerCallbacks(false);
            mInstance.mDtsAudio.release();
            mInstance.mDtsAudio = null;
        }
        mInstance = null;
    }

    public void onBootCompleted() {
        dlog("onBootCompleted");

        /* Restore current profile now and on certain audio changes. */
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        setDtsOn(prefs.getBoolean(DtsSettingsFragment.PREF_ENABLE, getDtsOn()));
        setCurrentProfile();
    }

    private int[] getEqualizerPresetGains(int preset) {
        int id = switch (preset) {
            case DtsConstants.PRESET_ROCK -> R.array.dtsultra_preset_rock;
            case DtsConstants.PRESET_CLASSICAL -> R.array.dtsultra_preset_classical;
            case DtsConstants.PRESET_POP -> R.array.dtsultra_preset_pop;
            case DtsConstants.PRESET_OFF -> R.array.dtsultra_preset_off;
            default -> -1;
        };
        if (id == -1) {
            return null;
        }
        return mContext.getResources().getIntArray(id);
    }

    private void checkEffect() {
        if (!mDtsAudio.hasControl()) {
            Log.w(TAG, "Lost control, recreating effect");
            mDtsAudio.release();
            mDtsAudio = new DtsAudio(EFFECT_PRIORITY, 0);
        }
    }

    public static String eqToString(int[] eq) {
        return Arrays.stream(eq).mapToObj(String::valueOf).collect(Collectors.joining(";"));
    }

    public static int[] eqFromString(String values) {
        return values.isEmpty() ? null : Arrays.stream(values.split(";")).mapToInt(Integer::parseInt).toArray();
    }

    private void setCurrentProfile() {
        @SuppressLint("MissingPermission") final AudioDeviceAttributes device = mAudioManager.getDevicesForAttributes(ATTRIBUTES_MEDIA).getFirst();
        mCurrentDeviceType = device.getType();

        if (!getDtsOn()) {
            dlog("setCurrentProfile: skip, DTS is off");
            return;
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);

        int profile = Integer.parseInt(Objects.requireNonNull(prefs.getString(DtsSettingsFragment.PREF_PROFILE, "3" /* music */)));
        int preset = Integer.parseInt((Objects.requireNonNull(prefs.getString(DtsSettingsFragment.PREF_PRESET, "3" /* off */))));

        dlog("setCurrentProfile: " + profile + "," + preset);

        int[] eq = null;
        if (preset == DtsConstants.PRESET_CUSTOM) {
            String values = prefs.getString(DtsSettingsFragment.PREF_EQ, "");
            if (values != null && !values.isEmpty()) {
                eq = eqFromString(values);
            }
        }

        setProfile(profile, preset, eq);
    }

    private void registerCallbacks(boolean register) {
        dlog("registerCallbacks(" + register + ") mCallbacksRegistered=" + mCallbacksRegistered);
        if (register && !mCallbacksRegistered) {
            mAudioManager.registerAudioPlaybackCallback(mPlaybackCallback, mHandler);
            mAudioManager.registerAudioDeviceCallback(mAudioDeviceCallback, mHandler);
            mCallbacksRegistered = true;
        } else if (!register && mCallbacksRegistered) {
            mAudioManager.unregisterAudioPlaybackCallback(mPlaybackCallback);
            mAudioManager.unregisterAudioDeviceCallback(mAudioDeviceCallback);
            mCallbacksRegistered = false;
        }
    }

    public void setDtsOn(boolean on) {
        checkEffect();
        dlog("setDtsOn: " + on);
        mDtsAudio.setDtsOn(on);
        registerCallbacks(on);
    }

    public boolean getDtsOn() {
        checkEffect();
        boolean on = mDtsAudio.getDtsOn();
        dlog("getDtsOn: " + on);
        return on;
    }

    public void setProfile(int index, int preset, int[] eq) {
        checkEffect();
        dlog("setProfile: " + index + ", " + preset);
        if (getProfile() != index) {
            mDtsAudio.setProfile(index);
        }
        if (index == DtsConstants.PROFILE_MUSIC && preset != -1) {
            if (preset != DtsConstants.PRESET_CUSTOM) {
                eq = getEqualizerPresetGains(preset);
            }

            if (eq == null) {
                if (preset == DtsConstants.PRESET_CUSTOM) {
                    Log.e(TAG, "No eq gains are provided for custom preset");
                } else {
                    Log.e(TAG, "Unknown preset: " + preset);
                }
                return;
            }

            //setEqOn(mCurrentDeviceType, false);
            //setEqOn(mCurrentDeviceType, true);
            setEq(mCurrentDeviceType, eq);
        }

    }

    public int getProfile() {
        int profile = mDtsAudio.getProfile();
        dlog("getProfile: " + profile);
        return profile;
    }

    public String getProfileName() {
        String profile = Integer.toString(mDtsAudio.getProfile());
        List<String> profiles = Arrays.asList(mContext.getResources().getStringArray(R.array.dtsultra_profile_values));
        int profileIndex = profiles.indexOf(profile);
        dlog("getProfileName: profile=" + profile + " index=" + profileIndex);
        return profileIndex == -1 ? null : mContext.getResources().getStringArray(R.array.dtsultra_profile_entries)[profileIndex];
    }

    public String getPresetName() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        int preset = Integer.parseInt((Objects.requireNonNull(prefs.getString(DtsSettingsFragment.PREF_PRESET, "3" /* off */))));
        List<String> presets = Arrays.asList(mContext.getResources().getStringArray(R.array.dtsultra_preset_entries));
        int presetIndex = presets.indexOf(String.valueOf(preset));
        dlog("getPresetName: preset=" + preset + " index=" + presetIndex);
        return presetIndex == -1 ? null : mContext.getResources().getStringArray(R.array.dtsultra_preset_values)[presetIndex];
    }

    public static int toDb(int gain) {
        return (int) Math.round(Math.log10(gain / 4096.) * 20.);
    }

    public static int toEagleGain(int db) {
        return (int) (Math.pow(10., db / 20.) * 4096.);
    }

    private void setEq(int deviceType, int[] gains) {
        if (gains.length != 10) {
            throw new IllegalArgumentException("Expected 10 bands");
        }

        int profile = mDtsAudio.getProfile();
        if (profile < 0) {
            Log.e(TAG, "Failed to get profile: " + profile);
            return;
        }

        int audioRoute = getRouteFromDeviceType(deviceType).ordinal();
        int[] eagleGains = Arrays.stream(gains).map(DtsUtils::toEagleGain).toArray();
        mDtsAudio.setAllEqGainsV3(audioRoute, profile, eagleGains);
    }

    public void setEq(int[] gains) {
        checkEffect();
        setEq(mCurrentDeviceType, gains);
    }

    private void setEqOn(int deviceType, boolean enabled) {
        checkEffect();
        int profile = getProfile();
        if (profile < 0) {
            Log.e(TAG, "Failed to get profile: " + profile);
            return;
        }

        int audioRoute = getRouteFromDeviceType(deviceType).ordinal();
        mDtsAudio.setEqOnV3(audioRoute, profile, enabled);
    }

    public void setEqBand(int band, int gain) {
        checkEffect();
        int profile = mDtsAudio.getProfile();
        if (profile < 0) {
            Log.e(TAG, "Failed to get profile: " + profile);
            return;
        }

        int audioRoute = getRouteFromDeviceType(mCurrentDeviceType).ordinal();
        int eagleGain = toEagleGain(gain);
        mDtsAudio.setEqGainV3(audioRoute, profile, band, eagleGain);
    }

    private static DtsConstants.AudioRoute getRouteFromDeviceType(int type) {
        return switch(type) {
            case TYPE_BUILTIN_EARPIECE, TYPE_BUILTIN_SPEAKER -> INTERNAL_SPEAKERS;
            case TYPE_WIRED_HEADSET, TYPE_WIRED_HEADPHONES, TYPE_LINE_ANALOG, TYPE_LINE_DIGITAL -> LINE_OUT;
            case TYPE_BLUETOOTH_A2DP, TYPE_BLE_HEADSET, TYPE_BLE_SPEAKER -> BLUETOOTH;
            case TYPE_USB_HEADSET -> USB;
            default -> UNKNOWN;
        };
    }

    private static void dlog(String msg) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, msg);
        }
    }
}
