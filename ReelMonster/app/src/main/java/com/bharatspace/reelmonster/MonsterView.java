package com.bharatspace.reelmonster;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

/** The cute monster. Used for the floating bubble and the big one in the app. */
public class MonsterView extends View {
    public static final int HAPPY = 0, MUNCH = 1, WORRY = 2, ALERT = 3;

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rf = new RectF();
    private final Path path = new Path();
    private int count, mood, color = 0xFFE0559A;
    private float sizeDp = 72;
    private boolean badge = true;

    public MonsterView(Context c) {
        super(c);
    }

    public void set(int count, int mood, float sizeDp, int color, boolean badge) {
        this.count = count;
        this.mood = mood;
        this.sizeDp = sizeDp;
        this.color = color;
        this.badge = badge;
        requestLayout();
        invalidate();
    }

    @Override
    protected void onMeasure(int w, int h) {
        int s = Math.round(sizeDp * getResources().getDisplayMetrics().density);
        setMeasuredDimension(s, s);
    }

    private static int shade(int c, float f) {
        int r = Math.round(((c >> 16) & 255) * f), g = Math.round(((c >> 8) & 255) * f), b = Math.round((c & 255) * f);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    @Override
    protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight(), m = Math.min(w, h);
        float cx = w / 2f, cy = h / 2f + m * 0.02f, R = m * 0.40f;
        final int ink = 0xFF3B1030;

        // soft shadow
        p.setStyle(Paint.Style.FILL);
        p.setColor(0x22000000);
        c.drawCircle(cx, cy + R * 0.08f, R * 1.02f, p);
        // horns
        p.setColor(shade(color, 0.82f));
        c.drawCircle(cx - R * 0.55f, cy - R * 0.88f, R * 0.2f, p);
        c.drawCircle(cx + R * 0.55f, cy - R * 0.88f, R * 0.2f, p);
        // body
        p.setColor(color);
        c.drawCircle(cx, cy, R, p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(R * 0.09f);
        p.setColor(Color.WHITE);
        c.drawCircle(cx, cy, R, p);

        // cheeks
        p.setStyle(Paint.Style.FILL);
        p.setColor(0x66FFFFFF);
        c.drawCircle(cx - R * 0.62f, cy + R * 0.3f, R * 0.15f, p);
        c.drawCircle(cx + R * 0.62f, cy + R * 0.3f, R * 0.15f, p);

        // eyes
        float eyeR = R * 0.24f, ex = R * 0.36f, ey = cy - R * 0.12f;
        p.setColor(Color.WHITE);
        c.drawCircle(cx - ex, ey, eyeR, p);
        c.drawCircle(cx + ex, ey, eyeR, p);
        float dy = mood == ALERT ? eyeR * 0.2f : 0f;
        p.setColor(0xFF2B2140);
        c.drawCircle(cx - ex, ey + dy, eyeR * 0.5f, p);
        c.drawCircle(cx + ex, ey + dy, eyeR * 0.5f, p);
        p.setColor(Color.WHITE);
        c.drawCircle(cx - ex + eyeR * 0.18f, ey + dy - eyeR * 0.18f, eyeR * 0.16f, p);
        c.drawCircle(cx + ex + eyeR * 0.18f, ey + dy - eyeR * 0.18f, eyeR * 0.16f, p);

        // brows and mouth
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeWidth(R * 0.07f);
        p.setColor(ink);
        if (mood == HAPPY) {
            rf.set(cx - R * 0.28f, cy + R * 0.12f, cx + R * 0.28f, cy + R * 0.52f);
            c.drawArc(rf, 15, 150, false, p);
        } else if (mood == MUNCH) {
            p.setStyle(Paint.Style.FILL);
            rf.set(cx - R * 0.2f, cy + R * 0.25f, cx + R * 0.2f, cy + R * 0.62f);
            c.drawOval(rf, p);
            p.setColor(0xFFFF7BA9);
            rf.set(cx - R * 0.12f, cy + R * 0.46f, cx + R * 0.12f, cy + R * 0.62f);
            c.drawOval(rf, p);
        } else if (mood == WORRY) {
            c.drawLine(cx - ex - eyeR, ey - eyeR * 1.15f, cx - ex + eyeR, ey - eyeR * 1.65f, p);
            c.drawLine(cx + ex + eyeR, ey - eyeR * 1.15f, cx + ex - eyeR, ey - eyeR * 1.65f, p);
            path.reset();
            path.moveTo(cx - R * 0.28f, cy + R * 0.45f);
            path.quadTo(cx - R * 0.14f, cy + R * 0.33f, cx, cy + R * 0.45f);
            path.quadTo(cx + R * 0.14f, cy + R * 0.57f, cx + R * 0.28f, cy + R * 0.45f);
            c.drawPath(path, p);
        } else {
            c.drawLine(cx - ex - eyeR, ey - eyeR * 1.65f, cx - ex + eyeR, ey - eyeR * 1.15f, p);
            c.drawLine(cx + ex + eyeR, ey - eyeR * 1.65f, cx + ex - eyeR, ey - eyeR * 1.15f, p);
            rf.set(cx - R * 0.28f, cy + R * 0.4f, cx + R * 0.28f, cy + R * 0.8f);
            c.drawArc(rf, 200, 140, false, p);
            p.setStyle(Paint.Style.FILL);
            p.setColor(0xFF7FD3FF);
            c.drawCircle(cx - ex - eyeR * 0.2f, ey + eyeR * 1.55f, R * 0.06f, p);
        }

        // yellow count badge
        if (badge) {
            float br = R * 0.36f, bx = cx + R * 0.72f, by = cy + R * 0.78f;
            p.setStyle(Paint.Style.FILL);
            p.setColor(0xFFFFC93C);
            c.drawCircle(bx, by, br, p);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(R * 0.05f);
            p.setColor(Color.WHITE);
            c.drawCircle(bx, by, br, p);
            p.setStyle(Paint.Style.FILL);
            p.setColor(ink);
            p.setTextAlign(Paint.Align.CENTER);
            p.setFakeBoldText(true);
            String s = String.valueOf(count);
            p.setTextSize(br * (s.length() <= 2 ? 1.1f : s.length() == 3 ? 0.85f : 0.68f));
            c.drawText(s, bx, by - (p.descent() + p.ascent()) / 2f, p);
            p.setFakeBoldText(false);
            p.setTextAlign(Paint.Align.LEFT);
        }
    }
}
