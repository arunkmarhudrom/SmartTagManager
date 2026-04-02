package com.grf.helper;

import android.app.ProgressDialog;
import android.content.Context;

public class LoaderUtil {

    private static ProgressDialog dialog;

    public static void show(Context context, String message) {
        try {
            if (dialog != null && dialog.isShowing()) return;

            dialog = new ProgressDialog(context);
            dialog.setMessage(message);
            dialog.setCancelable(false);
            dialog.setCanceledOnTouchOutside(false);
            dialog.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void hide() {
        try {
            if (dialog != null && dialog.isShowing()) {
                dialog.dismiss();
                dialog = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}