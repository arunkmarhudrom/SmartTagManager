package com.grf.utils;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
public class PermissionUtil {

    public interface PermissionCallback {
        void onGranted();
        void onDenied();
    }

    private static final String TAG = "PermissionUtil";

    /**
     * Call this to ensure storage access suitable for your target use-case.
     *
     * - For API >= 30: checks Environment.isExternalStorageManager() and if false opens Settings.
     * - For API 29: the app may rely on requestLegacyExternalStorage if targetSdk==29 (not covered here).
     * - For API < 29: requests WRITE_EXTERNAL_STORAGE runtime permission.
     *
     * If you only need app-internal storage (filesDir), you do NOT need to call this.
     *
     * @param activity activity used to request permissions / open settings
     * @param requestCode requestCode for ActivityCompat.requestPermissions for pre-R permission flow
     */
    public static void requestStoragePermission(Activity activity, int requestCode) {
        try {
            if (activity == null) return;

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                // For Android 11+ check All files access
                try {
                    if (!android.os.Environment.isExternalStorageManager()) {
                        // Open Settings for user to grant "All files access"
                        Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                        intent.setData(android.net.Uri.parse("package:" + activity.getPackageName()));
                        try {
                            activity.startActivity(intent);
                        } catch (Exception inner) {
                            // fallback to general all-files settings page
                            Intent fallback = new Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                            activity.startActivity(fallback);
                        }
                    } else {
                        // already granted
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return;
            }

            // For Android Q (29) and below:
            // On Q you generally should use MediaStore/SAF; runtime request still applies for < Q in practice.
            String writePerm = android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
            String readPerm = android.Manifest.permission.READ_EXTERNAL_STORAGE;

            boolean writeGranted = androidx.core.content.ContextCompat.checkSelfPermission(activity, writePerm) == PackageManager.PERMISSION_GRANTED;
            boolean readGranted = androidx.core.content.ContextCompat.checkSelfPermission(activity, readPerm) == PackageManager.PERMISSION_GRANTED;

            if (!writeGranted || !readGranted) {
                androidx.core.app.ActivityCompat.requestPermissions(
                        activity,
                        new String[]{writePerm, readPerm},
                        requestCode
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Handle the result from ActivityCompat.requestPermissions or from your Activity's onRequestPermissionsResult.
     * For API >= 30 you must also check Environment.isExternalStorageManager() separately (see requestStoragePermission).
     *
     * Usage: call this from Activity.onRequestPermissionsResult(requestCode, permissions, grantResults)
     */
    public static void handleResult(
            int requestCode,
            int expectedCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults,
            PermissionCallback callback
    ) {
        try {
            if (requestCode != expectedCode) return;

            // If no results, treat as denied
            if (grantResults == null || grantResults.length == 0) {
                if (callback != null) callback.onDenied();
                return;
            }

            boolean allGranted = true;
            for (int r : grantResults) {
                if (r != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                if (callback != null) callback.onGranted();
            } else {
                if (callback != null) callback.onDenied();
            }
        } catch (Exception e) {
            e.printStackTrace();
            try {
                if (callback != null) callback.onDenied();
            } catch (Exception ignored) {}
        }
    }

    /**
     * Helper to check whether we currently have broad access (API 30+).
     */
    public static boolean isAllFilesAccessGranted() {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                return android.os.Environment.isExternalStorageManager();
            }
            // Pre-R: rely on runtime permissions
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
