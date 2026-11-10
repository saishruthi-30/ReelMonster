package com.bharatspace.reelmonster;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.PixelFormat;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Random;

/** Draws the floating monster bubble on top of other apps and raises the stop alert. */
public class MonsterService extends Service implements Store.Listener {
    static volatile MonsterService instance;
    static final String ACTION_STOP = "com.bharatspace.reelmonster.STOP";

    private static final int NID = 1, NID_ALERT = 2;
    private static final String CH_RUN = "run", CH_ALERT = "alert";

    private final Handler h = new Handler(Looper.getMainLooper());
    private final Random rnd = new Random();
    private WindowManager wm;
    private Store st;
    private LinearLayout bubble;
    private MonsterView mv;
    private TextView speech;
    private WindowManager.LayoutParams lp;
    private FrameLayout alertView;
    private boolean inTarget = true;
    private int lastStage = -1;

    private final Runnable hideSpeech = new Runnable() {
        @Override
        public void run() {
            if (speech != null && alertView == null) speech.setVisibility(View.GONE);
        }
    };

    @Override
    public IBinder onBind(Intent i) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        st = Store.get(this);
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        createChannels();
        startFg();
        if (!Settings.canDrawOverlays(this)) {
            st.setMonsterOn(false);
            stopForeground(true);
            stopSelf();
            return;
        }
        st.markActive();
        buildBubble();
        st.addListener(this);
        refreshBubble();
        say(st.mode().equals(Store.AUTO) ? "Auto mode: I count for you! 🤖" : "Tap me after every reel! 👆", false);
    }

    @Override
    public int onStartCommand(Intent i, int flags, int startId) {
        if (i != null && ACTION_STOP.equals(i.getAction())) {
            st.setMonsterOn(false);
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        h.removeCallbacksAndMessages(null);
        if (st != null) st.removeListener(this);
        dismissAlert();
        if (bubble != null) {
            try {
                wm.removeView(bubble);
            } catch (Exception ignored) {
            }
            bubble = null;
        }
        instance = null;
        super.onDestroy();
    }

    // ---------- notification ----------
    private void createChannels() {
        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel(CH_RUN, "Monster is awake", NotificationManager.IMPORTANCE_LOW));
        NotificationChannel a = new NotificationChannel(CH_ALERT, "Reel limit alerts", NotificationManager.IMPORTANCE_HIGH);
        nm.createNotificationChannel(a);
    }

    private PendingIntent openAppIntent() {
        Intent open = new Intent(this, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(this, 0, open, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private Notification buildNotif() {
        Intent stop = new Intent(this, MonsterService.class).setAction(ACTION_STOP);
        PendingIntent sp = PendingIntent.getService(this, 1, stop, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        return new Notification.Builder(this, CH_RUN)
                .setSmallIcon(R.drawable.ic_stat)
                .setContentTitle("Reely is watching 👀")
                .setContentText(st.count() + " reels today · limit " + st.limit())
                .setContentIntent(openAppIntent())
                .setOngoing(true)
                .addAction(new Notification.Action.Builder(Icon.createWithResource(this, R.drawable.ic_stat), "Sleep", sp).build())
                .build();
    }

    private void startFg() {
        Notification n = buildNotif();
        if (Build.VERSION.SDK_INT >= 34) startForeground(NID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        else startForeground(NID, n);
    }

    private void updateNotif() {
        getSystemService(NotificationManager.class).notify(NID, buildNotif());
    }

    // ---------- bubble ----------
    private int dp(float v) {
        return Ui.dp(this, v);
    }

    private void buildBubble() {
        bubble = new LinearLayout(this);
        bubble.setOrientation(LinearLayout.VERTICAL);
        bubble.setGravity(Gravity.CENTER_HORIZONTAL);

        mv = new MonsterView(this);
        bubble.addView(mv, new LinearLayout.LayoutParams(-2, -2));

        speech = new TextView(this);
        speech.setTextSize(12);
        speech.setTextColor(Ui.INK);
        speech.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        speech.setBackground(Ui.outline(this, Ui.WHITE, Ui.PINK, 1.5f, 14));
        speech.setPadding(dp(10), dp(6), dp(10), dp(6));
        speech.setMaxWidth(dp(190));
        speech.setVisibility(View.GONE);
        bubble.addView(speech, new LinearLayout.LayoutParams(-2, -2));

        lp = new WindowManager.LayoutParams(-2, -2, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.START;
        lp.x = st.bubbleX() >= 0 ? st.bubbleX() : dp(8);
        lp.y = st.bubbleY() >= 0 ? st.bubbleY() : dp(220);

        final int slop = Math.round(ViewConfiguration.get(this).getScaledTouchSlop() * 1.5f);
        bubble.setOnTouchListener(new View.OnTouchListener() {
            float downX, downY;
            int startX, startY;
            long t0;
            boolean moved;

            @Override
            public boolean onTouch(View v, MotionEvent e) {
                switch (e.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startX = lp.x;
                        startY = lp.y;
                        downX = e.getRawX();
                        downY = e.getRawY();
                        t0 = SystemClock.uptimeMillis();
                        moved = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        float mx = e.getRawX() - downX, my = e.getRawY() - downY;
                        if (!moved && Math.hypot(mx, my) > slop) moved = true;
                        if (moved) {
                            DisplayMetrics dm = getResources().getDisplayMetrics();
                            lp.x = Math.max(0, Math.min(dm.widthPixels - bubble.getWidth(), startX + (int) mx));
                            lp.y = Math.max(0, Math.min(dm.heightPixels - bubble.getHeight(), startY + (int) my));
                            try {
                                wm.updateViewLayout(bubble, lp);
                            } catch (Exception ignored) {
                            }
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (moved) st.setBubblePos(lp.x, lp.y);
                        else if (SystemClock.uptimeMillis() - t0 >= 600) openApp(false);
                        else onTap();
                        return true;
                    default:
                        return true;
                }
            }
        });
        try {
            wm.addView(bubble, lp);
        } catch (Exception e) {
            DebugLog.add("could not show bubble: " + e);
        }
    }

    private void onTap() {
        if (Store.MANUAL.equals(st.mode())) {
            st.addReel(true);
        } else {
            say("I count automatically 🤖 (long-press to open the app)", false);
        }
    }

    private void openApp(boolean celebrate) {
        Intent i = new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if (celebrate) i.putExtra("celebrate", true);
        startActivity(i);
    }

    private void refreshBubble() {
        if (mv == null) return;
        int c = st.count();
        float r = (float) c / Math.max(1, st.limit());
        mv.set(c, Advice.mood(r), Advice.bubbleDp(r), Advice.color(r), true);
        applyVisibility();
    }

    private void applyVisibility() {
        if (bubble == null) return;
        boolean show = !st.hideOutside() || ReelAccessibilityService.instance == null || inTarget;
        bubble.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    static void onForegroundApp(String pkg) {
        MonsterService s = instance;
        if (s == null || Perms.isNoise(pkg)) return;
        s.inTarget = Perms.isTarget(pkg);
        s.applyVisibility();
    }

    private void say(String s, boolean sticky) {
        if (speech == null) return;
        speech.setText(s);
        speech.setVisibility(View.VISIBLE);
        h.removeCallbacks(hideSpeech);
        if (!sticky) h.postDelayed(hideSpeech, 4500);
    }

    private void squish() {
        if (mv == null) return;
        mv.animate().cancel();
        mv.setScaleX(0.82f);
        mv.setScaleY(0.82f);
        mv.animate().scaleX(1f).scaleY(1f).setDuration(220).start();
    }

    // ---------- store updates ----------
    @Override
    public void onStoreChanged(boolean countUp) {
        if (bubble == null) return;
        refreshBubble();
        if (countUp) {
            squish();
            int c = st.count(), lim = Math.max(1, st.limit());
            float r = (float) c / lim;
            int stage = Advice.stage(r);
            if (c >= st.nextAlert()) {
                triggerAlert();
            } else if (c * 100 >= lim * 80 && !st.warned()) {
                st.setWarned();
                say("Heads up! 80% of today's limit is gone 😮", false);
                vibrate(false);
            } else if (c == 1 || c % 5 == 0 || stage != lastStage) {
                say(Advice.line(stage, rnd), false);
            }
            lastStage = stage;
        }
        updateNotif();
    }

    // ---------- the stop alert ----------
    private void triggerAlert() {
        if (alertView != null) return;
        int c = st.count();
        vibrate(true);
        String task = Advice.pick(st.role(), st.age(), rnd);
        sendAlertNotif(c, task);
        showAlertCard(c, task);
    }

    private void showAlertCard(int c, String task) {
        final int sn = st.snoozes();
        alertView = new FrameLayout(this);
        alertView.setBackgroundColor(0xAA1A0F24);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setBackground(Ui.round(this, Ui.WHITE, 28));
        card.setPadding(dp(22), dp(18), dp(22), dp(20));

        MonsterView big = new MonsterView(this);
        big.set(c, MonsterView.ALERT, 110, 0xFFE5484D, false);
        card.addView(big, new LinearLayout.LayoutParams(-2, -2));

        TextView title = Ui.text(this, sn == 0 ? "Reel limit reached! 🛑" : "Still scrolling? 😟", 22, Ui.DEEP, true);
        title.setGravity(Gravity.CENTER);
        card.addView(title, Ui.lp(this, 6));

        String msg = sn == 0
                ? "You watched " + c + " reels today (about " + Ui.dur(st.seconds()) + "). My belly is full!"
                : "That is " + c + " reels now. You asked for more time, but your eyes and brain need a break.";
        TextView m = Ui.text(this, msg, 15, Ui.INK, false);
        m.setGravity(Gravity.CENTER);
        card.addView(m, Ui.lp(this, 8));

        TextView t = Ui.text(this, "Try this instead:\n" + task, 15, Ui.DEEP, true);
        t.setGravity(Gravity.CENTER);
        t.setBackground(Ui.round(this, Ui.PINKBG, 16));
        t.setPadding(dp(12), dp(10), dp(12), dp(10));
        card.addView(t, Ui.lp(this, 12));

        card.addView(Ui.button(this, "✅  I'll stop now  (+50 points)", Ui.MINT, Ui.WHITE, v -> {
            st.stopHonored();
            openApp(true);
            dismissAlert();
            say("You are a star! ⭐", false);
        }), Ui.lp(this, 14));
        card.addView(Ui.button(this, "⏳  5 more reels", Ui.LAV, Ui.DEEP, v -> {
            st.snooze(5);
            dismissAlert();
            say("Okay... just 5 more. Promise? 🤞", false);
        }), Ui.lp(this, 8));

        FrameLayout.LayoutParams flp = new FrameLayout.LayoutParams(-1, -2, Gravity.CENTER);
        flp.leftMargin = dp(24);
        flp.rightMargin = dp(24);
        alertView.addView(card, flp);

        WindowManager.LayoutParams alp = new WindowManager.LayoutParams(-1, -1,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        try {
            wm.addView(alertView, alp);
        } catch (Exception e) {
            alertView = null;
            DebugLog.add("could not show alert: " + e);
        }
    }

    private void dismissAlert() {
        if (alertView != null) {
            try {
                wm.removeView(alertView);
            } catch (Exception ignored) {
            }
            alertView = null;
        }
    }

    private void sendAlertNotif(int c, String task) {
        Notification n = new Notification.Builder(this, CH_ALERT)
                .setSmallIcon(R.drawable.ic_stat)
                .setContentTitle("Time to stop scrolling 🛑")
                .setContentText(c + " reels today. Try: " + task)
                .setStyle(new Notification.BigTextStyle().bigText(c + " reels today. Try: " + task))
                .setContentIntent(openAppIntent())
                .setAutoCancel(true)
                .build();
        getSystemService(NotificationManager.class).notify(NID_ALERT, n);
    }

    @SuppressWarnings("deprecation")
    private void vibrate(boolean strong) {
        try {
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (v == null || !v.hasVibrator()) return;
            long[] pat = strong ? new long[]{0, 220, 120, 220, 120, 400} : new long[]{0, 120};
            v.vibrate(VibrationEffect.createWaveform(pat, -1));
        } catch (Exception ignored) {
        }
    }
}
