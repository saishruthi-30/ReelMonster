package com.bharatspace.reelmonster;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;

final class Perms {
    static final String IG = "com.instagram.android";
    static final String YT = "com.google.android.youtube";

    static boolean overlay(Context c) {
        return Settings.canDrawOverlays(c);
    }

    static boolean notifications(Context c) {
        return Build.VERSION.SDK_INT < 33
                || c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }

    static boolean accessibility(Context c) {
        String en = Settings.Secure.getString(c.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (en == null) return false;
        ComponentName cn = new ComponentName(c, ReelAccessibilityService.class);
        String e = en.toLowerCase();
        return e.contains(cn.flattenToString().toLowerCase()) || e.contains(cn.flattenToShortString().toLowerCase());
    }

    static boolean isTarget(String p) {
        return IG.equals(p) || YT.equals(p);
    }

    /** Packages whose window changes should not decide whether we are "inside" Instagram/YouTube. */
    static boolean isNoise(String p) {
        if (p == null) return true;
        return p.equals("com.android.systemui") || p.equals("com.bharatspace.reelmonster")
                || p.contains("inputmethod") || p.contains("keyboard");
    }
}
