package com.bharatspace.reelmonster;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

/** Simple bar chart for the last 7 days with a dashed line for your limit. */
public class BarChartView extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rf = new RectF();
    private int[] vals = new int[7];
    private String[] labels = new String[7];
    private int limit = 100;

    public BarChartView(Context c) {
        super(c);
    }

    public void set(int[] vals, String[] labels, int limit) {
        this.vals = vals;
        this.labels = labels;
        this.limit = limit;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas c) {
        float d = getResources().getDisplayMetrics().density;
        float w = getWidth(), h = getHeight();
        float top = 22 * d, bottom = h - 24 * d;
        int max = limit;
        for (int v : vals) max = Math.max(max, v);
        float scale = (bottom - top) / (max * 1.08f);
        int n = vals.length;
        float slot = w / n, bw = slot * 0.55f;

        for (int i = 0; i < n; i++) {
            float cx = slot * i + slot / 2f;
            float bh = Math.max(3 * d, vals[i] * scale);
            boolean today = i == n - 1;
            boolean over = vals[i] > limit;
            p.setStyle(Paint.Style.FILL);
            p.setColor(over ? 0xFFE5484D : today ? 0xFFB0125F : 0xFFE0559A);
            rf.set(cx - bw / 2, bottom - bh, cx + bw / 2, bottom);
            c.drawRoundRect(rf, 8 * d, 8 * d, p);

            p.setColor(0xFF2B2140);
            p.setTextAlign(Paint.Align.CENTER);
            p.setTextSize(11 * d);
            p.setFakeBoldText(true);
            c.drawText(String.valueOf(vals[i]), cx, bottom - bh - 5 * d, p);
            p.setFakeBoldText(false);
            p.setColor(today ? 0xFFB0125F : 0xFF7A6B8A);
            p.setTextSize(11 * d);
            c.drawText(labels[i], cx, h - 6 * d, p);
        }

        // limit line
        float ly = bottom - limit * scale;
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(1.5f * d);
        p.setColor(0xFF7C4DFF);
        p.setPathEffect(new DashPathEffect(new float[]{10 * d, 7 * d}, 0));
        c.drawLine(0, ly, w, ly, p);
        p.setPathEffect(null);
        p.setStyle(Paint.Style.FILL);
        p.setTextAlign(Paint.Align.LEFT);
        p.setTextSize(10 * d);
        p.setColor(0xFF7C4DFF);
        c.drawText("limit " + limit, 2 * d, ly - 4 * d, p);
    }
}
