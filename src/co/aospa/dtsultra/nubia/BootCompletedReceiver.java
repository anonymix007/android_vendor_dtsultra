/*
 * Copyright (C) 2025 Paranoid Android
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.dtsultra.nubia;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootCompletedReceiver extends BroadcastReceiver {
    private static final String TAG = "DtsUltra-BCR";

    @Override
    public void onReceive(final Context context, Intent intent) {
        Log.d(TAG, "Received intent: " + intent.getAction());
        if (!intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            return;
        }

        Log.i(TAG, "Boot completed, starting dtsultra");

        DtsUtils dtsUtils = DtsUtils.getInstance(context);
        dtsUtils.onBootCompleted();

        if (dtsUtils.getDtsOn()) {
            DtsUtils.reset();
            dtsUtils = DtsUtils.getInstance(context);
        }
    }
}
