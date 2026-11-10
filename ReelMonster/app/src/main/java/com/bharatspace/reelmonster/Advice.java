package com.bharatspace.reelmonster;

import java.util.ArrayList;
import java.util.Random;

/** Safe screen-time suggestions, funny lines, tasks and monster looks. */
final class Advice {
    static final String STUDENT = "student", PRO = "professional", HOME = "homemaker";

    static final int[] RANK_AT = {0, 100, 250, 500, 1000};
    static final String[] RANK = {"Baby Monster 🥚", "Calm Cub 🐣", "Focus Fox 🦊", "Scroll Tamer 🦁", "Zen Master 🐉"};

    static String roleLabel(String r) {
        if (STUDENT.equals(r)) return "Student 🎒";
        if (PRO.equals(r)) return "Working professional 💼";
        return "Housewife / Househusband 🏡";
    }

    /** Suggested minutes per day for short-video scrolling. A friendly guideline, not medical advice. */
    static int safeMinutes(String role, int age) {
        int m;
        if (STUDENT.equals(role)) m = 45;
        else if (PRO.equals(role)) m = 40;
        else m = 50;
        if (age < 13) m = 20;
        else if (age < 16) m = 30;
        else if (age < 18) m = 40;
        else if (age >= 60) m += 10;
        return m;
    }

    static int reelsFor(int minutes) {
        int r = Math.round(minutes * 60f / Store.SEC_PER_REEL);
        return Math.max(10, Math.round(r / 5f) * 5);
    }

    static String why(String role, int age) {
        String who = STUDENT.equals(role) ? "student" : PRO.equals(role) ? "working person" : "home-maker";
        return "A friendly guideline for a " + age + "-year-old " + who
                + ": keep reels to short breaks so there is still room for "
                + (STUDENT.equals(role) ? "studying, " : PRO.equals(role) ? "focused work, " : "your chores and rest, ")
                + "sleep, movement and people you love. It is a gentle suggestion, not medical advice. You can change it anytime.";
    }

    static int rankIndex(int pts) {
        int idx = 0;
        for (int i = 0; i < RANK_AT.length; i++) if (pts >= RANK_AT[i]) idx = i;
        return idx;
    }

    static String[] tasks(String role, int age) {
        ArrayList<String> l = new ArrayList<>();
        l.add("💧 Drink a full glass of water");
        l.add("🧘 Take 10 slow, deep breaths");
        l.add("🤸 Stretch your neck and shoulders for 2 minutes");
        l.add("👀 Look at something far away for 20 seconds");
        l.add("🎵 Listen to one favourite song without your phone");
        l.add("📞 Call someone you love for 5 minutes");
        l.add("✏️ Write down 3 things you are thankful for");
        if (age < 13) {
            l.add("🎨 Draw something silly");
            l.add("⚽ Go and play outside for 15 minutes");
            l.add("📖 Read a short story");
            l.add("🧸 Help someone at home with a small chore");
        } else if (STUDENT.equals(role)) {
            l.add("📚 Revise one topic for 15 minutes");
            l.add("📝 Solve 3 practice questions");
            l.add("📖 Read 5 pages of a book");
            l.add("🗓️ Plan tomorrow's study timetable");
            l.add("✍️ Write 5 key points from today's class");
        } else if (PRO.equals(role)) {
            l.add("✅ Finish one small pending task");
            l.add("🗓️ Write your top 3 tasks for tomorrow");
            l.add("🚶 Take a 5-minute walk");
            l.add("📖 Read a page of a book or article");
            l.add("💬 Reply to one pending message");
        } else {
            l.add("🍵 Make a cup of tea and sit quietly");
            l.add("🍲 Plan tomorrow's meals");
            l.add("🌱 Water the plants");
            l.add("🧺 Tidy one small shelf or corner");
            l.add("📖 Try a new recipe or read a few pages");
        }
        return l.toArray(new String[0]);
    }

    static String pick(String role, int age, Random r) {
        String[] t = tasks(role, age);
        return t[r.nextInt(t.length)];
    }

    private static final String[][] LINES = {
            {"Nom nom, a tiny snack! 😋", "Yum! Your evening tastes great.", "Just a light snack, okay?", "Hehe, I'm awake! 👋"},
            {"Hehe, I'm getting chubby! 🍩", "Burp! Excuse me. 🙊", "Tasty reels, but remember to blink!"},
            {"Burp. Your homework says hi. 📚", "I'm getting so full... 😵", "Did you drink water today? 💧", "My tummy is round like a ball!"},
            {"Careful! I'm almost bursting! 🎈", "Last few reels, then let's stop? 🥺", "I can't eat another byte!"},
            {"I'm FULL! Please stop! 🛑", "Phone down, buddy! Go stretch! 🤸", "My belly hurts... let's do something else?"}
    };

    static String line(int stage, Random r) {
        String[] a = LINES[Math.max(0, Math.min(4, stage))];
        return a[r.nextInt(a.length)];
    }

    static int stage(float ratio) {
        if (ratio < 0.25f) return 0;
        if (ratio < 0.5f) return 1;
        if (ratio < 0.8f) return 2;
        if (ratio < 1f) return 3;
        return 4;
    }

    static int mood(float ratio) {
        if (ratio < 0.5f) return MonsterView.HAPPY;
        if (ratio < 0.8f) return MonsterView.MUNCH;
        if (ratio < 1f) return MonsterView.WORRY;
        return MonsterView.ALERT;
    }

    static int color(float r) {
        int pink = 0xFFE0559A, orange = 0xFFF08A24, red = 0xFFE5484D;
        if (r < 0.5f) return pink;
        if (r < 0.9f) return lerp(pink, orange, (r - 0.5f) / 0.4f);
        return lerp(orange, red, Math.min(1f, (r - 0.9f) / 0.2f));
    }

    static int lerp(int a, int b, float t) {
        int ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255;
        int br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255;
        int r = Math.round(ar + (br - ar) * t), g = Math.round(ag + (bg - ag) * t), bl = Math.round(ab + (bb - ab) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }

    /** The monster grows as the count rises. */
    static float bubbleDp(float ratio) {
        return 64f + Math.min(ratio, 1.3f) * 34f;
    }
}
