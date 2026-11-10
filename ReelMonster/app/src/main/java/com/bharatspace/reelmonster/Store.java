package com.bharatspace.reelmonster;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Everything is saved on the phone in SharedPreferences, so counts survive restarts. */
public class Store {
    public interface Listener {
        void onStoreChanged(boolean countUp);
    }

    public static final String MANUAL = "manual", AUTO = "auto";
    public static final int SEC_PER_REEL = 25;

    /** id, emoji, name, how to unlock */
    public static final String[][] BADGES = {
            {"first", "🍽️", "First Munch", "Count your very first reel"},
            {"stop", "🛑", "Stop Hero", "Obey a stop alert"},
            {"s3", "🌤️", "3-Day Calm", "Stay under your limit 3 days in a row"},
            {"s7", "🏅", "Week Warrior", "7 days in a row under your limit"},
            {"light", "🥗", "Light Snacker", "End a day at half your limit or less"},
            {"bee", "🐝", "Busy Bee", "Finish 5 do-something-else tasks"},
            {"master", "👑", "Monster Master", "Reach 500 points"}
    };

    private static Store inst;

    public static synchronized Store get(Context c) {
        if (inst == null) inst = new Store(c.getApplicationContext());
        return inst;
    }

    private final SharedPreferences sp;
    private final List<Listener> listeners = new ArrayList<>();
    private static final SimpleDateFormat DF = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    private Store(Context c) {
        sp = c.getSharedPreferences("reel_monster", Context.MODE_PRIVATE);
    }

    // ---------- listeners ----------
    public void addListener(Listener l) {
        if (!listeners.contains(l)) listeners.add(l);
    }

    public void removeListener(Listener l) {
        listeners.remove(l);
    }

    private void fire(boolean up) {
        for (Listener l : new ArrayList<>(listeners)) l.onStoreChanged(up);
    }

