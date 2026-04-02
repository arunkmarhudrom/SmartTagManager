package com.grf.helper;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPreferencesHelper {

    private static final String PREF_NAME = "app_settings";
    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;

    public SharedPreferencesHelper(Context context) {
        try {
            prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            editor = prefs.edit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // String
    public void putString(String key, String value) {
        try {
            editor.putString(key, value);
            editor.apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getString(String key) {
        try {
            return prefs.getString(key, "");
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    // Int
    public void putInt(String key, int value) {
        try {
            editor.putInt(key, value);
            editor.apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getInt(String key) {
        try {
            return prefs.getInt(key, 0);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    // Boolean
    public void putBoolean(String key, boolean value) {
        try {
            editor.putBoolean(key, value);
            editor.apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean getBoolean(String key) {
        try {
            return prefs.getBoolean(key, true);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // Long
    public void putLong(String key, long value) {
        try {
            editor.putLong(key, value);
            editor.apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public long getLong(String key) {
        try {
            return prefs.getLong(key, 0L);
        } catch (Exception e) {
            e.printStackTrace();
            return 0L;
        }
    }

    // Float
    public void putFloat(String key, float value) {
        try {
            editor.putFloat(key, value);
            editor.apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public float getFloat(String key) {
        try {
            return prefs.getFloat(key, 0f);
        } catch (Exception e) {
            e.printStackTrace();
            return 0f;
        }
    }

    // Remove key
    public void remove(String key) {
        try {
            editor.remove(key);
            editor.apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Clear all
    public void clear() {
        try {
            editor.clear();
            editor.apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



   /* SharedPreferencesHelper pref = new SharedPreferencesHelper(this);

// Save
pref.putString("username", "meow");
pref.putInt("age", 25);
pref.putBoolean("isLogin", true);

    // Get
    String name = pref.getString("username");
    int age = pref.getInt("age");
    boolean isLogin = pref.getBoolean("isLogin");*/
}