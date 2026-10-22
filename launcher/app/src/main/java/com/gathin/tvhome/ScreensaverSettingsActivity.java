package com.gathin.tvhome;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public final class ScreensaverSettingsActivity extends Activity {
    private final int[] minutes = {0, 2, 5, 10, 15};
    private final TextView[] options = new TextView[5];
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(64), dp(42), dp(64), dp(42));
        root.setBackgroundColor(0xFF101419);
        root.addView(text(getString(R.string.clock_settings), 32));
        TextView hint = text(getString(R.string.clock_hint), 17);
        hint.setPadding(0, dp(22), 0, dp(32));
        root.addView(hint);
        LinearLayout row = new LinearLayout(this);
        for (int n = 0; n < minutes.length; n++) {
            final int value = minutes[n];
            TextView option = text(value == 0 ? getString(R.string.off) : getString(R.string.clock_minutes, value), 20);
            option.setGravity(Gravity.CENTER); option.setFocusable(true); option.setClickable(true);
            option.setBackgroundResource(R.drawable.settings_choice_bg);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(60), 1);
            lp.setMargins(dp(4), 0, dp(4), 0);
            row.addView(option, lp); options[n] = option;
            option.setOnClickListener(v -> {
                if (!getSharedPreferences(HomeIdleClock.PREFS, 0).edit().putInt(HomeIdleClock.MINUTES, value).commit()) {
                    Toast.makeText(this, R.string.background_save_failed, Toast.LENGTH_SHORT).show(); return;
                }
                setResult(RESULT_OK); refresh();
            });
        }
        root.addView(row);
        TextView preview = text(getString(R.string.clock_preview), 20);
        preview.setGravity(Gravity.CENTER_VERTICAL); preview.setPadding(dp(20), 0, dp(20), 0);
        preview.setFocusable(true); preview.setClickable(true);
        preview.setBackgroundResource(R.drawable.settings_choice_bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(60)); lp.topMargin = dp(28);
        root.addView(preview, lp);
        preview.setOnClickListener(v -> startActivity(new Intent(this, ClockActivity.class)));
        setContentView(root);
        refresh();
        for (int n=0; n<minutes.length; n++) if (minutes[n] == HomeIdleClock.minutes(this)) options[n].requestFocus();
    }
    private void refresh() {
        for (int n=0; n<minutes.length; n++) options[n].setSelected(minutes[n] == HomeIdleClock.minutes(this));
    }
    private int dp(int x) { return Math.round(x * getResources().getDisplayMetrics().density); }
    private TextView text(String value, int size) {
        TextView v = new TextView(this); v.setText(value); v.setTextSize(size); v.setTextColor(Color.WHITE); return v;
    }
}
