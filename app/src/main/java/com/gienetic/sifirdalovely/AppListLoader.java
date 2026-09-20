package com.gienetic.sifirdalovely;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Loaded off main thread — caller's responsibility. */
public class AppListLoader {

    public static class AppEntry implements Comparable<AppEntry> {
        public final String packageName;
        public final String label;
        public final boolean isSystem;
        public final Drawable icon;

        AppEntry(String packageName, String label, boolean isSystem, Drawable icon) {
            this.packageName = packageName;
            this.label = label;
            this.isSystem = isSystem;
            this.icon = icon;
        }

        @Override
        public int compareTo(AppEntry other) {
            int t = Boolean.compare(this.isSystem, other.isSystem);
            if (t != 0) return t;
            return this.label.compareToIgnoreCase(other.label);
        }
    }

    public static List<AppEntry> loadInstalledApps(Context ctx, boolean includeSystem) {
        PackageManager pm = ctx.getPackageManager();
        List<ApplicationInfo> all;
        try {
            all = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        } catch (Exception e) {
            return Collections.emptyList();
        }
        List<AppEntry> out = new ArrayList<>();
        for (ApplicationInfo info : all) {
            boolean system = (info.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
            if (system && !includeSystem) continue;
            String label;
            try { label = pm.getApplicationLabel(info).toString(); }
            catch (Exception e) { label = info.packageName; }
            Drawable icon;
            try { icon = pm.getApplicationIcon(info); }
            catch (Exception e) { icon = null; }
            out.add(new AppEntry(info.packageName, label, system, icon));
        }
        Collections.sort(out);
        return out;
    }
}
