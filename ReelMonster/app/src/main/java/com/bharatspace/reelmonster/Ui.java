package com.bharatspace.reelmonster;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Small helpers for building the cute, colourful screens in code (no libraries needed). */
final class Ui {
    static final int BG = 0xFFFFF4FA, PINK = 0xFFE0559A, DEEP = 0xFFB0125F, INK = 0xFF2B2140, SOFT = 0xFF7A6B8A,
            YELLOW = 0xFFFFC93C, PURPLE = 0xFF7C4DFF, MINT = 0xFF2FBF92, RED = 0xFFE5484D, ORANGE = 0xFFF59E0B,
            PINKBG = 0xFFFCE4F1, LAV = 0xFFEDE7FF, CREAM = 0xFFFFF4CC, WHITE = 0xFFFFFFFF;

    static int dp(Context c, float v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    static GradientDrawable round(Context c, int color, float rDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(c, rDp));
        return g;
    }

    static GradientDrawable outline(Context c, int fill, int stroke, float strokeDp, float rDp) {
        GradientDrawable g = round(c, fill, rDp);
        g.setStroke(dp(c, strokeDp), stroke);
        return g;
    }

    static TextView text(Context c, String s, float sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    static LinearLayout col(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(c, 16), dp(c, 14), dp(c, 16), dp(c, 24));
        return l;
    }

    static LinearLayout card(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setBackground(outline(c, WHITE, 0xFFF3C6DE, 1.2f, 22));
        l.setPadding(dp(c, 16), dp(c, 14), dp(c, 16), dp(c, 14));
        return l;
    }

    static LinearLayout.LayoutParams lp(Context c, int topDp) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.topMargin = dp(c, topDp);
        return p;
    }

    static LinearLayout.LayoutParams lpH(Context c, int heightDp, int topDp) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(c, heightDp));
        p.topMargin = dp(c, topDp);
        return p;
    }

    static TextView button(Context c, String label, int bg, int fg, View.OnClickListener l) {
        TextView b = text(c, label, 16, fg, true);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(c, 18), dp(c, 13), dp(c, 18), dp(c, 13));
        b.setBackground(new RippleDrawable(ColorStateList.valueOf(0x33000000), round(c, bg, 26), round(c, WHITE, 26)));
        b.setClickable(true);
        b.setOnClickListener(l);
        return b;
    }

    static TextView chip(Context c, String s, int bg, int fg) {
        TextView t = text(c, s, 14, fg, true);
        t.setGravity(Gravity.CENTER);
        t.setBackground(round(c, bg, 16));
        t.setPadding(dp(c, 10), dp(c, 10), dp(c, 10), dp(c, 10));
        return t;
    }

    static View progress(Context c, float ratio, int color) {
        LinearLayout bar = new LinearLayout(c);
        bar.setBackground(round(c, LAV, 6));
        float r = Math.max(0.03f, Math.min(1f, ratio));
        View f = new View(c);
        f.setBackground(round(c, color, 6));
        bar.addView(f, new LinearLayout.LayoutParams(0, -1, r));
        bar.addView(new View(c), new LinearLayout.LayoutParams(0, -1, 1f - r));
        return bar;
    }

    static String dur(int sec) {
        int m = Math.round(sec / 60f);
        if (m < 1) return sec + "s";
        if (m < 60) return m + " min";
        return (m / 60) + "h " + (m % 60) + "m";
    }
}
