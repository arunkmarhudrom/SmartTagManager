package com.grf.utils;


import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.util.Log;

import com.grf.smarttagmanager.R;

public class SoundUtils {

    private static SoundPool soundPool;
    private static int soundId;
    private static boolean isLoaded = false;

    private static long lastPlayTime = 0;
    private static final long MIN_INTERVAL = 10; // 🔥 VERY LOW (allows near-every tag)

    public static void init(Context context) {
        try {
            if (soundPool != null) return;

            AudioAttributes attrs = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();

            soundPool = new SoundPool.Builder()
                    .setMaxStreams(10) // 🔥 allow overlap
                    .setAudioAttributes(attrs)
                    .build();

            soundId = soundPool.load(context, R.raw.beep, 1);

            soundPool.setOnLoadCompleteListener((sp, id, status) -> {
                if (status == 0) isLoaded = true;
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void play() {
        try {
            long now = System.currentTimeMillis();

            // 🔥 ultra-fast but still protected
            if (now - lastPlayTime < MIN_INTERVAL) return;

            lastPlayTime = now;

            if (soundPool != null && isLoaded) {
                soundPool.play(soundId, 1f, 1f, 1, 0, 1f);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void release() {
        try {
            if (soundPool != null) {
                soundPool.release();
                soundPool = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}