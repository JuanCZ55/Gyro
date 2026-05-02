package com.supra.gyro;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.provider.Settings;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

public class MiBotonRotacionService extends TileService {

    @SuppressLint("StartActivityAndCollapseDeprecated")
    @Override
    public void onClick() {
        super.onClick();

        if (Settings.System.canWrite(this)) {
            Intent serviceIntent = new Intent(this, RotacionCoreService.class);
            // Si el servicio no está corriendo, startForegroundService lo inicia.
            // Si ya está corriendo, el sistema entrega el Intent para rotar.
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
            // Opción 1: Minimalista (Estado Estático)
            // Siempre muestra "Giro" (definido en strings.xml)
            tile.setLabel(getString(R.string.tile_label));
            tile.setSubtitle(null);

            if (RotacionCoreService.isRunning) {
                // Estado ACTIVO (Azul/Color de énfasis)
                tile.setState(Tile.STATE_ACTIVE);
            } else {
                // Estado INACTIVO (Gris)
                tile.setState(Tile.STATE_INACTIVE);
            }
            
            tile.updateTile();
        }
    }
}
