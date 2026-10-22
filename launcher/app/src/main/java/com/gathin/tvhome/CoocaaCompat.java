package com.gathin.tvhome;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.content.ComponentName;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.widget.Toast;

/** Explicit entry points for older Coocaa firmware, which omits normal launcher categories. */
final class CoocaaCompat {
    static final String[] SYSTEM_APPS = { "com.tianci.setting", "com.coocaa.tvmanager",
            "com.skyworth.tv", "com.tianci.localmedia" };
    private CoocaaCompat() { }

    static boolean isSkyworth(Context context) {
        try {
            context.getPackageManager().getPackageInfo("com.tianci.system", 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    static Intent appIntent(Context context, String pkg) {
        PackageManager pm = context.getPackageManager();
        Intent intent = pm.getLeanbackLaunchIntentForPackage(pkg);
        if (intent == null) intent = pm.getLaunchIntentForPackage(pkg);
        if (intent != null) return intent;
        try {
            PackageInfo info = pm.getPackageInfo(pkg, PackageManager.GET_ACTIVITIES);
            ActivityInfo candidate = null;
            if (info.activities != null) for (ActivityInfo activity : info.activities) {
                if (!activity.exported || !activity.enabled || activity.permission != null) continue;
                if (candidate == null || (!candidate.name.contains("Main") && activity.name.contains("Main")))
                    candidate = activity;
            }
            if (candidate != null) return new Intent(Intent.ACTION_MAIN)
                    .setComponent(new ComponentName(pkg, candidate.name));
        } catch (PackageManager.NameNotFoundException ignored) { }
        return null;
    }

    static ResolveInfo entry(Context context, String pkg) {
        Intent intent = appIntent(context, pkg);
        if (intent == null) return null;
        ResolveInfo resolved = context.getPackageManager().resolveActivity(intent, 0);
        if (resolved != null) return resolved;
        if (intent.getComponent() == null) return null;
        try {
            ResolveInfo entry = new ResolveInfo();
            entry.activityInfo = context.getPackageManager().getActivityInfo(intent.getComponent(), 0);
            return entry;
        } catch (PackageManager.NameNotFoundException e) { return null; }
    }

    static void openInputs(Activity activity) {
        Intent intent = new Intent("coocaa.intent.action.SOURCE_DIALOG")
                .setComponent(new ComponentName("com.tianci.movieplatform",
                        "com.ccos.cooui.homepage.tvsource.SourceDialogActivity"));
        try { activity.startActivity(intent); }
        catch (RuntimeException unavailable) { Toast.makeText(activity, R.string.not_available, Toast.LENGTH_SHORT).show(); }
    }

    static void home(Context context) {
        context.startActivity(new Intent(context, MainActivity.class).setAction(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                        | Intent.FLAG_ACTIVITY_NO_ANIMATION),
                ActivityOptions.makeCustomAnimation(context, 0, 0).toBundle());
    }
}
