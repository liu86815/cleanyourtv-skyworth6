package com.gathin.tvhome;

import android.app.Activity;
import android.app.TimePickerDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

/** D-pad friendly daily shutdown controls. */
public final class ShutdownSettingsActivity extends Activity {
    private TextView enabledRow;
    private TextView timeRow;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_shutdown_settings);
        enabledRow = findViewById(R.id.shutdownEnabled);
        timeRow = findViewById(R.id.shutdownTime);

        enabledRow.setOnClickListener(v -> {
            boolean enabled = !ShutdownScheduler.isEnabled(this);
            ShutdownScheduler.preferences(this).edit()
                    .putBoolean(ShutdownScheduler.KEY_ENABLED, enabled).apply();
            ShutdownScheduler.schedule(this);
            refresh();
            if (enabled) showScheduledToast();
        });
        timeRow.setOnClickListener(v -> new TimePickerDialog(this, (picker, hour, minute) -> {
            SharedPreferences.Editor edit = ShutdownScheduler.preferences(this).edit();
            edit.putInt(ShutdownScheduler.KEY_HOUR, hour)
                    .putInt(ShutdownScheduler.KEY_MINUTE, minute).apply();
            ShutdownScheduler.schedule(this);
            refresh();
            if (ShutdownScheduler.isEnabled(this)) showScheduledToast();
        }, ShutdownScheduler.hour(this), ShutdownScheduler.minute(this), true).show());

        ShutdownScheduler.schedule(this);
        refresh();
        enabledRow.requestFocus();
    }

    private void refresh() {
        boolean enabled = ShutdownScheduler.isEnabled(this);
        enabledRow.setText(getString(R.string.shutdown_enabled,
                getString(enabled ? R.string.on : R.string.off)));
        timeRow.setText(getString(R.string.shutdown_time,
                ShutdownScheduler.hour(this), ShutdownScheduler.minute(this)));
        timeRow.setEnabled(enabled);
        timeRow.setAlpha(enabled ? 1f : 0.5f);
    }

    private void showScheduledToast() {
        Toast.makeText(this, getString(R.string.shutdown_scheduled,
                ShutdownScheduler.hour(this), ShutdownScheduler.minute(this)), Toast.LENGTH_SHORT).show();
    }
}
