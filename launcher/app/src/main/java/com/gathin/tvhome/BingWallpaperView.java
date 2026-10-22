package com.gathin.tvhome;

import android.content.Context;
import android.graphics.*;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

/** A single cached static image; the download never blocks Home or a remote key callback. */
final class BingWallpaperView extends FrameLayout {
    private final ImageView image;
    private final TextView credit;
    private BingWallpaper.Asset asset;
    private boolean active, fixed, closed;
    BingWallpaperView(Context context) {
        super(context);
        image = new ImageView(context);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        addView(image, new FrameLayout.LayoutParams(-1, -1));
        credit = new TextView(context);
        credit.setTextColor(0xAFFFFFFF); credit.setTextSize(10);
        credit.setSingleLine(true); credit.setEllipsize(TextUtils.TruncateAt.END);
        int padding = Math.round(16 * getResources().getDisplayMetrics().density);
        credit.setPadding(padding, 0, padding, padding / 2);
        credit.setGravity(Gravity.END);
        addView(credit, new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM));
        setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
    }
    BingWallpaper.Asset asset() { return asset; }
    void start(boolean pinned) {
        if (fixed != pinned) clear();
        fixed = pinned; active = true;
        show(BingWallpaper.cached(getContext(), pinned));
        update();
    }
    void stop() { active = false; }
    void close() { active = false; closed = true; }
    void update() {
        if (!active || closed || fixed) return;
        BingWallpaper.update(getContext(), next -> { if (active && !closed && !fixed) show(next); });
    }
    private void clear() {
        image.setImageDrawable(null);
        credit.setText("");
        asset = null;
    }
    private void show(BingWallpaper.Asset next) {
        if (next == null || (asset != null && asset.file.equals(next.file))) return;
        try {
            BitmapFactory.Options opts = new BitmapFactory.Options(); opts.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(next.file.getPath(), opts);
            int w = opts.outWidth, h = opts.outHeight;
            opts.inJustDecodeBounds = false;
            opts.inSampleSize = WallpaperSize.sampleSize(w, h, 1280, 720);
            int[] scale = WallpaperSize.coverScale(w / opts.inSampleSize, h / opts.inSampleSize, 1280, 720);
            if (scale != null) { opts.inScaled = true; opts.inDensity = scale[0]; opts.inTargetDensity = scale[1]; }
            opts.inPreferredConfig = Bitmap.Config.RGB_565; opts.inMutable = true;
            Bitmap bitmap = BitmapFactory.decodeFile(next.file.getPath(), opts);
            if (bitmap == null) return;
            Canvas canvas = new Canvas(bitmap); Paint paint = new Paint();
            paint.setShader(new LinearGradient(0, 0, 0, bitmap.getHeight(),
                    new int[]{0x60040A18, 0x30040A18, 0xC0040A18}, new float[]{0, .45f, 1}, Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, bitmap.getWidth(), bitmap.getHeight(), paint);
            image.setImageBitmap(bitmap); asset = next;
            credit.setText("Bing · " + next.copyright);
        } catch (Exception | OutOfMemoryError failure) { /* Keep the last image or the static fallback. */ }
    }
}
