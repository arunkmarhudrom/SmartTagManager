package com.grf.utils;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;


import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.grf.smarttagmanager.R;

public class PopupUtils {

    public interface PopupCallback {
        void onYes();

        void onNo();

        void onCLose();
    }

    public static void showCustomYesNoDialog(Context context, String title, String message, PopupCallback callback) {
        try {
            Dialog dialog = new Dialog(context);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setCancelable(false);

            // Inflate custom layout
            LayoutInflater inflater = LayoutInflater.from(context);
            android.view.View view = inflater.inflate(R.layout.dialog_yes_no, null);
            dialog.setContentView(view);

            // Set title & message
            TextView tvTitle = view.findViewById(R.id.tvTitle);
            TextView tvMessage = view.findViewById(R.id.tvMessage);
            Button btnYes = view.findViewById(R.id.btnYes);
            Button btnNo = view.findViewById(R.id.btnNo);
            ImageView close = view.findViewById(R.id.ivClose);

            tvTitle.setText(title);
            tvMessage.setText(message);

            btnYes.setOnClickListener(v -> {
                try {
                    callback.onYes();
                } catch (Exception ignored) {
                }
                dialog.dismiss();
            });

            btnNo.setOnClickListener(v -> {
                try {
                    callback.onNo();
                } catch (Exception ignored) {
                }
                dialog.dismiss();
            });

            close.setOnClickListener(v -> {
                try {
                    callback.onCLose();
                } catch (Exception ignored) {
                }
                dialog.dismiss();
            });

            dialog.show();

            // ------------------ ONLY THESE LINES WERE ADDED ------------------
            Window window = dialog.getWindow();
            if (window != null) {
                window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT)); // keep rounded corners
                window.setLayout(
                        (int) (context.getResources().getDisplayMetrics().widthPixels * 0.90),
                        WindowManager.LayoutParams.WRAP_CONTENT
                );
            }
            // ------------------------------------------------------------------

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