    // ---------- dates ----------
    public static String dayKey(int offset) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_YEAR, offset);
        synchronized (DF) {
            return DF.format(c.getTime());
        }
    }

    public static String today() {
        return dayKey(0);
    }

    // ---------- profile ----------
    public boolean onboarded() { return sp.getBoolean("onboarded", false); }
    public void setOnboarded(boolean v) { sp.edit().putBoolean("onboarded", v).apply(); }
    public String role() { return sp.getString("role", Advice.STUDENT); }
    public int age() { return sp.getInt("age", 18); }
    public int safeMinutes() { return sp.getInt("safeMin", 45); }

    public void saveProfile(String role, int age, int minutes, int limit) {
        sp.edit().putString("role", role).putInt("age", age).putInt("safeMin", minutes).putInt("limit", limit)
                .remove("na_" + today()).apply();
        fire(false);
    }

    public int limit() { return sp.getInt("limit", 100); }

    public void setLimit(int v) {
        sp.edit().putInt("limit", Math.max(5, Math.min(500, v))).remove("na_" + today()).apply();
        fire(false);
    }

    public String mode() { return sp.getString("mode", MANUAL); }
    public void setMode(String m) { sp.edit().putString("mode", m).apply(); fire(false); }
    public boolean monsterOn() { return sp.getBoolean("on", false); }
    public void setMonsterOn(boolean v) { sp.edit().putBoolean("on", v).apply(); }
    public boolean hideOutside() { return sp.getBoolean("hideOut", false); }
    public void setHideOutside(boolean v) { sp.edit().putBoolean("hideOut", v).apply(); fire(false); }
    public int bubbleX() { return sp.getInt("bx", -1); }
    public int bubbleY() { return sp.getInt("by", -1); }
    public void setBubblePos(int x, int y) { sp.edit().putInt("bx", x).putInt("by", y).apply(); }

    // ---------- counting ----------
    public int count() { return count(today()); }
    public int count(String day) { return sp.getInt("c_" + day, 0); }
    public int seconds() { return seconds(today()); }
    public int seconds(String day) { return sp.getInt("s_" + day, 0); }
    public boolean hasData(String day) { return sp.getBoolean("a_" + day, false) || count(day) > 0; }
    public void markActive() { sp.edit().putBoolean("a_" + today(), true).apply(); }

    /** timed = measure the real gap since the previous reel (falls back to 25s). */
    public synchronized int addReel(boolean timed) {
        settle();
        String d = today();
        long now = System.currentTimeMillis();
        long last = sp.getLong("lastTs", 0);
        int sec = SEC_PER_REEL;
        if (timed && last > 0) {
            long gap = (now - last) / 1000;
            if (gap >= 3 && gap <= 120) sec = (int) gap;
        }
        int c = count(d) + 1;
        sp.edit().putInt("c_" + d, c).putInt("s_" + d, seconds(d) + sec).putBoolean("a_" + d, true)
                .putInt("lim_" + d, limit()).putInt("total", sp.getInt("total", 0) + 1)
                .putLong("lastTs", now).apply();
        checkBadges();
        fire(true);
        return c;
    }

    /** In-app fix: +1 or -1. */
    public synchronized void adjust(int delta) {
        String d = today();
        int c = Math.max(0, count(d) + delta);
        int s = Math.max(0, seconds(d) + delta * SEC_PER_REEL);
        sp.edit().putInt("c_" + d, c).putInt("s_" + d, s).putBoolean("a_" + d, true)
                .putInt("lim_" + d, limit()).apply();
        fire(delta > 0);
    }

    public synchronized void resetToday() {
        String d = today();
        sp.edit().remove("c_" + d).remove("s_" + d).remove("na_" + d).remove("sn_" + d).remove("w_" + d).apply();
        fire(false);
    }

    // ---------- alerts ----------
    public int nextAlert() { return sp.getInt("na_" + today(), limit()); }
    public int snoozes() { return sp.getInt("sn_" + today(), 0); }
    public boolean warned() { return sp.getBoolean("w_" + today(), false); }
    public void setWarned() { sp.edit().putBoolean("w_" + today(), true).apply(); }

    public synchronized void snooze(int more) {
        sp.edit().putInt("na_" + today(), count() + more).putInt("sn_" + today(), snoozes() + 1).apply();
        fire(false);
    }

    /** The user obeyed the stop alert. */
    public synchronized int stopHonored() {
        addPoints(50);
        sp.edit().putInt("stops", sp.getInt("stops", 0) + 1).putInt("na_" + today(), count() + 10).apply();
        checkBadges();
        fire(false);
        return 50;
    }

    // ---------- rewards ----------
    public int points() { return sp.getInt("points", 0); }
    public int streak() { return sp.getInt("streak", 0); }
    public int bestStreak() { return sp.getInt("best", 0); }
    public int stops() { return sp.getInt("stops", 0); }
    private void addPoints(int p) { sp.edit().putInt("points", points() + p).apply(); }

    /** Self-reported "I did something else instead" task. Max 3 rewarded per day. */
    public synchronized boolean doTask() {
        String k = "t_" + today();
        int n = sp.getInt(k, 0);
        if (n >= 3) return false;
        sp.edit().putInt(k, n + 1).putInt("tasks", sp.getInt("tasks", 0) + 1).apply();
        addPoints(15);
        checkBadges();
        fire(false);
        return true;
    }

    public int tasksToday() { return sp.getInt("t_" + today(), 0); }

    /** Looks at yesterday once and gives points if you stayed under the limit. */
    public synchronized void settle() {
        String y = dayKey(-1);
        if (y.equals(sp.getString("settled", ""))) return;
        sp.edit().putString("settled", y).apply();
        if (!sp.getBoolean("a_" + y, false)) return;
        int c = count(y);
        int lim = sp.getInt("lim_" + y, limit());
        if (c <= lim) {
            boolean light = c * 2 <= lim;
            int pts = 20 + (light ? 30 : 0);
            int st = streak() + 1;
            sp.edit().putInt("streak", st).putInt("best", Math.max(st, bestStreak()))
                    .putInt("light", sp.getInt("light", 0) + (light ? 1 : 0)).apply();
            addPoints(pts);
            addNews("Yesterday you stayed under your limit: +" + pts + " points! 🎉");
        } else {
            sp.edit().putInt("streak", 0).apply();
            addNews("Yesterday went over the limit. Fresh start today! 💪");
        }
        checkBadges();
        fire(false);
    }

    public Set<String> badges() {
        Set<String> s = new LinkedHashSet<>();
        String raw = sp.getString("badges", "");
        if (!raw.isEmpty()) for (String b : raw.split(",")) s.add(b);
        return s;
    }

    private void unlock(String id, String label) {
        Set<String> b = badges();
        if (b.add(id)) {
            sp.edit().putString("badges", android.text.TextUtils.join(",", b)).apply();
            addNews("Badge unlocked: " + label + " 🏅");
        }
    }

    private void checkBadges() {
        if (sp.getInt("total", 0) >= 1) unlock("first", "First Munch");
        if (stops() >= 1) unlock("stop", "Stop Hero");
        if (bestStreak() >= 3) unlock("s3", "3-Day Calm");
        if (bestStreak() >= 7) unlock("s7", "Week Warrior");
        if (sp.getInt("light", 0) >= 1) unlock("light", "Light Snacker");
        if (sp.getInt("tasks", 0) >= 5) unlock("bee", "Busy Bee");
        if (points() >= 500) unlock("master", "Monster Master");
    }

    // ---------- news banner ----------
    public String news() {
        String n = sp.getString("news", "");
        return n.isEmpty() ? null : n;
    }

    private void addNews(String s) {
        String n = sp.getString("news", "");
        sp.edit().putString("news", n.isEmpty() ? s : n + "\n" + s).apply();
    }

    public void clearNews() { sp.edit().remove("news").apply(); }
}
