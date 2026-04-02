package com.grf.smarttagmanager;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatDelegate;

import com.grf.utils.SoundUtils;

public class App extends Application {
    public static int ReaderType = 2;
    private static App instance;
    private static String DeviceMac;

    @Override
    public void onCreate() {
        try {
            super.onCreate();
            // Force light (day) theme for the entire app
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            instance = this;

            // Apply fullscreen to ALL activities
            registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
                @Override
                public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                    try {
                        activity.getWindow().setFlags(
                                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                                WindowManager.LayoutParams.FLAG_FULLSCREEN
                        );
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onActivityStarted(Activity activity) {
                }

                @Override
                public void onActivityResumed(Activity activity) {
                }

                @Override
                public void onActivityPaused(Activity activity) {
                }

                @Override
                public void onActivityStopped(Activity activity) {
                }

                @Override
                public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
                }

                @Override
                public void onActivityDestroyed(Activity activity) {
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static Context getContext() {
        try {
            return instance.getApplicationContext();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // ✅ Getter for DeviceMac
    public static String getDeviceMac() {
        try {
            return DeviceMac != null ? DeviceMac : "";
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    // ✅ Setter for DeviceMac
    public static void setDeviceMac(String mac) {
        try {
            DeviceMac = mac;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}