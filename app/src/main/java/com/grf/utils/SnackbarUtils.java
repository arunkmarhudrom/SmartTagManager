package com.grf.utils;

import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

import com.google.android.material.snackbar.Snackbar;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.annotation.NonNull;

import com.google.android.material.snackbar.Snackbar;

import android.view.Gravity;
import android.util.TypedValue;
import android.util.Log;

public final class SnackbarUtils {
    private static final String TAG = "SnackbarUtils";

    private SnackbarUtils() { /* no instance */ }

    public static void show(@NonNull View anchorView, @NonNull String message) {
        try {
            if (anchorView == null || message == null) return;

            Snackbar snackbar = Snackbar.make(anchorView, message, Snackbar.LENGTH_SHORT);
            moveSnackbarToTop(anchorView, snackbar, 16); // 16dp top margin default
            snackbar.show();
        } catch (Exception e) {
            Log.e(TAG, "showTop error", e);
        }
    }

    public static void showTopLong(@NonNull View anchorView, @NonNull String message) {
        try {
            if (anchorView == null || message == null) return;

            Snackbar snackbar = Snackbar.make(anchorView, message, Snackbar.LENGTH_LONG);
            moveSnackbarToTop(anchorView, snackbar, 16);
            snackbar.show();
        } catch (Exception e) {
            Log.e(TAG, "showTopLong error", e);
        }
    }

    private static void moveSnackbarToTop(@NonNull View anchorView, @NonNull Snackbar snackbar, int topMarginDp) {
        try {
            View sbView = snackbar.getView();
            ViewGroup.LayoutParams lp = sbView.getLayoutParams();
            int topMarginPx = dpToPx(anchorView.getContext(), topMarginDp);

            if (lp instanceof FrameLayout.LayoutParams) {
                FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) lp;
                params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
                params.topMargin = topMarginPx;
                sbView.setLayoutParams(params);
                return;
            }

            if (lp instanceof CoordinatorLayout.LayoutParams) {
                CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) lp;
                params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
                params.setMargins(params.leftMargin, topMarginPx, params.rightMargin, params.bottomMargin);
                sbView.setLayoutParams(params);
                return;
            }

            // fallback: try to adjust translationY so it appears near top (best-effort)
            sbView.setTranslationY(-(anchorView.getRootView().getHeight() / 3f)); // crude fallback
        } catch (Exception e) {
            Log.w(TAG, "moveSnackbarToTop failed", e);
        }
    }

    private static int dpToPx(Context c, int dp) {
        try {
            return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, c.getResources().getDisplayMetrics());
        } catch (Exception e) {
            return dp;
        }
    }
}
