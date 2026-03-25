package com.grf.utils;


import android.view.KeyEvent;

public interface OnKeyPressHandler {
    /**
     * Called when a key down event is received.
     *
     * @return true if consumed, false to allow default handling.
     */
    boolean onKeyDownEvent(int keyCode, KeyEvent event);

    /**
     * Called when a key up event is received.
     *
     * @return true if consumed, false to allow default handling.
     */
    boolean onKeyUpEvent(int keyCode, KeyEvent event);
}
