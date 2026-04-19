package com.supra.gyro;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.Surface;

public class MiBotonRotacionService extends TileService {

    private static final String TAG = "GyroService";
    private static final String PREFS_NAME = "GyroPrefs";
    private static final String KEY_ROTACION_ACTUAL = "rotacion_actual";
    private static final String KEY_MODO_FUNCIONAMIENTO = "modo_funcionamiento";

    @Override
    public void onClick() {
        super.onClick();

        if (Settings.System.canWrite(this)) {
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            int modo = prefs.getInt(KEY_MODO_FUNCIONAMIENTO, MainActivity.MODO_INTELIGENTE);

            switch (modo) {
                case MainActivity.MODO_INTELIGENTE:
                    ejecutarModoInteligente();
                    break;
                case MainActivity.MODO_ALTERNAR:
                    ejecutarModoAlternar(prefs);
                    break;
                case MainActivity.MODO_RELOJ:
                    ejecutarModoReloj(prefs);
                    break;
            }
        } else {
            // Si no hay permiso, abrimos la app para que el usuario lo otorgue
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startActivityAndCollapse(pendingIntent);
            } else {
                // Para versiones anteriores a Android 14
                startActivityAndCollapse(intent);
            }
        }
    }

    private void ejecutarModoInteligente() {
        OrientationEventListener listener = new OrientationEventListener(this) {
            @Override
            public void onOrientationChanged(int orientation) {
                if (orientation == ORIENTATION_UNKNOWN) return;

                int nuevaRotacion;
                if (orientation > 315 || orientation <= 45) {
                    nuevaRotacion = Surface.ROTATION_0;
                } else if (orientation > 45 && orientation <= 135) {
                    nuevaRotacion = Surface.ROTATION_270;
                } else if (orientation > 135 && orientation <= 225) {
                    nuevaRotacion = Surface.ROTATION_180;
                } else {
                    nuevaRotacion = Surface.ROTATION_90;
                }

                aplicarRotacion(nuevaRotacion);
                disable(); // Apagamos el sensor tras detectar la posición
            }
        };

        if (listener.canDetectOrientation()) {
            listener.enable();
        } else {
            // Fallback si el dispositivo no tiene sensor habilitado
            ejecutarModoAlternar(getSharedPreferences(PREFS_NAME, MODE_PRIVATE));
        }
    }

    private void ejecutarModoAlternar(SharedPreferences prefs) {
        int actual = prefs.getInt(KEY_ROTACION_ACTUAL, Surface.ROTATION_0);
        int nueva = (actual == Surface.ROTATION_0) ? Surface.ROTATION_90 : Surface.ROTATION_0;
        aplicarRotacion(nueva);
    }

    private void ejecutarModoReloj(SharedPreferences prefs) {
        int actual = prefs.getInt(KEY_ROTACION_ACTUAL, Surface.ROTATION_0);
        int nueva;
        switch (actual) {
            case Surface.ROTATION_0: nueva = Surface.ROTATION_90; break;
            case Surface.ROTATION_90: nueva = Surface.ROTATION_180; break;
            case Surface.ROTATION_180: nueva = Surface.ROTATION_270; break;
            default: nueva = Surface.ROTATION_0; break;
        }
        aplicarRotacion(nueva);
    }

    private void aplicarRotacion(int rotacionConstante) {
        try {
            // Desactivar rotación automática primero
            Settings.System.putInt(getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 0);
            // Aplicar la rotación deseada
            Settings.System.putInt(getContentResolver(), Settings.System.USER_ROTATION, rotacionConstante);
            
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                    .edit()
                    .putInt(KEY_ROTACION_ACTUAL, rotacionConstante)
                    .apply();
                    
            actualizarBotonUI(rotacionConstante);
        } catch (Exception e) {
            Log.e(TAG, "Error al aplicar rotación", e);
        }
    }

    private void actualizarBotonUI(int rotacionConstante) {
        Tile tile = getQsTile();
        if (tile != null) {
            String subtitulo;
            switch (rotacionConstante) {
                case Surface.ROTATION_90: subtitulo = getString(R.string.rot_horiz_izq); break;
                case Surface.ROTATION_270: subtitulo = getString(R.string.rot_horiz_der); break;
                case Surface.ROTATION_180: subtitulo = getString(R.string.rot_vert_inv); break;
                default: subtitulo = getString(R.string.rot_vertical); break;
            }
            tile.setState(Tile.STATE_ACTIVE);
            
            // Como minSdk es 31, setSubtitle siempre está disponible (API 29+)
            tile.setSubtitle(subtitulo);
            tile.updateTile();
        }
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        int ultima = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getInt(KEY_ROTACION_ACTUAL, Surface.ROTATION_0);
        actualizarBotonUI(ultima);
    }
}
