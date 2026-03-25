package com.grf.utils;


import android.content.Context;
import android.content.SharedPreferences;
import java.util.Set;

public class PreferenceUtils {

    private static final String PREF_NAME = "APP_SHARED_PREF";

    private static SharedPreferences getPref(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // --------------------------------------------------
    // String
    // --------------------------------------------------
    public static void setString(Context context, String key, String value) {
        try {
            getPref(context).edit().putString(key, value).apply();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static String getString(Context context, String key, String def) {
        try {
            return getPref(context).getString(key, def);
        } catch (Exception ex) {
            ex.printStackTrace();
            return def;
        }
    }

    // --------------------------------------------------
    // int
    // --------------------------------------------------
    public static void setInt(Context context, String key, int value) {
        try {
            getPref(context).edit().putInt(key, value).apply();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static int getInt(Context context, String key, int def) {
        try {
            return getPref(context).getInt(key, def);
        } catch (Exception ex) {
            ex.printStackTrace();
            return def;
        }
    }

    // --------------------------------------------------
    // boolean
    // --------------------------------------------------
    public static void setBoolean(Context context, String key, boolean value) {
        try {
            getPref(context).edit().putBoolean(key, value).apply();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static boolean getBoolean(Context context, String key, boolean def) {
        try {
            return getPref(context).getBoolean(key, def);
        } catch (Exception ex) {
            ex.printStackTrace();
            return def;
        }
    }

    // --------------------------------------------------
    // float
    // --------------------------------------------------
    public static void setFloat(Context context, String key, float value) {
        try {
            getPref(context).edit().putFloat(key, value).apply();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static float getFloat(Context context, String key, float def) {
        try {
            return getPref(context).getFloat(key, def);
        } catch (Exception ex) {
            ex.printStackTrace();
            return def;
        }
    }

    // --------------------------------------------------
    // long
    // --------------------------------------------------
    public static void setLong(Context context, String key, long value) {
        try {
            getPref(context).edit().putLong(key, value).apply();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static long getLong(Context context, String key, long def) {
        try {
            return getPref(context).getLong(key, def);
        } catch (Exception ex) {
            ex.printStackTrace();
            return def;
        }
    }

    // --------------------------------------------------
    // String Set
    // --------------------------------------------------
    public static void setStringSet(Context context, String key, Set<String> value) {
        try {
            getPref(context).edit().putStringSet(key, value).apply();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static Set<String> getStringSet(Context context, String key, Set<String> def) {
        try {
            return getPref(context).getStringSet(key, def);
        } catch (Exception ex) {
            ex.printStackTrace();
            return def;
        }
    }

    // --------------------------------------------------
    // Remove Key
    // --------------------------------------------------
    public static void remove(Context context, String key) {
        try {
            getPref(context).edit().remove(key).apply();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // --------------------------------------------------
    // Clear All
    // --------------------------------------------------
    public static void clear(Context context) {
        try {
            getPref(context).edit().clear().apply();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
