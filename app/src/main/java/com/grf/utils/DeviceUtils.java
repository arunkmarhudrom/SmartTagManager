package com.grf.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.util.Log;

import com.grf.smarttagmanager.App;


public class DeviceUtils {


    @SuppressLint("HardwareIds")
    public static String getOrCreateDeviceId(Context context) {
        try {
            SharedPreferences sp = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE);
            String id = sp.getString("device_id", null);

            if (id == null) {
                id = Settings.Secure.getString(
                        context.getContentResolver(),
                        Settings.Secure.ANDROID_ID
                );
                sp.edit().putString("device_id", id).apply();

            }
            LogUtils.e("deviceId", id);
            App.setDeviceMac(id);
            return id;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }
}