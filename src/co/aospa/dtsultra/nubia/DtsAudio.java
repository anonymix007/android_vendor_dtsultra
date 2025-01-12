/*
 * Copyright (C) 2025 Paranoid Android
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.dtsultra.nubia;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;

import android.media.audiofx.AudioEffect;
import android.util.Log;

import co.aospa.dtsultra.nubia.DtsConstants.DtsSystemComm;

class DtsAudio extends AudioEffect {
    private static final String TAG = "DtsAudio";

    private static final UUID EFFECT_TYPE_DTS = UUID.fromString("1d4033c0-8557-11df-9f2d-0002a5d5c51b");
    private static final UUID EFFECT_UUID_DTS = UUID.fromString("146edfc0-7ed2-11e4-80eb-0002a5d5c51b");
    private static final byte[] GET_ALLOCATION_SIZE_MAGIC_BYTES = {10, 11, 11, 10};
    private static final int GET_ALLOCATION_SIZE_MAGIC = ByteBuffer.wrap(GET_ALLOCATION_SIZE_MAGIC_BYTES).order(ByteOrder.nativeOrder()).getInt();

    public DtsAudio(int priority, int audioSession) {
        super(EFFECT_TYPE_DTS, EFFECT_UUID_DTS, priority, audioSession);
    }

    private static byte[] intArrayToByteArray(int[] arr) {
        ByteBuffer bytes = ByteBuffer.allocate(arr.length * 4);
        bytes.order(ByteOrder.nativeOrder());
        for (int v : arr) {
            bytes.putInt(v);
        }
        return bytes.array();
    }

    private int setParameter(int param, int[] value) {
        return setParameter(param, intArrayToByteArray(value));
    }

    private int getParameterIntegerCombined(DtsSystemComm param, int[] data) {
        int[] cmd = new int[data != null ? data.length + 1 : 1];
        cmd[0] = param.getNum();
        if (data != null) {
            System.arraycopy(data, 0, cmd, 1, data.length);
        }
        int[] result = new int[]{0};
        int ret = getParameter(cmd, result);

        dlog("getParameterIntegerCombined: " + param + " -> " + (ret < 0 ? ret : result[0]));

        return ret < 0 ? ret : result[0];
    }

    private int getParameterIntegerAsData(DtsSystemComm param, int[] data) {
        int[] cmd = new int[data != null ? data.length + 1 : 1];
        cmd[0] = param.getNum();
        if (data != null) {
            System.arraycopy(data, 0, cmd, 1, data.length);
        }
        int[] result = new int[1];
        int ret = getParameter(cmd, result);

        dlog("getParameterIntegerAsData: " + param + " -> " + (ret < 0 ? ret : result[0]));

        return ret < 0 ? ret : result[0];
    }

    int getParameterStringAllocationSize(DtsSystemComm param) {
        return switch (param) {
            case GET_ALL_DSEC_TOKENS, GET_VERSIONS, GET_CUSTOMER_CFG -> getParameterIntegerCombined(param, new int[]{GET_ALLOCATION_SIZE_MAGIC});
            default -> -1;
        };
    }

    private String getParameterString(DtsSystemComm param, int[] data, int length) {
        int[] cmd = new int[data != null ? data.length + 1 : 1];
        cmd[0] = param.getNum();
        if (data != null) {
            System.arraycopy(data, 0, cmd, 1, data.length);
        }

        byte[] result = new byte[length];

        int ret = getParameter(cmd, result);

        if (ret < 0) {
            Log.e(TAG, "getParameterString(" + param + ") error: " + ret);
            return null;
        } else if (ret > length) {
            Log.e(TAG, "getParameterString(" + param + ") error: bad length " + ret + " > " + length);
            return null;
        } else if (ret < 8) {
            return "";
        }

        try {
            String stringResult = new String(result, 0, ret, StandardCharsets.UTF_8);
            dlog("getParameterString(" + param + "):" + stringResult);
            return stringResult;
        } catch (Exception e) {
            Log.e(TAG, "getParameterString(" + param + ") exception", e);
            return null;
        }
    }

    public boolean isLicenseValid() {
        return getParameterIntegerAsData(DtsSystemComm.GET_DTS_LICENSE_IS_VALID, null) == 0;
    }
    
    private String getParameterString(DtsSystemComm param) {
        int length = getParameterStringAllocationSize(param);
        if (length <= 0) {
            return "";
        } else {
            return getParameterString(param, new int[]{0}, length);
        }
    }

    public String getDSECTokens() {
        return getParameterString(DtsSystemComm.GET_ALL_DSEC_TOKENS);
    }

    public String getCustomerConfigs() {
        return getParameterString(DtsSystemComm.GET_CUSTOMER_CFG);
    }

    public String getVersions() {
        return getParameterString(DtsSystemComm.GET_VERSIONS);
    }

    public boolean getDtsOn() {
        return getParameterIntegerCombined(DtsSystemComm.GET_ENABLED, null) > 0;
    }

    public void setDtsOn(boolean enable) {
        dlog("setDtsOn: " + enable);
        setParameter(DtsSystemComm.SET_ENABLED.getNum(), enable ? 1 : 0);
    }

    public void setProfile(int profile) {
        dlog("setProfile: " + profile);
        setParameter(DtsSystemComm.SET_CONTENT_MODE_V2.getNum(), profile);
    }

    public int getProfile() {
        return getParameterIntegerAsData(DtsSystemComm.GET_CONTENT_MODE_V2, null);
    }

    public void setEqOnV3(int route, int profile, boolean enabled) {
        dlog("setEqOnV3: " + enabled);
        int[] data = {route, profile, enabled ? 1 : 0};
        int ret = setParameter(DtsSystemComm.SET_GEQ_ENABLED_V3.getNum(), data);
        dlog("setEqOnV3 -> " + ret);
    }

    public void setAllEqGainsV3(int route, int profile, int gains[]) {
        if (gains.length != 10) {
            Log.e(TAG, "setAllGEQGainsV3 expected 10 bands, but got " + gains.length);
            return;
        }
        int[] data = new int[12];
        data[0] = route;
        data[1] = profile;
        System.arraycopy(gains, 0, data, 2, 10);
        dlog("setAllEqGainsV3: " + Arrays.toString(gains));
        int ret = setParameter(DtsSystemComm.SET_GEQ_GAIN_ALL_V3.getNum(), data);
        dlog("setAllEqGainsV3 -> " + ret);
    }

    public void setEqGainV3(int route, int profile, int band, int gain) {
        int[] data = new int[]{route, profile, band, gain};
        dlog("setEqGainV3: " + band + " -> " + gain);
        int ret = setParameter(DtsSystemComm.SET_GEQ_GAIN_V3.getNum(), data);
        dlog("setEqGainV3 -> " + ret);
    }

    private static void dlog(String msg) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, msg);
        }
    }
}
