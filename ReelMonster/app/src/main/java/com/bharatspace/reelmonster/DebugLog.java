package com.bharatspace.reelmonster;

import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.Locale;

/** Tiny in-memory log so you can see what the auto counter is doing (Settings tab). */
final class DebugLog {
    private static final ArrayDeque<String> LINES = new ArrayDeque<>();

    static synchronized void add(String s) {
        String t = new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date());
        LINES.addFirst(t + "  " + s);
        while (LINES.size() > 40) LINES.removeLast();
    }

    static synchronized String dump() {
        if (LINES.isEmpty()) return "(nothing yet)";
        StringBuilder sb = new StringBuilder();
        for (String l : LINES) sb.append(l).append('\n');
        return sb.toString().trim();
    }
}
