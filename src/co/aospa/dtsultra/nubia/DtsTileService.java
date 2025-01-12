/*
 * Copyright (C) 2023-25 Paranoid Android
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.dtsultra.nubia;

import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

public class DtsTileService extends TileService {
    private static final String TAG = "DtsTileService";

    @Override
    public void onStartListening() {
        Tile tile = getQsTile();
        DtsUtils dtsUtils = DtsUtils.getInstance(getApplicationContext());
        if (dtsUtils.getDtsOn()) {
            tile.setState(Tile.STATE_ACTIVE);
        } else {
            tile.setState(Tile.STATE_INACTIVE);
        }
        tile.setSubtitle(dtsUtils.getProfileName());
        tile.updateTile();
        super.onStartListening();
    }

    @Override
    public void onClick() {
        Tile tile = getQsTile();
        DtsUtils dtsUtils = DtsUtils.getInstance(getApplicationContext());
        if (dtsUtils.getDtsOn()) {
            dtsUtils.setDtsOn(false);
            tile.setState(Tile.STATE_INACTIVE);
        } else {
            dtsUtils.setDtsOn(true);
            tile.setState(Tile.STATE_ACTIVE);
        }
        tile.updateTile();
        super.onClick();
    }
}
