package com.supra.gyro;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.service.quicksettings.TileService;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.Surface;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class RotacionCoreService extends Service {
    private static final String TAG = "RotacionCoreService";
    private static final String CHANNEL_ID = "RotacionServiceChannel";
    private static final int NOTIFICATION_ID = 1;
    private static final String PREFS_NAME = "GyroPrefs";
    private static final String KEY_ROTACION_ACTUAL = "rotacion_actual";
    private static final String KEY_MODO_FUNCIONAMIENTO = "modo_funcionamiento";

    public static final String ACTION_TRIGGER_ROTATION = "com.supra.gyro.TRIGGER_ROTATION";
    public static final String ACTION_STOP_SERVICE = "com.supra.gyro.STOP_SERVICE";
    
    public static boolean isRunning = false;
    private boolean isFirstStartCommand = true;

    @Override
    public void onCreate() {
        super.onCreate();
        isRunning = true;
        isFirstStartCommand = true;
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP_SERVICE.equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }

        startForegroundService();
        
        if (isFirstStartCommand) {
            isFirstStartCommand = false;
            procesarRotacionInicial();
        } else {
            procesarCicloDeRotacion();
        }
        
        return START_STICKY;
    }

    private void startForegroundService() {
        Intent stopIntent = new Intent(this, RotacionCoreService.class);
        stopIntent.setAction(ACTION_STOP_SERVICE);
        PendingIntent stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, 
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.notif_title))
                .setContentText(getString(R.string.notif_content))
                .setSmallIcon(R.drawable.ic_logo)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Detener Servicio", stopPendingIntent)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    private void procesarRotacionInicial() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int modo = prefs.getInt(KEY_MODO_FUNCIONAMIENTO, MainActivity.MODO_INTELIGENTE);
        
        if (modo == MainActivity.MODO_INTELIGENTE) {
            ejecutarModoInteligente();
        } else {
            int actual = prefs.getInt(KEY_ROTACION_ACTUAL, Surface.ROTATION_0);
            aplicarRotacion(actual);
        }
    }

    private void procesarCicloDeRotacion() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int modo = prefs.getInt(KEY_MODO_FUNCIONAMIENTO, MainActivity.MODO_INTELIGENTE);
        int actual = prefs.getInt(KEY_ROTACION_ACTUAL, Surface.ROTATION_0);

        switch (modo) {
            case MainActivity.MODO_INTELIGENTE:
                // En modo inteligente, re-detectamos con el sensor
                ejecutarModoInteligente();
                break;
            case MainActivity.MODO_ALTERNAR:
                // Ciclo infinito: Vertical (0) -> Horizontal (90) -> Vertical (0)
                int nuevaAlt = (actual == Surface.ROTATION_0) ? Surface.ROTATION_90 : Surface.ROTATION_0;
                aplicarRotacion(nuevaAlt);
                break;
            case MainActivity.MODO_RELOJ:
                // Ciclo infinito: 0 -> 90 -> 180 -> 270 -> 0
                int nuevaReloj;
                switch (actual) {
                    case Surface.ROTATION_0: nuevaReloj = Surface.ROTATION_90; break;
                    case Surface.ROTATION_90: nuevaReloj = Surface.ROTATION_180; break;
                    case Surface.ROTATION_180: nuevaReloj = Surface.ROTATION_270; break;
                    default: nuevaReloj = Surface.ROTATION_0; break;
                }
                aplicarRotacion(nuevaReloj);
                break;
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
                } else if (orientation <= 135) {
                    nuevaRotacion = Surface.ROTATION_270;
                } else if (orientation <= 225) {
                    nuevaRotacion = Surface.ROTATION_180;
                } else {
                    nuevaRotacion = Surface.ROTATION_90;
                }

                aplicarRotacion(nuevaRotacion);
                this.disable();
            }
        };

        if (listener.canDetectOrientation()) {
            listener.enable();
        } else {
            aplicarRotacion(Surface.ROTATION_0);
        }
    }

    private void aplicarRotacion(int rotacion) {
        if (Settings.System.canWrite(this)) {
            try {
                // Forzar que la rotación automática esté desactivada
                Settings.System.putInt(getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 0);
                Settings.System.putInt(getContentResolver(), Settings.System.USER_ROTATION, rotacion);
                
                getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                        .edit()
                        .putInt(KEY_ROTACION_ACTUAL, rotacion)
                        .apply();

                TileService.requestListeningState(this, new ComponentName(this, MiBotonRotacionService.class));
            } catch (Exception e) {
                Log.e(TAG, "Error al aplicar rotación", e);
            }
        }
    }

    @Override
    public void onDestroy() {
        isRunning = false;
        // NO reactivamos la rotación automática aquí para cumplir con la petición del usuario.
        try {
            TileService.requestListeningState(this, new ComponentName(this, MiBotonRotacionService.class));
        } catch (Exception e) {
            Log.e(TAG, "Error al notificar al botón", e);
        }
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.notif_channel_name),
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }
}
