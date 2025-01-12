/*
 * Copyright (C) 2019 The Android Open Source Project
 *           (C) 2023-25 Paranoid Android
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.dtsultra.nubia;

import static com.android.settingslib.drawer.TileUtils.META_DATA_PREFERENCE_SUMMARY;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

import co.aospa.dtsultra.nubia.R;

import java.util.Arrays;
import java.util.List;

/** Provide preference summary for injected items. */
public class SummaryProvider extends ContentProvider {

    private static final String KEY_DTSULTRA = "dtsultra";

    @Override
    public Bundle call(String method, String uri, Bundle extras) {
        final Bundle bundle = new Bundle();
        String summary;
        switch (method) {
            case KEY_DTSULTRA:
                summary = getDtsUltraSummary();
                break;
            default:
                throw new IllegalArgumentException("Unknown method: " + method);
        }
        bundle.putString(META_DATA_PREFERENCE_SUMMARY, summary);
        return bundle;
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getType(Uri uri) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    private String getDtsUltraSummary() {
        final DtsUtils dtsUtils = DtsUtils.getInstance(getContext());
        if (!dtsUtils.getDtsOn()) {
            return getContext().getString(R.string.dtsultra_off);
        }
        final String profileName = dtsUtils.getProfileName();
        final String presetName = dtsUtils.getPresetName();
        if (profileName == null) {
            return getContext().getString(R.string.dtsultra_on);
        } else if (presetName == null) {
            return getContext().getString(R.string.dtsultra_on_profile, profileName);
        } else {
            return getContext().getString(R.string.dtsultra_on_profile_preset, profileName, presetName);
        }
    }

}
