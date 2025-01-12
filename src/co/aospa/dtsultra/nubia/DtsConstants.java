/*
 * Copyright (C) 2025 Paranoid Android
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.dtsultra.nubia;

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

import java.util.EnumMap;
import java.util.Map;

public class DtsConstants {
    /* Effect error codes */
    public static final int SERVICE_CODE_SUCCESS = 0;
    public static final int SERVICE_CODE_INVALID_ARGUMENT = -1;
    public static final int SERVICE_CODE_FILE_NOT_PRESENT = -2;
    public static final int SERVICE_CODE_TRANSACTION_ERROR = -3;
    public static final int SERVICE_CODE_WRONG_FILE_SIZE = -4;
    public static final int SERVICE_CODE_SERVICE_NOT_PRESENT = -5;
    public static final int SERVICE_CODE_OUT_OF_MEMORY = -6;
    public static final int SERVICE_CODE_INSUFFICIENT_BUFFER_SIZE = -7;
    public static final int SERVICE_CODE_NOT_SUPPORTED = -8;
    public static final int SERVICE_CODE_WRONG_MODE = -9;
    public static final int SERVICE_CODE_INVALID_DTSCS = -1000;
    public static final int SERVICE_CODE_INVALID_EAGLE_PIPELINE = -1001;
    public static final int SERVICE_CODE_ROUTE_NOT_FOUND = -1002;
    public static final int SERVICE_CODE_ROOM_NOT_FOUND = -1003;
    public static final int SERVICE_CODE_DTSCS_TO_EAGLE_CONV_FAILED = -1004;
    public static final int SERVICE_CODE_UUID_NOT_FOUND = -1005;

    /* Profiles */
    public static final int PROFILE_MUSIC = 3;
    public static final int PROFILE_MOVIE = 4;
    public static final int PROFILE_GAME = 6;

    public static final int PRESET_ROCK = 0;
    public static final int PRESET_CLASSICAL = 1;
    public static final int PRESET_POP = 2;
    public static final int PRESET_OFF = 3;
    public static final int PRESET_CUSTOM = 4;

    enum AudioRoute {
        UNKNOWN,
        INTERNAL_SPEAKERS,
        LINE_OUT,
        BLUETOOTH,
        USB,
    }

    enum DtsSystemComm {
        GET_ENABLED,
        SET_ENABLED,
        GET_GEQ_GAIN,
        SET_GEQ_GAIN,
        GET_VERSIONS,
        SET_GEQ_ENABLED,
        GET_GEQ_ENABLED,
        GET_GEQ_GAIN_ALL,
        SET_GEQ_GAIN_ALL,
        SET_LISTENING_TEST,
        GET_LISTENING_TEST,
        RESET_LISTENING_TEST,
        GET_DTS_LICENSE_IS_VALID,
        GET_M6M8_LICENSE_IS_VALID,
        GET_ALL_DSEC_TOKENS,
        GET_CUSTOMER_CFG,
        SET_TUNING_ACCESSORY,
        GET_TUNING_ACCESSORY,
        GET_STEREO_MODE_V1(100),
        SET_STEREO_MODE_V1,
        SET_TUNING_SPEAKER_ON_V1,
        GET_TUNING_SPEAKER_ON_V1,
        SET_MULTI_CHANNEL_ROOM_V1,
        GET_MULTI_CHANNEL_ROOM_V1,
        SET_BASSBOOST_LEVEL_V1,
        GET_BASSBOOST_LEVEL_V1,
        SET_TREBLEBOOST_LEVEL_V1,
        GET_TREBLEBOOST_LEVEL_V1,
        GET_STEREO_MODE_V2(200),
        SET_STEREO_MODE_V2,
        SET_BASSBOOST_LEVEL_V2,
        GET_BASSBOOST_LEVEL_V2,
        SET_TREBLEBOOST_LEVEL_V2,
        GET_TREBLEBOOST_LEVEL_V2,
        SET_GEQ_5BAND_GAIN_V2,
        GET_GEQ_5BAND_GAIN_V2,
        SET_LOUDNESS_CONTROL_ENABLED_V2,
        GET_LOUDNESS_CONTROL_ENABLED_V2,
        SET_BASSBOOST_ENABLE_V2,
        GET_BASSBOOST_ENABLE_V2,
        SET_TREBLEBOOST_ENABLE_V2,
        GET_TREBLEBOOST_ENABLE_V2,
        SET_DIALOGBOOST_ENABLE_V2,
        GET_DIALOGBOOST_ENABLE_V2,
        SET_DIALOGBOOST_LEVEL_V2,
        GET_DIALOGBOOST_LEVEL_V2,
        SET_GEQ_5BAND_GAIN_ALL_V2,
        GET_GEQ_5BAND_GAIN_ALL_V2,
        RESET_AUDIO_LEVEL_SETTINGS_V2,
        SET_CONTENT_MODE_V2,
        GET_CONTENT_MODE_V2,
        SET_GEQ_ENABLED_V3,
        GET_GEQ_ENABLED_V3,
        SET_GEQ_GAIN_V3,
        GET_GEQ_GAIN_V3,
        SET_GEQ_GAIN_ALL_V3,
        GET_GEQ_GAIN_ALL_V3,
        SET_GEQ_5BAND_GAIN_V3,
        GET_GEQ_5BAND_GAIN_V3,
        SET_GEQ_5BAND_GAIN_ALL_V3,
        GET_GEQ_5BAND_GAIN_ALL_V3,
        TUNE_SET_DATA(5000);

        private final int mNum;
        private static final Map<DtsSystemComm, Integer> NUMS = new EnumMap(DtsSystemComm.class);
        private static final int NUM_BASE = 0;

        static {
            int num = NUM_BASE;
            for (var value : values()) {
                int currNum = value.mNum;
                if (currNum > 0) {
                    if (currNum <= num) {
                        throw new ExceptionInInitializerError("Non unique code for " + value);
                    }
                    num = currNum;
                }
                NUMS.put(value, num);
                num++;
            }
        }

        DtsSystemComm() {
            this(-1);
        }

        DtsSystemComm(int num) {
            this.mNum = num;
        }

        public int getNum() {
            return NUMS.get(this).intValue();
        }
    }
}
