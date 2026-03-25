package com.grf.uhfmanager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import java.util.Map;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * ReconnectManager
 *
 * - Uses AtomicBoolean for connectInProgress to avoid races
 * - Expects UhfManagerHelper.powerOnAsync() -> Future<Boolean>
 * - Waits with timeout for powerOn result before probing getAllParams()
 * - Attempts reconnect when page visible or screen unlocked
 * - Keeps try/catch everywhere as requested
 */
public class ReconnectManager {

    private static final String TAG = "ReconnectManager";

    private final Context context;
    private final ExecutorService scheduler;
    private final UhfManagerHelper uhfHelper;
    private final BroadcastReceiver screenReceiver;

    private final AtomicBoolean connectInProgress = new AtomicBoolean(false);
    private volatile boolean isReaderConnected = false;
    private volatile boolean isVisible = false; // page visibility flag

    private static final int MAX_CONNECT_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 1000L; // ms between attempts
    private static final long POWER_ON_TIMEOUT_MS = 2000L; // wait for powerOn result
    private static final long POST_POWER_STABILIZE_MS = 200L; // small stabilize delay

    public ReconnectManager(Context context, ExecutorService scheduler, UhfManagerHelper uhfHelper) {
        this.context = context.getApplicationContext();
        this.scheduler = scheduler;
        this.uhfHelper = uhfHelper;

        screenReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent intent) {
                try {
                    String action = intent.getAction();
                    if (action == null) return;

                    if (action.equals(Intent.ACTION_SCREEN_ON) || action.equals(Intent.ACTION_USER_PRESENT)) {
                        Log.d(TAG, "Screen on / user present received");
                        handleScreenOnEvent();
                    } else if (action.equals(Intent.ACTION_SCREEN_OFF)) {
                        Log.d(TAG, "Screen off received");
                        // Optional: mark disconnected or pause health checks
                    }
                } catch (Exception e) {
                    Log.e(TAG, "screenReceiver error", e);
                }
            }
        };
    }

    // Call from Activity#onCreate or when you want to start listening
    public void start() {
        try {
            IntentFilter filter = new IntentFilter();
            try {
                filter.addAction(Intent.ACTION_SCREEN_ON);
                filter.addAction(Intent.ACTION_SCREEN_OFF);
                filter.addAction(Intent.ACTION_USER_PRESENT);
            } catch (Exception ignored) {}

            try {
                context.registerReceiver(screenReceiver, filter);
            } catch (Exception e) {
                Log.e(TAG, "registerReceiver failed", e);
            }
        } catch (Exception e) {
            Log.e(TAG, "start error", e);
        }
    }

    // Call from Activity#onDestroy or when stopping
    public void stop() {
        try {
            try {
                context.unregisterReceiver(screenReceiver);
            } catch (Exception ignored) {}
        } catch (Exception e) {
            Log.e(TAG, "stop error", e);
        }
    }

    private void handleScreenOnEvent() {
        try {
            if (isVisible) {
                attemptReconnectOnce();
            } else {
                // If you want reconnect even when not visible, uncomment:
                // attemptReconnectOnce();
            }
        } catch (Exception e) {
            Log.e(TAG, "handleScreenOnEvent error", e);
        }
    }

    /** Call from Activity/Fragment onResume */
    public void onVisible() {
        try {
            isVisible = true;
            if (!isReaderConnected && !connectInProgress.get()) {
                attemptReconnectOnce();
            }
        } catch (Exception e) {
            Log.e(TAG, "onVisible error", e);
        }
    }

    /** Call from Activity/Fragment onPause */
    public void onInvisible() {
        try {
            isVisible = false;
        } catch (Exception e) {
            Log.e(TAG, "onInvisible error", e);
        }
    }

    public boolean isReaderConnected() {
        return isReaderConnected;
    }

    public boolean isConnectInProgress() {
        return connectInProgress.get();
    }

    /**
     * connectWithRetries: tries to connect up to MAX_CONNECT_RETRIES times.
     * Uses powerOnAsync() -> Future<Boolean> and waits for result with timeout.
     */
    public void connectWithRetries() {
        try {
            // atomically claim if not already in progress
            if (!connectInProgress.compareAndSet(false, true)) {
                Log.d(TAG, "connectWithRetries: already in progress");
                return;
            }

            scheduler.execute(() -> {
                boolean ok = false;
                try {
                    int attempt = 0;

                    while (attempt < MAX_CONNECT_RETRIES && !ok) {
                        attempt++;
                        try {
                            Log.d(TAG, "Connect attempt " + attempt);

                            // 1) power on and wait for result (Future<Boolean>)
                            Future<Boolean> powerFuture = null;
                            try {
                                powerFuture = uhfHelper.powerOnAsync();
                            } catch (Exception e) {
                                Log.e(TAG, "powerOnAsync scheduling error", e);
                                powerFuture = null;
                            }

                            boolean powered = false;
                            if (powerFuture != null) {
                                try {
                                    powered = powerFuture.get(POWER_ON_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                                } catch (TimeoutException te) {
                                    Log.w(TAG, "powerOn timed out");
                                    powered = false;
                                } catch (Exception e) {
                                    Log.e(TAG, "powerOn exception", e);
                                    powered = false;
                                }
                            } else {
                                Log.w(TAG, "powerFuture was null");
                            }

                            // small stabilization delay if power was ok
                            try {
                                if (powered) Thread.sleep(POST_POWER_STABILIZE_MS);
                            } catch (InterruptedException ignored) {}

                            if (!powered) {
                                Log.w(TAG, "Power on unsuccessful - will retry");
                                // go to next attempt after delay
                            } else {
                                // 2) probe the SDK for params to determine connectivity
                                Map<String, Object> params = null;
                                try {
                                    params = uhfHelper.GetAllParams();
                                } catch (Exception e) {
                                    Log.e(TAG, "getAllParams error", e);
                                }

                                if (params != null) {
                                    ok = true;
                                    Log.d(TAG, "Connected on attempt " + attempt);
                                    break;
                                } else {
                                    Log.w(TAG, "getAllParams returned null");
                                }
                            }

                        } catch (Exception e) {
                            Log.e(TAG, "Connect attempt failed", e);
                        }

                        try {
                            Thread.sleep(RETRY_DELAY_MS);
                        } catch (InterruptedException ignored) {}
                    }
                } finally {
                    connectInProgress.set(false);
                    isReaderConnected = ok;
                    if (!ok) {
                        Log.e(TAG, "FAILED to connect after " + MAX_CONNECT_RETRIES + " attempts");
                    }
                }
            });
        } catch (Exception e) {
            connectInProgress.set(false);
            Log.e(TAG, "connectWithRetries outer error", e);
        }
    }

    /**
     * attemptReconnectOnce: single try (non-looping) reconnect attempt.
     * Similar to connectWithRetries but does a single powerOn -> getAllParams probe.
     */
    public void attemptReconnectOnce() {
        try {
            if (!connectInProgress.compareAndSet(false, true)) {
                Log.d(TAG, "attemptReconnectOnce: already in progress");
                return;
            }

            scheduler.execute(() -> {
                boolean success = false;
                try {
                    try {
                        uhfHelper.powerOff();
                    } catch (Exception ignored) {}

                    try {
                        Thread.sleep(400);
                    } catch (InterruptedException ignored) {}

                    Future<Boolean> powerFuture = null;
                    try {
                        powerFuture = uhfHelper.powerOnAsync();
                    } catch (Exception e) {
                        Log.e(TAG, "powerOnAsync scheduling error", e);
                        powerFuture = null;
                    }

                    boolean powered = false;
                    if (powerFuture != null) {
                        try {
                            powered = powerFuture.get(POWER_ON_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                        } catch (TimeoutException te) {
                            Log.w(TAG, "powerOn timed out");
                            powered = false;
                        } catch (Exception e) {
                            Log.e(TAG, "powerOn exception", e);
                            powered = false;
                        }
                    } else {
                        Log.w(TAG, "powerFuture was null");
                    }

                    try {
                        if (powered) Thread.sleep(POST_POWER_STABILIZE_MS);
                    } catch (InterruptedException ignored) {}

                    if (powered) {
                        Map<String, Object> params = null;
                        try {
                            params = uhfHelper.GetAllParams();
                        } catch (Exception e) {
                            Log.e(TAG, "getAllParams error", e);
                        }

                        if (params != null) {
                            Log.i(TAG, "Reconnected OK");
                            isReaderConnected = true;
                            success = true;
                            // Optionally restart inventory:
                            try {
                                // uhfHelper.startInventory();
                            } catch (Exception ignored) {}
                        } else {
                            Log.e(TAG, "Reconnect failed - params null");
                            isReaderConnected = false;
                        }
                    } else {
                        Log.e(TAG, "Reconnect failed - power on unsuccessful");
                        isReaderConnected = false;
                    }

                } catch (Exception e) {
                    Log.e(TAG, "Reconnect exception", e);
                    isReaderConnected = false;
                } finally {
                    connectInProgress.set(false);
                    if (!success) {
                        // optional: schedule a single retry after delay (not loop)
                        // scheduler.schedule(this::attemptReconnectOnce, RETRY_DELAY_MS, TimeUnit.MILLISECONDS);
                    }
                }
            });
        } catch (Exception e) {
            connectInProgress.set(false);
            Log.e(TAG, "attemptReconnectOnce outer error", e);
        }
    }

    // Public API: ensure connected (will start connectWithRetries if needed)
    public void ensureConnected() {
        try {
            if (!isReaderConnected && !connectInProgress.get()) {
                connectWithRetries();
            }
        } catch (Exception e) {
            Log.e(TAG, "ensureConnected error", e);
        }
    }
}
