package com.bharatspace.reelmonster;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

public class MainActivity extends Activity implements Store.Listener {
    private Store st;
    private FrameLayout content;
    private LinearLayout tabs;
    private int tab = 0;
    private boolean onboarding = false;
    private int obStep = 0, obAge = 0, obLimit = 100;
    private String obRole = Advice.STUDENT;
    private EditText ageInput;
    private ObjectAnimator bob;

    private int dp(float v) {
        return Ui.dp(this, v);
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        st = Store.get(this);
        getWindow().setStatusBarColor(Ui.BG);
        getWindow().setNavigationBarColor(Color.WHITE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.BG);
        content = new FrameLayout(this);
        root.addView(content, new LinearLayout.LayoutParams(-1, 0, 1f));
        tabs = new LinearLayout(this);
        tabs.setBackgroundColor(Color.WHITE);
        root.addView(tabs, new LinearLayout.LayoutParams(-1, -2));
        setContentView(root);

        onboarding = !st.onboarded();
    }

    @Override
    protected void onResume() {
        super.onResume();
        st.settle();
        st.addListener(this);
        if (!onboarding && st.monsterOn() && MonsterService.instance == null && Settings.canDrawOverlays(this)) {
            try {
                startForegroundService(new Intent(this, MonsterService.class));
            } catch (Exception ignored) {
            }
        }
        render();
        if (getIntent() != null && getIntent().getBooleanExtra("celebrate", false)) {
            getIntent().removeExtra("celebrate");
            showCelebrate();
        }
    }

    @Override
    protected void onPause() {
        st.removeListener(this);
        super.onPause();
    }

    @Override
    protected void onNewIntent(Intent i) {
        super.onNewIntent(i);
        setIntent(i);
    }

