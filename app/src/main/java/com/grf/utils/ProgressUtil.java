package com.grf.utils;


import android.content.Context;

import io.github.rupinderjeet.kprogresshud.KProgressHUD;

public class ProgressUtil {

    private static KProgressHUD hud;

    // Show with custom message
    public static void showLoading(Context context, String message) {
        dismiss(); // Ensure old HUD is closed
        if (context == null) return;

        hud = KProgressHUD.create(context)
                .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                .setLabel(message != null ? message : "Loading...")
                .setCancellable(false)
                .setAnimationSpeed(2)
                .setDimAmount(0.5f);
        hud.show();
    }

    // Show with default message
    public static void showLoading(Context context) {
        showLoading(context, "Loading...");
    }

    // Dismiss safely
    public static void dismiss() {
        if (hud != null && hud.isShowing()) {
            hud.dismiss();
            hud = null;
        }
    }

    public static boolean isShowing() {
        return hud != null && hud.isShowing();
    }
}