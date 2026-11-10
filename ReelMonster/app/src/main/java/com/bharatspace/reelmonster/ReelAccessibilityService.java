package com.bharatspace.reelmonster;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Level 2 (automatic): notices when a NEW reel / short appears in Instagram or YouTube.
 * It only reads the on-screen text of those two apps, compares it with the previous reel and
 * adds +1 when the reel changes. Nothing is stored except the count, and nothing leaves the phone.
 */
public class ReelAccessibilityService extends AccessibilityService {
    static volatile ReelAccessibilityService instance;

    private final Handler h = new Handler(Looper.getMainLooper());
    private boolean pending, dirty;
    private Set<String> cur, prev;
    private long lastCount, lastLog;

    private static final Set<String> BLOCK = new HashSet<>(Arrays.asList(
            "follow", "following", "subscribe", "subscribed", "share", "like", "comment", "remix", "reels", "reel",
            "shorts", "short", "home", "search", "create", "explore", "messages", "profile", "library",
            "subscriptions", "you", "live", "original audio", "sponsored", "more", "send", "save", "audio",
            "for you", "menu", "back", "close"));

    @Override
    protected void onServiceConnected() {
        instance = this;
        DebugLog.add("Auto-count service connected ✅");
    }

    @Override
    public boolean onUnbind(Intent i) {
        instance = null;
        return super.onUnbind(i);
    }

    @Override
    public void onDestroy() {
        instance = null;
        super.onDestroy();
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent ev) {
        if (ev == null || ev.getPackageName() == null) return;
        String p = ev.getPackageName().toString();

        if (ev.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            MonsterService.onForegroundApp(p);
            if (!Perms.isTarget(p) && !Perms.isNoise(p)) {
                cur = null;
                prev = null;
            }
        }
        if (!Perms.isTarget(p)) return;
        Store st = Store.get(this);
        if (!st.monsterOn() || !Store.AUTO.equals(st.mode())) return;

        if (pending) {
            dirty = true;
        } else {
            pending = true;
            h.postDelayed(scanRunnable, 450);
        }
    }

    private final Runnable scanRunnable = new Runnable() {
        @Override
        public void run() {
            pending = false;
            try {
                scan();
            } catch (Exception e) {
                DebugLog.add("scan error: " + e);
            }
            if (dirty) {
                dirty = false;
                pending = true;
                h.postDelayed(this, 600);
            }
        }
    };

    private void scan() {
        Store st = Store.get(this);
        if (!st.monsterOn() || !Store.AUTO.equals(st.mode())) return;
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null || root.getPackageName() == null) return;
        String pkg = root.getPackageName().toString();
        boolean ig = Perms.IG.equals(pkg), yt = Perms.YT.equals(pkg);
        if (!ig && !yt) return;

        boolean gate = false;
        Set<String> texts = new LinkedHashSet<>();
        Set<String> ids = new LinkedHashSet<>();
        ArrayDeque<AccessibilityNodeInfo> q = new ArrayDeque<>();
        q.add(root);
        int visited = 0;
        while (!q.isEmpty() && visited < 500) {
            AccessibilityNodeInfo node = q.poll();
            visited++;
            String id = node.getViewIdResourceName();
            if (id != null) {
                String s = id.toLowerCase(Locale.ROOT);
                int k = s.indexOf(":id/");
                String shortId = k >= 0 ? s.substring(k + 4) : s;
                if (ids.size() < 14) ids.add(shortId);
                if (ig && shortId.contains("clips")) gate = true;
                if (yt && shortId.contains("reel") && !shortId.contains("shelf") && !shortId.contains("carousel")) gate = true;
            }
            CharSequence tx = node.getText();
            if (tx != null && node.isVisibleToUser()) {
                String s = tx.toString().trim();
                if (useful(s)) texts.add(s.length() > 60 ? s.substring(0, 60) : s);
            }
            for (int i = 0; i < node.getChildCount(); i++) {
                AccessibilityNodeInfo ch = node.getChild(i);
                if (ch != null) q.add(ch);
            }
        }

        if (!gate) {
            cur = null;
            prev = null;
            long now = SystemClock.uptimeMillis();
            if (now - lastLog > 4000) {
                lastLog = now;
                DebugLog.add((ig ? "IG" : "YT") + ": not on reels/shorts. ids: " + ids);
            }
            return;
        }
        if (texts.isEmpty()) return;

        if (cur == null) {
            cur = texts;
            countReel(ig, "first", texts);
        } else if (similar(cur, texts)) {
            // same reel (maybe a button text changed) -> nothing to do
        } else if (prev != null && similar(prev, texts)) {
            prev = cur;
            cur = texts;
            DebugLog.add("went back to the previous reel (not counted)");
        } else {
            prev = cur;
            cur = texts;
            countReel(ig, "next", texts);
        }
    }

    private void countReel(boolean ig, String why, Set<String> t) {
        long now = SystemClock.uptimeMillis();
        if (now - lastCount < 700) {
            DebugLog.add("skipped (too fast)");
            return;
        }
        lastCount = now;
        int c = Store.get(this).addReel(true);
        StringBuilder sb = new StringBuilder();
        int n = 0;
        for (String s : t) {
            if (n++ >= 3) break;
            sb.append(s.length() > 18 ? s.substring(0, 18) : s).append(" | ");
        }
        DebugLog.add("#" + c + " " + (ig ? "IG" : "YT") + " " + why + " -> " + sb);
    }

    private static boolean useful(String s) {
        if (s.length() < 3) return false;
        String l = s.toLowerCase(Locale.ROOT);
        if (BLOCK.contains(l)) return false;
        return !l.matches("[0-9\\s.,:/%+kmb-]+");
    }

    /** True when at least half of the smaller set also appears in the other set. */
    private static boolean similar(Set<String> a, Set<String> b) {
        int common = 0;
        for (String s : a) if (b.contains(s)) common++;
        int min = Math.min(a.size(), b.size());
        return min > 0 && common * 2 >= min;
    }
}
