package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.AtomicFile;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/** One daily image plus an independent pinned copy. No alarm, service or background polling. */
final class BingWallpaper {
    static final String MODE = "mode";
    static final String DAILY = "bing";
    static final String FIXED = "fixed";
    static final String STATIC = "static";
    static final String BLACK = "black";
    private static final ExecutorService IO = new ThreadPoolExecutor(0, 1, 10, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>());
    private static final List<Consumer<Asset>> waiting = new ArrayList<>();
    private static boolean fetching;
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    static final class Asset {
        final File file;
        final String title, copyright, date;
        Asset(File file, String title, String copyright, String date) {
            this.file = file; this.title = title; this.copyright = copyright; this.date = date;
        }
        JSONObject json() throws Exception {
            return new JSONObject().put("file", file.getName()).put("title", title)
                    .put("copyright", copyright).put("date", date);
        }
    }

    static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(AuroraView.PREFERENCES, Context.MODE_PRIVATE);
    }
    static String mode(Context context) { return preferences(context).getString(MODE, STATIC); }
    private static File directory(Context context) {
        File dir = new File(context.getFilesDir(), "bing-background");
        if (!dir.isDirectory() && !dir.mkdirs()) throw new IllegalStateException("No wallpaper cache");
        return dir;
    }
    static synchronized Asset cached(Context context, boolean fixed) {
        try {
            File dir = directory(context);
            AtomicFile metadata = new AtomicFile(new File(dir, fixed ? "fixed.json" : "daily.json"));
            JSONObject json;
            try (InputStream in = metadata.openRead()) { json = new JSONObject(new String(read(in, 32768), "UTF-8")); }
            String name = json.getString("file");
            if (name.contains("/") || name.contains("\\")) return null;
            File image = new File(dir, name);
            if (!image.isFile() || image.length() == 0) return null;
            return new Asset(image, json.optString("title"), json.optString("copyright"), json.optString("date"));
        } catch (Exception failure) { return null; }
    }

    static synchronized void update(Context context, Consumer<Asset> callback) {
        if (fetching) { waiting.add(callback); return; }
        Context app = context.getApplicationContext();
        SharedPreferences prefs = preferences(app);
        String today = new SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date());
        Asset old = cached(app, false);
        long now = System.currentTimeMillis(), attempted = prefs.getLong("last_attempt", 0);
        if ((old != null && today.equals(prefs.getString("checked_day", "")))
                || (now >= attempted && now - attempted < 30 * 60_000L)) {
            MAIN.post(() -> callback.accept(old));
            return;
        }
        waiting.add(callback);
        fetching = true;
        prefs.edit().putLong("last_attempt", now).apply();
        IO.execute(() -> {
            Asset result = old;
            try {
                byte[] json = request("https://cn.bing.com/HPImageArchive.aspx?format=js&idx=0&n=1&mkt=zh-CN", 131072);
                JSONObject image = new JSONObject(new String(json, "UTF-8")).getJSONArray("images").getJSONObject(0);
                String path = image.getString("url");
                if (!path.startsWith("/th?")) throw new IOException("Unexpected image URL");
                String date = image.getString("startdate");
                String identity = date + "-" + Integer.toHexString(path.hashCode());
                File dir = directory(app);
                File output = new File(dir, "daily-" + identity.replaceAll("[^a-zA-Z0-9-]", "") + ".jpg");
                if (!output.isFile()) {
                    byte[] bytes = request("https://cn.bing.com" + path, 6 * 1024 * 1024);
                    android.graphics.BitmapFactory.Options bounds = new android.graphics.BitmapFactory.Options();
                    bounds.inJustDecodeBounds = true;
                    android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.length, bounds);
                    if (bounds.outWidth < 100 || bounds.outHeight < 100
                            || (long) bounds.outWidth * bounds.outHeight > 16_000_000L)
                        throw new IOException("Invalid image");
                    AtomicFile imageFile = new AtomicFile(output);
                    FileOutputStream stream = imageFile.startWrite();
                    try { stream.write(bytes); imageFile.finishWrite(stream); }
                    catch (Exception failure) { imageFile.failWrite(stream); throw failure; }
                }
                synchronized (BingWallpaper.class) {
                    result = new Asset(output, image.optString("title"), image.optString("copyright"), date);
                    metadata(new File(dir, "daily.json"), result);
                    // Retain the previous image while an open preview might still be showing it.
                    File[] leftovers = dir.listFiles();
                    if (leftovers != null) for (File file : leftovers) {
                        if (file.getName().startsWith("daily-") && file.getName().endsWith(".jpg")
                                && !file.equals(output) && (old == null || !file.equals(old.file))) file.delete();
                    }
                }
                prefs.edit().putString("checked_day", today).putString("last_error", "").apply();
            } catch (Exception failure) {
                prefs.edit().putString("last_error", failure.getClass().getSimpleName()).apply();
            }
            final Asset completed = result;
            List<Consumer<Asset>> callbacks;
            synchronized (BingWallpaper.class) {
                fetching = false; callbacks = new ArrayList<>(waiting); waiting.clear();
            }
            MAIN.post(() -> { for (Consumer<Asset> consumer : callbacks) consumer.accept(completed); });
        });
    }

    static void pin(Context context, Asset shown, Consumer<Boolean> callback) {
        Context app = context.getApplicationContext();
        IO.execute(() -> {
            boolean saved = false;
            try {
                synchronized (BingWallpaper.class) {
                    // Unique name: replacing a favorite never invalidates its old metadata on failure.
                    File dir = directory(app);
                    File target = new File(dir, "fixed-" + System.currentTimeMillis() + ".jpg");
                    AtomicFile targetFile = new AtomicFile(target);
                    FileOutputStream out = targetFile.startWrite();
                    try (InputStream in = new FileInputStream(shown.file)) {
                        byte[] buffer = new byte[8192]; int count;
                        while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
                        targetFile.finishWrite(out);
                    } catch (Exception failure) {
                        targetFile.failWrite(out);
                        throw failure;
                    }
                    metadata(new File(dir, "fixed.json"), new Asset(target, shown.title, shown.copyright, shown.date));
                    File[] files = dir.listFiles();
                    if (files != null) for (File file : files)
                        if (file.getName().startsWith("fixed-") && file.getName().endsWith(".jpg")
                                && !file.equals(target)) file.delete();
                    saved = preferences(app).edit().putString(MODE, FIXED).commit();
                }
            } catch (Exception failure) { /* Keep the existing favorite and background mode. */ }
            final boolean result = saved;
            MAIN.post(() -> callback.accept(result));
        });
    }

    private static void metadata(File path, Asset asset) throws Exception {
        AtomicFile file = new AtomicFile(path);
        FileOutputStream stream = file.startWrite();
        try { stream.write(asset.json().toString().getBytes("UTF-8")); file.finishWrite(stream); }
        catch (Exception failure) { file.failWrite(stream); throw failure; }
    }
    private static byte[] request(String address, int max) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(address).openConnection();
        connection.setConnectTimeout(12000); connection.setReadTimeout(15000);
        connection.setRequestProperty("User-Agent", "CleanTV/1.0");
        try {
            if (connection.getResponseCode() != 200) throw new IOException("HTTP " + connection.getResponseCode());
            try (InputStream in = connection.getInputStream()) { return read(in, max); }
        } finally { connection.disconnect(); }
    }
    private static byte[] read(InputStream in, int max) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(); byte[] buffer = new byte[8192]; int count;
        while ((count = in.read(buffer)) != -1) {
            if (out.size() + count > max) throw new IOException("Wallpaper too large");
            out.write(buffer, 0, count);
        }
        return out.toByteArray();
    }
}
