package com.bharatspace.reelmonster;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent i) {
        if (i == null || !Intent.ACTION_BOOT_COMPLETED.equals(i.getAction())) return;
        Store st = Store.get(c);
        if (st.monsterOn() && Settings.canDrawOverlays(c)) {
            try {
                c.startForegroundService(new Intent(c, MonsterService.class));
            } catch (Exception ignored) {
            }
        }
    }
}
