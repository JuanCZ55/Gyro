package com.supra.gyro;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.provider.Settings;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.view.Surface;
import android.content.SharedPreferences;

public class MiBotonRotacionService extends TileService {

    private static final String PREFS_NAME = "GyroPrefs";
    private static final String KEY_ROTACION_ACTUAL = "rotacion_actual";

    @SuppressLint("StartActivityAndCollapseDeprecated")
    @Override
    public void onClick() {
        super.onClick();

        if (Settings.System.canWrite(this)) {
            Intent serviceIntent = new Intent(this, RotacionCoreService.class);
            serviceIntent.setAction(RotacionCoreService.ACTION_TRIGGER_ROTATION);
            startForegroundService(serviceIntent);
        } else {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivityAndCollapse(intent);
        }
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        updateTileState();
    }

    private void updateTileState() {
        Tile tile = getQsTile();
        if (tile != null) {
            if (RotacionCoreService.isRunning) {
                // Estado ACTIVO (Color)
                tile.setState(Tile.STATE_ACTIVE);
                
                SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                int rotacion = prefs.getInt(KEY_ROTACION_ACTUAL, Surface.ROTATION_0);
                
                // IMPORTANTE: Ponemos el estado en el Label y dejamos el Subtitle nulo.
                // Esto elimina el símbolo '>' en Android 12, 13 y 14.
                tile.setLabel(getSubtitleForRotation(rotacion));
                tile.setSubtitle(null);
            } else {
                // Estado INACTIVO (Gris)
                tile.setState(Tile.STATE_INACTIVE);
                tile.setLabel(getString(R.string.tile_label)); // "Girar"
                tile.setSubtitle(null);
            }
            tile.updateTile();
        }
    }

    private String getSubtitleForRotation(int rotacion) {
        switch (rotacion) {
            case Surface.ROTATION_90: return getString(R.string.rot_horiz_izq);
            case Surface.ROTATION_270: return getString(R.string.rot_horiz_der);
            case Surface.ROTATION_180: return getString(R.string.rot_vert_inv);
            default: return getString(R.string.rot_vertical);
        }
    }
}
