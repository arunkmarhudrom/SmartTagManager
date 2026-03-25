package com.grf.utils;

public class LogUtils {

    private static final String DEFAULT_TAG = "MyAppLog";
    private static boolean isLoggingEnabled = true;
//LogUtils.enableLogging(false);

    private LogUtils() {
        // no instance
    }

    public static void enableLogging(boolean enable) {
        isLoggingEnabled = enable;
    }

    // -------------------- INFO --------------------
    public static void i(String tag, String msg) {
        try {
            if (isLoggingEnabled) {
                android.util.Log.i(tag, msg);
            }
        } catch (Exception e) {
            android.util.Log.e(DEFAULT_TAG, "LogUtils.i error: " + e.getMessage());
        }
    }

    public static void i(String msg) {
        i(DEFAULT_TAG, msg);
    }

    // -------------------- DEBUG --------------------
    public static void d(String tag, String msg) {
        try {
            if (isLoggingEnabled) {
                android.util.Log.d(tag, msg);
            }
        } catch (Exception e) {
            android.util.Log.e(DEFAULT_TAG, "LogUtils.d error: " + e.getMessage());
        }
    }

    public static void d(String msg) {
        d(DEFAULT_TAG, msg);
    }

    // -------------------- ERROR --------------------
    public static void e(String tag, String msg) {
        try {
            if (isLoggingEnabled) {
                android.util.Log.e(tag, msg);
            }
        } catch (Exception ex) {
            android.util.Log.e(DEFAULT_TAG, "LogUtils.e error: " + ex.getMessage());
        }
    }

    public static void e(String msg) {
        e(DEFAULT_TAG, msg);
    }

    public static void e(String tag, String msg, Throwable t) {
        try {
            if (isLoggingEnabled) {
                android.util.Log.e(tag, msg, t);
            }
        } catch (Exception ex) {
            android.util.Log.e(DEFAULT_TAG, "LogUtils.e (throwable) error: " + ex.getMessage());
        }
    }

    // -------------------- WARNING --------------------
    public static void w(String tag, String msg) {
        try {
            if (isLoggingEnabled) {
                android.util.Log.w(tag, msg);
            }
        } catch (Exception e) {
            android.util.Log.e(DEFAULT_TAG, "LogUtils.w error: " + e.getMessage());
        }
    }

    public static void w(String msg) {
        w(DEFAULT_TAG, msg);
    }

    // -------------------- VERBOSE --------------------
    public static void v(String tag, String msg) {
        try {
            if (isLoggingEnabled) {
                android.util.Log.v(tag, msg);
            }
        } catch (Exception e) {
            android.util.Log.e(DEFAULT_TAG, "LogUtils.v error: " + e.getMessage());
        }
    }

    public static void v(String msg) {
        v(DEFAULT_TAG, msg);
    }
}
