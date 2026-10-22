package com.gathin.tvhome;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/** Local static background preview; nothing runs after this screen closes. */
public final class BackgroundSettingsActivity extends Activity {
    private BingWallpaperView daily;
    private AuroraView preview;
    private boolean saving;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        preview = new AuroraView(this, null);
        root.addView(preview, new FrameLayout.LayoutParams(-1, -1));
        daily = new BingWallpaperView(this);
        root.addView(daily, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout title = new LinearLayout(this);
        title.setOrientation(LinearLayout.VERTICAL);
        title.setPadding(dp(54), dp(38), dp(54), 0);
        TextView heading = text(getString(R.string.background_settings), 32);
        title.addView(heading);
        TextView hint = text(getString(R.string.background_hint), 16);
        hint.setPadding(0, dp(14), 0, 0);
        title.addView(hint);
        root.addView(title, new FrameLayout.LayoutParams(-1, -2, Gravity.TOP));

        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.VERTICAL);
        LinearLayout modes = new LinearLayout(this);
        modes.setPadding(dp(48), dp(8), dp(48), dp(12));
        TextView black = modeButton(modes, R.string.background_black);
        black.setOnFocusChangeListener((view, focused) -> {
            if (focused) { daily.stop(); daily.setVisibility(View.GONE); preview.setVisibility(View.GONE); }
        });
        black.setOnClickListener(view -> saveMode(BingWallpaper.BLACK));
        TextView bing = modeButton(modes, R.string.background_daily);
        TextView pin = modeButton(modes, R.string.background_pin);
        TextView saved = modeButton(modes, R.string.background_fixed);
        bing.setOnFocusChangeListener((view, focused) -> { if (focused) showPhoto(false); });
        bing.setOnClickListener(view -> saveMode(BingWallpaper.DAILY));
        pin.setOnClickListener(view -> {
            BingWallpaper.Asset shown = daily.asset();
            if (shown == null || daily.getVisibility() != View.VISIBLE || saving) {
                Toast.makeText(this, R.string.background_wait, Toast.LENGTH_LONG).show(); return;
            }
            saving = true;
            BingWallpaper.pin(this, shown, success -> {
                saving = false;
                if (isFinishing() || isDestroyed()) return;
                if (success) done();
                else Toast.makeText(this, R.string.background_save_failed, Toast.LENGTH_LONG).show();
            });
        });
        saved.setOnFocusChangeListener((view, focused) -> { if (focused) showPhoto(true); });
        saved.setOnClickListener(view -> {
            if (BingWallpaper.cached(this, true) == null) {
                Toast.makeText(this, R.string.background_no_fixed, Toast.LENGTH_LONG).show(); return;
            }
            saveMode(BingWallpaper.FIXED);
        });
        controls.addView(modes);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dp(48), dp(12), dp(48), dp(42));
        String[] names = getResources().getStringArray(R.array.background_names);
        int selected = AuroraView.selected(this);
        View initial = null;
        for (int i = 0; i < AuroraView.COUNT; i++) {
            final int palette = i;
            TextView button = text(names[i], 17);
            button.setGravity(Gravity.CENTER);
            button.setFocusable(true);
            button.setClickable(true);
            button.setSelected(BingWallpaper.STATIC.equals(BingWallpaper.mode(this)) && i == selected);
            button.setBackgroundResource(R.drawable.settings_choice_bg);
            button.setContentDescription(names[i]);
            button.setOnFocusChangeListener((view, focused) -> {
                if (focused) {
                    daily.stop(); daily.setVisibility(View.GONE);
                    preview.setVisibility(View.VISIBLE);
                    preview.setPalette(palette);
                }
            });
            button.setOnClickListener(view -> {
                boolean stored = getSharedPreferences(AuroraView.PREFERENCES, Context.MODE_PRIVATE)
                        .edit().putInt(AuroraView.PALETTE, palette).putString(BingWallpaper.MODE, BingWallpaper.STATIC).commit();
                if (!stored) {
                    Toast.makeText(this, R.string.background_save_failed, Toast.LENGTH_LONG).show();
                    return;
                }
                done();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(68), 1);
            lp.setMargins(dp(5), 0, dp(5), 0);
            row.addView(button, lp);
            if (i == selected) initial = button;
        }
        controls.addView(row);
        root.addView(controls, new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM));
        setContentView(root);
        String mode = BingWallpaper.mode(this);
        black.setSelected(BingWallpaper.BLACK.equals(mode));
        bing.setSelected(BingWallpaper.DAILY.equals(mode));
        saved.setSelected(BingWallpaper.FIXED.equals(mode));
        if (BingWallpaper.BLACK.equals(mode)) {
            daily.setVisibility(View.GONE); preview.setVisibility(View.GONE); black.requestFocus();
        } else if (BingWallpaper.DAILY.equals(mode)) { showPhoto(false); bing.requestFocus(); }
        else if (BingWallpaper.FIXED.equals(mode)) { showPhoto(true); saved.requestFocus(); }
        else if (initial != null) initial.requestFocus();
    }

    private TextView modeButton(LinearLayout row, int title) {
        TextView button = text(getString(title), 18);
        button.setGravity(Gravity.CENTER); button.setFocusable(true); button.setClickable(true);
        button.setBackgroundResource(R.drawable.settings_choice_bg);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(54), 1);
        params.setMargins(dp(5), 0, dp(5), 0); row.addView(button, params); return button;
    }
    private void showPhoto(boolean fixed) {
        daily.stop();
        preview.setVisibility(View.VISIBLE);
        if (fixed && BingWallpaper.cached(this, true) == null) {
            daily.setVisibility(View.GONE);
            return;
        }
        daily.setVisibility(View.VISIBLE);
        daily.start(fixed);
    }
    private void saveMode(String mode) {
        if (BingWallpaper.preferences(this).edit().putString(BingWallpaper.MODE, mode).commit()) done();
        else Toast.makeText(this, R.string.background_save_failed, Toast.LENGTH_LONG).show();
    }
    private void done() { setResult(RESULT_OK); finish(); overridePendingTransition(0, 0); }
    @Override protected void onDestroy() { daily.close(); super.onDestroy(); }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private TextView text(String value, int size) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextColor(Color.WHITE);
        text.setTextSize(size);
        return text;
    }
}
