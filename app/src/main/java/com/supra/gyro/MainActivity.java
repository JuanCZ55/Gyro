package com.supra.gyro;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.Surface;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "GyroPrefs";
    private static final String KEY_MODO_FUNCIONAMIENTO = "modo_funcionamiento";
    
    public static final int MODO_INTELIGENTE = 0;
    public static final int MODO_ALTERNAR = 1;
    public static final int MODO_RELOJ = 2;

    private TextView tvRotacionActual;
    private Button btnPermisoEscritura;
    private Button btnPermisoBateria;

    private final ContentObserver rotationObserver = new ContentObserver(new Handler(Looper.getMainLooper())) {
        @Override
        public void onChange(boolean selfChange) {
            actualizarEstadoUI();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvRotacionActual = findViewById(R.id.tv_rotacion_actual);
        btnPermisoEscritura = findViewById(R.id.btn_otorgar_permiso);
        btnPermisoBateria = findViewById(R.id.btn_ignorar_bateria);
        RadioGroup rgModo = findViewById(R.id.rg_modo_funcionamiento);
        
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int modoActual = prefs.getInt(KEY_MODO_FUNCIONAMIENTO, MODO_INTELIGENTE);

        if (modoActual == MODO_INTELIGENTE) rgModo.check(R.id.rb_modo_inteligente);
        else if (modoActual == MODO_ALTERNAR) rgModo.check(R.id.rb_modo_alternar);
        else if (modoActual == MODO_RELOJ) rgModo.check(R.id.rb_modo_reloj);

        rgModo.setOnCheckedChangeListener((group, checkedId) -> {
            int nuevoModo = MODO_INTELIGENTE;
            if (checkedId == R.id.rb_modo_alternar) nuevoModo = MODO_ALTERNAR;
            else if (checkedId == R.id.rb_modo_reloj) nuevoModo = MODO_RELOJ;
            
            prefs.edit().putInt(KEY_MODO_FUNCIONAMIENTO, nuevoModo).apply();
            Toast.makeText(this, getString(R.string.toast_modo_updated), Toast.LENGTH_SHORT).show();
        });

        btnPermisoEscritura.setOnClickListener(v -> {
            if (Settings.System.canWrite(this)) {
                Toast.makeText(this, getString(R.string.toast_perm_already), Toast.LENGTH_SHORT).show();
            } else {
                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }
        });

        btnPermisoBateria.setOnClickListener(v -> {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm.isIgnoringBatteryOptimizations(getPackageName())) {
                Toast.makeText(this, getString(R.string.toast_perm_already), Toast.LENGTH_SHORT).show();
            } else {
                Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }
        });

        // Observar cambios en la rotación del sistema
        getContentResolver().registerContentObserver(
                Settings.System.getUriFor(Settings.System.USER_ROTATION),
                false, rotationObserver);
        
        getContentResolver().registerContentObserver(
                Settings.System.getUriFor(Settings.System.ACCELEROMETER_ROTATION),
                false, rotationObserver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        actualizarEstadoUI();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        getContentResolver().unregisterContentObserver(rotationObserver);
    }

    private void actualizarEstadoUI() {
        // 1. Actualizar botones de permisos (Deshabilitar si ya están concedidos)
        if (Settings.System.canWrite(this)) {
            btnPermisoEscritura.setText(getString(R.string.perm_write_settings) + " (" + getString(R.string.perm_granted) + ")");
            btnPermisoEscritura.setEnabled(false);
        }

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm.isIgnoringBatteryOptimizations(getPackageName())) {
            btnPermisoBateria.setText(getString(R.string.perm_battery_opt) + " (" + getString(R.string.perm_granted) + ")");
            btnPermisoBateria.setEnabled(false);
        }

        // 2. Detectar y mostrar la rotación actual del sistema en tiempo real
        try {
            int autoRotate = Settings.System.getInt(getContentResolver(), Settings.System.ACCELEROMETER_ROTATION);
            if (autoRotate == 1) {
                tvRotacionActual.setText(String.format(getString(R.string.status_current_rotation), getString(R.string.rot_auto)));
            } else {
                int rotation = Settings.System.getInt(getContentResolver(), Settings.System.USER_ROTATION);
                String label;
                switch (rotation) {
                    case Surface.ROTATION_90: label = getString(R.string.rot_horiz_izq); break;
                    case Surface.ROTATION_180: label = getString(R.string.rot_vert_inv); break;
                    case Surface.ROTATION_270: label = getString(R.string.rot_horiz_der); break;
                    default: label = getString(R.string.rot_vertical); break;
                }
                tvRotacionActual.setText(String.format(getString(R.string.status_current_rotation), label));
            }
        } catch (Settings.SettingNotFoundException e) {
            tvRotacionActual.setText(String.format(getString(R.string.status_current_rotation), getString(R.string.rot_unknown)));
        }
    }
}