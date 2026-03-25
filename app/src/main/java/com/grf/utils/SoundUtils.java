package com.grf.utils;


import android.content.Context;
import android.media.MediaPlayer;
import android.util.Log;

public class SoundUtils {

    private static final String TAG = "SoundPlayer";

    public static void play(Context context, int rawSoundRes) {
        try {
            MediaPlayer mp = MediaPlayer.create(context, rawSoundRes);

            mp.setOnCompletionListener(player -> {
                try {
                    player.stop();
                    player.release();
                } catch (Exception ignored) {}
            });

            mp.setOnErrorListener((player, what, extra) -> {
                try {
                    player.release();
                } catch (Exception ignored) {}
                return true;
            });

            mp.start();

        } catch (Exception e) {
            Log.e(TAG, "play: error", e);
        }
    }
}