    @Override
    public void onStoreChanged(boolean countUp) {
        if (!onboarding && tab == 0) render();
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] res) {
        super.onRequestPermissionsResult(code, perms, res);
        render();
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_LONG).show();
    }

    // ---------- rendering ----------
    private void render() {
        if (bob != null) {
            bob.cancel();
            bob = null;
        }
        content.removeAllViews();
        if (onboarding) {
            tabs.setVisibility(View.GONE);
            content.addView(scroll(buildOnboarding()));
            return;
        }
        tabs.setVisibility(View.VISIBLE);
        buildTabs();
        LinearLayout page;
        switch (tab) {
            case 1:
                page = buildStats();
                break;
            case 2:
                page = buildRewards();
                break;
            case 3:
                page = buildSettings();
                break;
            default:
                page = buildHome();
        }
        content.addView(scroll(page));
    }

    private ScrollView scroll(LinearLayout inner) {
        ScrollView sv = new ScrollView(this);
        sv.setFillViewport(true);
        sv.addView(inner);
        return sv;
    }

    private void buildTabs() {
        tabs.removeAllViews();
        tabs.setPadding(dp(6), dp(6), dp(6), dp(6));
        String[] names = {"🏠\nHome", "📊\nStats", "🏆\nRewards", "⚙️\nSettings"};
        for (int i = 0; i < names.length; i++) {
            TextView t = Ui.text(this, names[i], 12, i == tab ? Ui.DEEP : Ui.SOFT, i == tab);
            t.setGravity(Gravity.CENTER);
            t.setPadding(0, dp(6), 0, dp(6));
            if (i == tab) t.setBackground(Ui.round(this, Ui.PINKBG, 16));
            final int idx = i;
            t.setOnClickListener(v -> {
                tab = idx;
                render();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1f);
            lp.setMargins(dp(3), 0, dp(3), 0);
            tabs.addView(t, lp);
        }
    }

    // ---------- onboarding ----------
    private LinearLayout buildOnboarding() {
        LinearLayout col = Ui.col(this);
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        col.setPadding(dp(22), dp(40), dp(22), dp(30));

        if (obStep == 0) {
            MonsterView mv = new MonsterView(this);
            mv.set(0, MonsterView.HAPPY, 170, 0xFFE0559A, false);
            col.addView(mv, new LinearLayout.LayoutParams(-2, -2));
            col.addView(centered("Hi! I'm Reely 👋", 30, Ui.DEEP, true), Ui.lp(this, 10));
            col.addView(centered("I'm a little monster who eats reels. Tell me a bit about you and I'll help you scroll less and feel better!", 16, Ui.INK, false), Ui.lp(this, 10));
            col.addView(Ui.button(this, "Let's start ✨", Ui.PINK, Color.WHITE, v -> {
                obStep = 1;
                render();
            }), Ui.lp(this, 28));
            return col;
        }

        if (obStep == 1) {
            col.addView(centered("Who are you? 🙋", 26, Ui.DEEP, true));
            col.addView(centered("This helps me suggest a healthy amount of reel time.", 15, Ui.SOFT, false), Ui.lp(this, 6));
            String[][] roles = {
                    {Advice.STUDENT, "🎒", "Student", "School, college or learning"},
                    {Advice.PRO, "💼", "Working professional", "Job, business or freelancing"},
                    {Advice.HOME, "🏡", "Housewife / Househusband", "Taking care of home and family"}};
            for (String[] r : roles) {
                LinearLayout card = Ui.card(this);
                card.setOrientation(LinearLayout.HORIZONTAL);
                card.setGravity(Gravity.CENTER_VERTICAL);
                card.addView(Ui.text(this, r[1], 34, Ui.INK, false));
                LinearLayout tx = new LinearLayout(this);
                tx.setOrientation(LinearLayout.VERTICAL);
                tx.setPadding(dp(14), 0, 0, 0);
                tx.addView(Ui.text(this, r[2], 18, Ui.DEEP, true));
                tx.addView(Ui.text(this, r[3], 13, Ui.SOFT, false));
                card.addView(tx);
                final String role = r[0];
                card.setOnClickListener(v -> {
                    obRole = role;
                    obStep = 2;
                    render();
                });
                col.addView(card, Ui.lp(this, 14));
            }
            return col;
        }

        if (obStep == 2) {
            col.addView(centered("How old are you? 🎂", 26, Ui.DEEP, true));
            ageInput = new EditText(this);
            ageInput.setInputType(InputType.TYPE_CLASS_NUMBER);
            ageInput.setTextSize(32);
            ageInput.setGravity(Gravity.CENTER);
            ageInput.setTextColor(Ui.INK);
            ageInput.setHint("18");
            ageInput.setBackground(Ui.outline(this, Ui.WHITE, Ui.PINK, 2f, 20));
            ageInput.setPadding(dp(12), dp(14), dp(12), dp(14));
            if (obAge > 0) ageInput.setText(String.valueOf(obAge));
            col.addView(ageInput, Ui.lp(this, 22));
            col.addView(Ui.button(this, "Next ➜", Ui.PINK, Color.WHITE, v -> {
                int a;
                try {
                    a = Integer.parseInt(ageInput.getText().toString().trim());
                } catch (Exception e) {
                    a = 0;
                }
                if (a < 5 || a > 99) {
                    toast("Please enter an age between 5 and 99");
                    return;
                }
                obAge = a;
                obLimit = Advice.reelsFor(Advice.safeMinutes(obRole, a));
                obStep = 3;
                render();
            }), Ui.lp(this, 22));
            col.addView(backLink(1));
            return col;
        }

        if (obStep == 3) {
            int min = Advice.safeMinutes(obRole, obAge);
            col.addView(centered("Your safe reel time ⏱️", 26, Ui.DEEP, true));
            LinearLayout card = Ui.card(this);
            card.setGravity(Gravity.CENTER_HORIZONTAL);
            card.addView(centered(min + " min / day", 34, Ui.PINK, true));
            card.addView(centered("about " + Advice.reelsFor(min) + " reels", 16, Ui.SOFT, false));
            card.addView(centered(Advice.why(obRole, obAge), 14, Ui.INK, false), Ui.lp(this, 12));
            col.addView(card, Ui.lp(this, 18));

            col.addView(centered("Your daily reel limit (I will alert you here)", 14, Ui.SOFT, true), Ui.lp(this, 18));
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.addView(stepBtn("−5", () -> {
                obLimit = Math.max(5, obLimit - 5);
                render();
            }), new LinearLayout.LayoutParams(0, -2, 1f));
            TextView val = centered(obLimit + " reels", 24, Ui.DEEP, true);
            row.addView(val, new LinearLayout.LayoutParams(0, -2, 1.6f));
            row.addView(stepBtn("+5", () -> {
                obLimit = Math.min(500, obLimit + 5);
                render();
            }), new LinearLayout.LayoutParams(0, -2, 1f));
            col.addView(row, Ui.lp(this, 8));

            col.addView(Ui.button(this, "Looks good ➜", Ui.PINK, Color.WHITE, v -> {
                obStep = 4;
                render();
            }), Ui.lp(this, 22));
            col.addView(backLink(2));
            return col;
        }

        col.addView(centered("How should I count? 🔢", 26, Ui.DEEP, true));
        col.addView(centered("You can change this anytime.", 15, Ui.SOFT, false), Ui.lp(this, 6));
        col.addView(modeCard("👆", "Manual", "Tap me after every reel. Simple and always accurate.", Store.MANUAL), Ui.lp(this, 16));
        col.addView(modeCard("🤖", "Automatic", "I count each new reel by myself. Needs the Accessibility permission.", Store.AUTO), Ui.lp(this, 12));
        col.addView(backLink(3));
        return col;
    }

    private View modeCard(String emoji, String title, String desc, final String mode) {
        LinearLayout card = Ui.card(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.addView(Ui.text(this, emoji, 34, Ui.INK, false));
        LinearLayout tx = new LinearLayout(this);
        tx.setOrientation(LinearLayout.VERTICAL);
        tx.setPadding(dp(14), 0, 0, 0);
        tx.addView(Ui.text(this, title, 18, Ui.DEEP, true));
        tx.addView(Ui.text(this, desc, 13, Ui.SOFT, false));
        card.addView(tx, new LinearLayout.LayoutParams(0, -2, 1f));
        card.setOnClickListener(v -> finishOnboarding(mode));
        return card;
    }

    private void finishOnboarding(String mode) {
        st.saveProfile(obRole, obAge, Advice.safeMinutes(obRole, obAge), obLimit);
        st.setMode(mode);
        st.setOnboarded(true);
        onboarding = false;
        tab = 0;
        render();
    }

    private TextView backLink(final int toStep) {
        TextView b = centered("← Back", 14, Ui.SOFT, true);
        b.setPadding(0, dp(18), 0, dp(10));
        b.setOnClickListener(v -> {
            obStep = toStep;
            render();
        });
        return b;
    }

    private TextView centered(String s, float sp, int color, boolean bold) {
        TextView t = Ui.text(this, s, sp, color, bold);
        t.setGravity(Gravity.CENTER);
        return t;
    }

    private TextView stepBtn(String label, final Runnable r) {
        TextView b = Ui.button(this, label, Ui.LAV, Ui.DEEP, v -> r.run());
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(dp(4), 0, dp(4), 0);
        b.setLayoutParams(p);
        return b;
    }

    private void showCelebrate() {
        String task = Advice.pick(st.role(), st.age(), new Random());
        new AlertDialog.Builder(this)
                .setTitle("🎉 Stop Hero!")
                .setMessage("You listened to your monster: +50 points!\n\nNow try this instead of scrolling:\n" + task)
                .setPositiveButton("Let's do it 💪", null)
                .show();
    }

    // ---------- HOME ----------
    private LinearLayout buildHome() {
        LinearLayout col = Ui.col(this);

        String news = st.news();
        if (news != null) {
            TextView n = Ui.text(this, news + "\n(tap to dismiss)", 14, Ui.INK, true);
            n.setBackground(Ui.round(this, Ui.CREAM, 16));
            n.setPadding(dp(14), dp(10), dp(14), dp(10));
            n.setOnClickListener(v -> {
                st.clearNews();
                render();
            });
            col.addView(n, Ui.lp(this, 0));
        }

        col.addView(Ui.text(this, "Reel Monster 👾", 28, Ui.DEEP, true), Ui.lp(this, 4));
        col.addView(Ui.text(this, "Your cute little scroll counter", 14, Ui.SOFT, false), Ui.lp(this, 0));

        final int c = st.count(), lim = Math.max(1, st.limit());
        final float ratio = (float) c / lim;
        final boolean manual = Store.MANUAL.equals(st.mode());

        LinearLayout hero = Ui.card(this);
        hero.setGravity(Gravity.CENTER_HORIZONTAL);
        final MonsterView mv = new MonsterView(this);
        mv.set(c, Advice.mood(ratio), 130 + Math.min(ratio, 1.2f) * 45, Advice.color(ratio), false);
        hero.addView(mv, new LinearLayout.LayoutParams(-2, -2));
        bob = ObjectAnimator.ofFloat(mv, "translationY", 0f, -dp(8));
        bob.setDuration(900);
        bob.setRepeatCount(ValueAnimator.INFINITE);
        bob.setRepeatMode(ValueAnimator.REVERSE);
        bob.start();
        if (manual) {
            mv.setOnClickListener(v -> st.addReel(false));
        }

        String line = manual ? "👆 Tap me +1 after every reel" : Advice.line(Advice.stage(ratio), new Random(Store.today().hashCode() + c / 5));
        TextView bubble = Ui.text(this, line, 14, Ui.INK, true);
        bubble.setGravity(Gravity.CENTER);
        bubble.setBackground(Ui.outline(this, Ui.WHITE, Ui.PINK, 1.5f, 16));
        bubble.setPadding(dp(14), dp(8), dp(14), dp(8));
        hero.addView(bubble, Ui.lp(this, 6));

        hero.addView(centered(String.valueOf(c), 60, Ui.INK, true), Ui.lp(this, 4));
        hero.addView(centered("reels today", 15, Ui.SOFT, false));

        LinearLayout row = new LinearLayout(this);
        LinearLayout.LayoutParams lw = new LinearLayout.LayoutParams(0, -2, 1f);
        lw.setMargins(dp(4), 0, dp(4), 0);
        row.addView(Ui.chip(this, "⏱ " + Ui.dur(st.seconds()) + " lost", Ui.PINKBG, Ui.DEEP), lw);
        row.addView(Ui.chip(this, "🎯 limit " + lim, Ui.LAV, Ui.PURPLE), lw);
        hero.addView(row, Ui.lp(this, 12));

        hero.addView(Ui.progress(this, ratio, Advice.color(ratio)), Ui.lpH(this, 12, 12));
        hero.addView(centered(Math.round(ratio * 100) + "% of today's limit", 13, Ui.SOFT, false), Ui.lp(this, 4));
        col.addView(hero, Ui.lp(this, 12));

        View setup = setupCard();
        if (setup != null) col.addView(setup, Ui.lp(this, 12));

        col.addView(segment(), Ui.lp(this, 14));
        col.addView(centered(manual ? "Tap the monster after each reel." : "I count each new reel by myself 🤖", 13, Ui.SOFT, false), Ui.lp(this, 6));

        final boolean running = MonsterService.instance != null && st.monsterOn();
        col.addView(Ui.button(this, running ? "😴  Put the monster to sleep" : "👾  Wake the monster",
                running ? Ui.PURPLE : Ui.PINK, Color.WHITE, v -> {
                    if (running) stopMonster();
                    else startMonster();
                }), Ui.lp(this, 12));

        LinearLayout fix = new LinearLayout(this);
        fix.setGravity(Gravity.CENTER_VERTICAL);
        TextView fl = Ui.text(this, "Counted wrong? Fix it:", 13, Ui.SOFT, false);
        fix.addView(fl, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView minus = Ui.button(this, "−1", Ui.LAV, Ui.DEEP, v -> st.adjust(-1));
        TextView plus = Ui.button(this, "+1", Ui.LAV, Ui.DEEP, v -> st.adjust(1));
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(dp(74), -2);
        bp.setMargins(dp(6), 0, 0, 0);
        fix.addView(minus, bp);
        fix.addView(plus, bp);
        col.addView(fix, Ui.lp(this, 12));

        LinearLayout tip = Ui.card(this);
        tip.setBackground(Ui.outline(this, Ui.CREAM, 0xFFFFD98A, 1.5f, 22));
        tip.addView(Ui.text(this, "Instead of scrolling, try:", 13, Ui.SOFT, true));
        tip.addView(Ui.text(this, Advice.pick(st.role(), st.age(), new Random(Store.today().hashCode() + c / 10)), 17, Ui.INK, true), Ui.lp(this, 4));
        col.addView(tip, Ui.lp(this, 14));
        return col;
    }

    private View segment() {
        LinearLayout seg = new LinearLayout(this);
        seg.setBackground(Ui.round(this, Ui.LAV, 22));
        seg.setPadding(dp(4), dp(4), dp(4), dp(4));
        String[][] opts = {{Store.MANUAL, "👆 Manual"}, {Store.AUTO, "🤖 Auto"}};
        for (String[] o : opts) {
            boolean sel = o[0].equals(st.mode());
            TextView t = Ui.text(this, o[1], 15, sel ? Color.WHITE : Ui.DEEP, true);
            t.setGravity(Gravity.CENTER);
            t.setPadding(0, dp(10), 0, dp(10));
            if (sel) t.setBackground(Ui.round(this, Ui.PINK, 18));
            final String m = o[0];
            t.setOnClickListener(v -> st.setMode(m));
            seg.addView(t, new LinearLayout.LayoutParams(0, -2, 1f));
        }
        return seg;
    }

    private View setupCard() {
        boolean auto = Store.AUTO.equals(st.mode());
        boolean ov = Perms.overlay(this), nt = Perms.notifications(this), ac = Perms.accessibility(this);
        if (ov && nt && (!auto || ac)) return null;
        LinearLayout card = Ui.card(this);
        card.setBackground(Ui.outline(this, Ui.CREAM, 0xFFFFD98A, 1.5f, 22));
        card.addView(Ui.text(this, "✨ Quick setup", 18, Ui.INK, true));
        if (!ov) card.addView(permRow("Display over other apps", "Lets the monster float on top of Instagram and YouTube.", v -> openOverlay()), Ui.lp(this, 10));
        if (!nt) card.addView(permRow("Notifications", "So I can send you the stop alert.", v -> requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1)), Ui.lp(this, 10));
        if (auto && !ac) {
            card.addView(permRow("Accessibility (auto count)", "Turn on \"Reel Monster auto counter\". It only reads Instagram and YouTube screens to count reels.", v -> {
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
                toast("Find Reel Monster auto counter and switch it ON");
            }), Ui.lp(this, 10));
            card.addView(Ui.text(this, "Switch greyed out? Open Settings > Apps > Reel Monster > ⋮ (top right) > Allow restricted settings, then try again.", 12, Ui.SOFT, false), Ui.lp(this, 6));
        }
        return card;
    }

    private View permRow(String title, String desc, View.OnClickListener l) {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.VERTICAL);
        r.addView(Ui.text(this, "❌  " + title, 15, Ui.INK, true));
        r.addView(Ui.text(this, desc, 13, Ui.SOFT, false));
        TextView b = Ui.button(this, "Allow", Ui.PINK, Color.WHITE, l);
        b.setTextSize(14);
        r.addView(b, Ui.lp(this, 6));
        return r;
    }

    private void openOverlay() {
        startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())));
    }

    private void startMonster() {
        if (!Settings.canDrawOverlays(this)) {
            toast("Please allow \"Display over other apps\" first");
            openOverlay();
            return;
        }
        st.setMonsterOn(true);
        startForegroundService(new Intent(this, MonsterService.class));
        toast("Reely is awake! Open Instagram or YouTube 👾");
        render();
    }

    private void stopMonster() {
        st.setMonsterOn(false);
        stopService(new Intent(this, MonsterService.class));
        render();
    }

    // ---------- STATS ----------
    private String dayLabel(int offset, String pattern) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_YEAR, offset);
        return new SimpleDateFormat(pattern, Locale.getDefault()).format(c.getTime());
    }

    private LinearLayout buildStats() {
        LinearLayout col = Ui.col(this);
        col.addView(Ui.text(this, "Your week 📊", 28, Ui.DEEP, true));

        int[] vals = new int[7];
        String[] labs = new String[7];
        int totalC = 0, totalS = 0, best = Integer.MAX_VALUE, days = 0;
        for (int i = 6; i >= 0; i--) {
            String d = Store.dayKey(-i);
            int idx = 6 - i;
            vals[idx] = st.count(d);
            totalC += vals[idx];
            totalS += st.seconds(d);
            labs[idx] = dayLabel(-i, "EEE");
            if (st.hasData(d)) {
                days++;
                best = Math.min(best, vals[idx]);
            }
        }

        LinearLayout today = Ui.card(this);
        today.setGravity(Gravity.CENTER_HORIZONTAL);
        today.addView(centered("Today", 14, Ui.SOFT, true));
        today.addView(centered(String.valueOf(st.count()), 54, Ui.INK, true));
        today.addView(centered("reels  ·  " + Ui.dur(st.seconds()) + " lost", 16, Ui.DEEP, true));
        col.addView(today, Ui.lp(this, 12));

        LinearLayout chart = Ui.card(this);
        chart.addView(Ui.text(this, "Last 7 days", 16, Ui.INK, true));
        BarChartView bc = new BarChartView(this);
        bc.set(vals, labs, st.limit());
        chart.addView(bc, Ui.lpH(this, 200, 8));
        col.addView(chart, Ui.lp(this, 12));

        LinearLayout sum = Ui.card(this);
        sum.addView(Ui.text(this, "This week", 16, Ui.INK, true));
        sum.addView(statLine("Total reels", String.valueOf(totalC)), Ui.lp(this, 8));
        sum.addView(statLine("Average per day", days == 0 ? "-" : String.valueOf(Math.round(totalC / (float) days))), Ui.lp(this, 4));
        sum.addView(statLine("Time lost", Ui.dur(totalS)), Ui.lp(this, 4));
        sum.addView(statLine("Calmest day", best == Integer.MAX_VALUE ? "-" : best + " reels"), Ui.lp(this, 4));
        col.addView(sum, Ui.lp(this, 12));

        LinearLayout list = Ui.card(this);
        list.addView(Ui.text(this, "Day by day", 16, Ui.INK, true));
        for (int i = 0; i < 7; i++) {
            String d = Store.dayKey(-i);
            list.addView(statLine(i == 0 ? "Today" : dayLabel(-i, "EEE, d MMM"), st.count(d) + " reels · " + Ui.dur(st.seconds(d))), Ui.lp(this, i == 0 ? 8 : 4));
        }
        col.addView(list, Ui.lp(this, 12));
        return col;
    }

    private View statLine(String a, String b) {
        LinearLayout r = new LinearLayout(this);
        r.addView(Ui.text(this, a, 14, Ui.SOFT, false), new LinearLayout.LayoutParams(0, -2, 1f));
        r.addView(Ui.text(this, b, 14, Ui.INK, true));
        return r;
    }

    // ---------- REWARDS ----------
    private LinearLayout buildRewards() {
        LinearLayout col = Ui.col(this);
        col.addView(Ui.text(this, "Rewards 🏆", 28, Ui.DEEP, true));

        int pts = st.points();
        int ri = Advice.rankIndex(pts);
        LinearLayout top = Ui.card(this);
        top.setGravity(Gravity.CENTER_HORIZONTAL);
        top.addView(centered("⭐ " + pts, 44, Ui.INK, true));
        top.addView(centered(Advice.RANK[ri], 18, Ui.DEEP, true));
        if (ri < Advice.RANK_AT.length - 1) {
            int a = Advice.RANK_AT[ri], b = Advice.RANK_AT[ri + 1];
            top.addView(Ui.progress(this, (pts - a) / (float) (b - a), Ui.PINK), Ui.lpH(this, 10, 10));
            top.addView(centered((b - pts) + " points to " + Advice.RANK[ri + 1], 13, Ui.SOFT, false), Ui.lp(this, 4));
        } else {
            top.addView(centered("Top rank reached! 🎉", 13, Ui.SOFT, false), Ui.lp(this, 4));
        }
        top.addView(centered("🔥 " + st.streak() + "-day streak  (best " + st.bestStreak() + ")", 16, Ui.PURPLE, true), Ui.lp(this, 10));
        col.addView(top, Ui.lp(this, 12));

        col.addView(Ui.text(this, "Badges", 18, Ui.INK, true), Ui.lp(this, 16));
        Set<String> have = st.badges();
        LinearLayout rowL = null;
        for (int i = 0; i < Store.BADGES.length; i++) {
            if (i % 2 == 0) {
                rowL = new LinearLayout(this);
                col.addView(rowL, Ui.lp(this, 8));
            }
            String[] b = Store.BADGES[i];
            boolean ok = have.contains(b[0]);
            LinearLayout bc = Ui.card(this);
            bc.setGravity(Gravity.CENTER_HORIZONTAL);
            bc.setAlpha(ok ? 1f : 0.45f);
            bc.addView(centered(ok ? b[1] : "🔒", 30, Ui.INK, false));
            bc.addView(centered(b[2], 14, Ui.DEEP, true));
            bc.addView(centered(b[3], 11, Ui.SOFT, false));
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -1, 1f);
            p.setMargins(dp(4), 0, dp(4), 0);
            rowL.addView(bc, p);
        }
        if (Store.BADGES.length % 2 == 1 && rowL != null) rowL.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1f));

        LinearLayout how = Ui.card(this);
        how.addView(Ui.text(this, "How to earn points", 18, Ui.INK, true));
        how.addView(Ui.text(this, "🌙  End a day under your limit: +20\n🥗  End a day at half your limit or less: +30 extra\n🛑  Obey the stop alert: +50\n✅  Do something else instead: +15 (3 times a day)", 14, Ui.INK, false), Ui.lp(this, 8));
        col.addView(how, Ui.lp(this, 16));

        LinearLayout tasks = Ui.card(this);
        tasks.addView(Ui.text(this, "Do something else instead 🌈", 18, Ui.INK, true));
        tasks.addView(Ui.text(this, "Done today: " + st.tasksToday() + "/3 rewarded", 12, Ui.SOFT, false));
        String[] all = Advice.tasks(st.role(), st.age());
        Random r = new Random(Store.today().hashCode());
        for (int i = 0; i < 5; i++) {
            String t = all[r.nextInt(all.length)];
            LinearLayout tr = new LinearLayout(this);
            tr.setGravity(Gravity.CENTER_VERTICAL);
            tr.addView(Ui.text(this, t, 14, Ui.INK, false), new LinearLayout.LayoutParams(0, -2, 1f));
            TextView done = Ui.button(this, "Done +15", Ui.MINT, Color.WHITE, v -> {
                boolean ok = st.doTask();
                toast(ok ? "+15 points! Great job 🎉" : "Daily bonus is used up, but well done anyway 💖");
                render();
            });
            done.setTextSize(13);
            done.setPadding(dp(12), dp(8), dp(12), dp(8));
            tr.addView(done, new LinearLayout.LayoutParams(-2, -2));
            tasks.addView(tr, Ui.lp(this, 10));
        }
        col.addView(tasks, Ui.lp(this, 16));
        return col;
    }

    // ---------- SETTINGS ----------
    private LinearLayout buildSettings() {
        LinearLayout col = Ui.col(this);
        col.addView(Ui.text(this, "Settings ⚙️", 28, Ui.DEEP, true));

        LinearLayout lim = Ui.card(this);
        lim.addView(Ui.text(this, "Daily reel limit", 18, Ui.INK, true));
        lim.addView(Ui.text(this, "About " + Ui.dur(st.limit() * Store.SEC_PER_REEL) + " of reels. Your safe suggestion: " + st.safeMinutes() + " min.", 13, Ui.SOFT, false));
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(stepBtn("−5", () -> {
            st.setLimit(st.limit() - 5);
            render();
        }), new LinearLayout.LayoutParams(0, -2, 1f));
        row.addView(centered(st.limit() + "", 26, Ui.DEEP, true), new LinearLayout.LayoutParams(0, -2, 1.4f));
        row.addView(stepBtn("+5", () -> {
            st.setLimit(st.limit() + 5);
            render();
        }), new LinearLayout.LayoutParams(0, -2, 1f));
        lim.addView(row, Ui.lp(this, 10));
        col.addView(lim, Ui.lp(this, 12));

        LinearLayout prof = Ui.card(this);
        prof.addView(Ui.text(this, "About you", 18, Ui.INK, true));
        prof.addView(Ui.text(this, Advice.roleLabel(st.role()) + " · age " + st.age(), 14, Ui.INK, false), Ui.lp(this, 4));
        prof.addView(Ui.button(this, "Change my profile", Ui.LAV, Ui.DEEP, v -> {
            obRole = st.role();
            obAge = st.age();
            obStep = 1;
            onboarding = true;
            render();
        }), Ui.lp(this, 10));
        col.addView(prof, Ui.lp(this, 12));

        LinearLayout opt = Ui.card(this);
        opt.addView(Ui.text(this, "Monster bubble", 18, Ui.INK, true));
        LinearLayout sr = new LinearLayout(this);
        sr.setGravity(Gravity.CENTER_VERTICAL);
        sr.addView(Ui.text(this, "Hide it outside Instagram and YouTube (needs auto-count permission)", 13, Ui.INK, false), new LinearLayout.LayoutParams(0, -2, 1f));
        Switch sw = new Switch(this);
        sw.setChecked(st.hideOutside());
        sw.setOnCheckedChangeListener((b, v) -> st.setHideOutside(v));
        sr.addView(sw);
        opt.addView(sr, Ui.lp(this, 8));
        opt.addView(Ui.text(this, "Tip: drag the monster anywhere. Long-press it to open this app.", 12, Ui.SOFT, false), Ui.lp(this, 8));
        col.addView(opt, Ui.lp(this, 12));

        LinearLayout perm = Ui.card(this);
        perm.addView(Ui.text(this, "Permissions", 18, Ui.INK, true));
        perm.addView(Ui.text(this, (Perms.overlay(this) ? "✅" : "❌") + " Display over other apps\n"
                + (Perms.notifications(this) ? "✅" : "❌") + " Notifications\n"
                + (Perms.accessibility(this) ? "✅" : "❌") + " Accessibility (auto count)", 14, Ui.INK, false), Ui.lp(this, 6));
        perm.addView(Ui.button(this, "Overlay settings", Ui.LAV, Ui.DEEP, v -> openOverlay()), Ui.lp(this, 10));
        perm.addView(Ui.button(this, "Accessibility settings", Ui.LAV, Ui.DEEP, v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))), Ui.lp(this, 8));
        col.addView(perm, Ui.lp(this, 12));

        LinearLayout dbg = Ui.card(this);
        dbg.addView(Ui.text(this, "Auto-count log 🔍", 18, Ui.INK, true));
        dbg.addView(Ui.text(this, "Shows what the auto counter saw. Handy for checking accuracy.", 12, Ui.SOFT, false));
        TextView log = Ui.text(this, DebugLog.dump(), 11, Ui.INK, false);
        log.setTypeface(Typeface.MONOSPACE);
        dbg.addView(log, Ui.lp(this, 8));
        dbg.addView(Ui.button(this, "Refresh log", Ui.LAV, Ui.DEEP, v -> render()), Ui.lp(this, 10));
        col.addView(dbg, Ui.lp(this, 12));

        col.addView(Ui.button(this, "Reset today's count", Ui.WHITE, Ui.RED, v ->
                new AlertDialog.Builder(this).setTitle("Reset today's count?")
                        .setMessage("Today's count and time lost will go back to 0.")
                        .setPositiveButton("Reset", (d, w) -> st.resetToday())
                        .setNegativeButton("Cancel", null).show()), Ui.lp(this, 14));

        col.addView(centered("🔒 No internet permission, no login. Everything stays on your phone.", 12, Ui.SOFT, false), Ui.lp(this, 16));
        return col;
    }
}
