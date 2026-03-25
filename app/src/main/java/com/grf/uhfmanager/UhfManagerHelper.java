package com.grf.uhfmanager;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Parcelable;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.nlscan.android.uhf.TagInfo;
import com.nlscan.android.uhf.UHFManager;
import com.nlscan.android.uhf.UHFReader;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Simple wrapper around UHFManager providing:
 * - async powerOn
 * - powerOff
 * - start/stop inventory
 * - set/get params
 * - trigger/prompts helpers
 * - Tag read callbacks via OnTagReadListener
 * <p>
 * Based on Newland SDK usage in the developer handbook. :contentReference[oaicite:0]{index=0} :contentReference[oaicite:1]{index=1}
 */
public class UhfManagerHelper {
    private static final String TAG = "UhfManagerHelper";

    private final Context context;
    private final UHFManager uhfManager;
    private final ExecutorService executor;
    private BroadcastReceiver receiver;
    private OnTagReadListener onTagReadListener;
    IntentFilter filter = new IntentFilter(ACTION_RESULT);
    // ACTION from SDK for tag broadcast
    private static final String ACTION_RESULT = "nlscan.intent.action.uhf.ACTION_RESULT";
    private boolean isInventoryRunning = false;

    public UhfManagerHelper(Context context) {
        this.context = context.getApplicationContext();
        UHFManager tmp = null;
        try {
            tmp = UHFManager.getInstance();
        } catch (Exception e) {
            Log.e(TAG, "getInstance error", e);
        }
        this.uhfManager = tmp;
        this.executor = Executors.newSingleThreadExecutor();
    }

    /**
     * Initialize — registers the broadcast receiver to receive tag results.
     * Call from Activity.onCreate() or similar.
     */
    public void init() {
        try {
            IntentFilter filter = new IntentFilter(ACTION_RESULT);

            receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    try {
                        if (intent == null) return;

                        String action = intent.getAction();
                        if (!ACTION_RESULT.equals(action)) return;

                        Parcelable[] tagInfos = intent.getParcelableArrayExtra("tag_info");
                        if (tagInfos == null) return;

                        for (Parcelable p : tagInfos) {
                            try {
                                TagInfo tagInfo = (TagInfo) p;
                                String epc = UHFReader.bytes_Hexstr(tagInfo.EpcId);
                                int rssi = tagInfo.RSSI;

                                if (onTagReadListener != null) {
                                    onTagReadListener.onTagRead(epc, rssi, tagInfo);
                                }
                            } catch (Exception ex) {
                                Log.e(TAG, "parse tag error", ex);
                            }
                        }

                    } catch (Exception e) {
                        Log.e(TAG, "receiver onReceive error", e);
                    }
                }
            };

            // ---------- REGISTER RECEIVER COMPATIBLY ----------
            if (android.os.Build.VERSION.SDK_INT >= 33) {
                // Android 13+ requires RECEIVER_EXPORTED or RECEIVER_NOT_EXPORTED
                context.registerReceiver(
                        receiver,
                        filter,
                        Context.RECEIVER_EXPORTED
                );
            } else {
                // Older Android uses normal call
                ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
            }

        } catch (Exception e) {
            Log.e(TAG, "init/registerReceiver error", e);
        }
    }


    /**
     * Set the listener for tag read events.
     */
    public void setOnTagReadListener(OnTagReadListener listener) {
        try {
            this.onTagReadListener = listener;
        } catch (Exception e) {
            Log.e(TAG, "setOnTagReadListener error", e);
        }
    }

    /**
     * Power on the module. This is time consuming and will be executed off the main thread.
     * Callback via listener is recommended by caller (we log state here).
     */
    // In UhfManagerHelper (or wherever)
    public Future<Boolean> powerOnAsync() {
        try {
            return executor.submit(() -> {
                try {
                    if (uhfManager == null) {
                        Log.e(TAG, "UHFManager is null - cannot power on");
                        return false;
                    }

                    UHFReader.READER_STATE state = uhfManager.powerOn();
                    Log.i(TAG, "powerOn result: " + state);
                    // adjust to your SDK's "OK" enum value
                    return state == UHFReader.READER_STATE.OK_ERR;
                } catch (Exception e) {
                    Log.e(TAG, "powerOn task error", e);
                    return false;
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "powerOnAsync error", e);
            // return a completed future with false as fallback
            return CompletableFuture.completedFuture(false);
        }
    }


    /**
     * Power off the module (synchronous).
     */
    public void powerOff() {
        try {
            if (uhfManager == null) {
                Log.e(TAG, "UHFManager is null - cannot power off");
                return;
            }
            UHFReader.READER_STATE state = uhfManager.powerOff();
            Log.i(TAG, "powerOff result: " + state);
        } catch (Exception e) {
            Log.e(TAG, "powerOff error", e);
        }
    }

    /**
     * Start inventory (reads tags continuously)
     */
    public void startInventory() {
        try {
            if (uhfManager == null) {
                Log.e(TAG, "UHFManager is null - cannot start inventory");
                return;
            }

            // ------- IGNORE IF ALREADY RUNNING -------
            if (isInventoryRunning) {
                Log.w(TAG, "startInventory ignored - already running");
                return;
            }

            UHFReader.READER_STATE state = uhfManager.startTagInventory();
            Log.i(TAG, "startInventory result: " + state);

            if (state == UHFReader.READER_STATE.OK_ERR) {
                isInventoryRunning = true;
            }

        } catch (Exception e) {
            Log.e(TAG, "startInventory error", e);
        }
    }


    /**
     * Stop inventory
     */
    public void stopInventory() {
        try {
            if (uhfManager == null) {
                Log.e(TAG, "UHFManager is null - cannot stop inventory");
                return;
            }

            // ------- IGNORE IF NOT RUNNING -------
            if (!isInventoryRunning) {
                Log.w(TAG, "stopInventory ignored - not running");
                return;
            }

            UHFReader.READER_STATE state = uhfManager.stopTagInventory();
            Log.i(TAG, "stopInventory result: " + state);

            if (state == UHFReader.READER_STATE.OK_ERR) {
                isInventoryRunning = false;
            }

        } catch (Exception e) {
            Log.e(TAG, "stopInventory error", e);
        }
    }

    /**
     * Apply exact match filters for a list of EPCs.
     * Each EPC must be hex string without spaces, e.g. "AA0833B2DDD9014000000001".
     */
    public UHFReader.READER_STATE applyExactEpcFilters(List<String> epcs) {
        try {
            if (epcs == null || epcs.isEmpty()) {
                Log.w("UHF", "applyExactEpcFilters: empty epc list");
                return UHFReader.READER_STATE.OK_ERR;
            }

            // If your SDK has UHFParams constants, prefer these; else use plain strings:
            final String key = "TAG_FILTER"; // replace with UHFParams.TAG_FILTER.KEY if available
            final String paramTagFilter = "PARAM_TAG_FILTER"; // replace with UHFParams.TAG_FILTER.PARAM_TAG_FILTER
            // The SDK sample used PARAM_CLEAR for clear: UHFParams.TAG_FILTER.PARAM_CLEAR

            UHFReader.READER_STATE lastState = UHFReader.READER_STATE.CMD_FAILED_ERR;

            for (String epc : epcs) {
                try {
                    if (epc == null) continue;
                    epc = epc.trim();
                    if (epc.length() == 0) continue;

                    // Build the JSON object expected by your SDK:
                    // {"bank":1,"startaddr":32,"fdata":"AA0833...","isInvert":0}
                    org.json.JSONObject jsFilter = new org.json.JSONObject();
                    jsFilter.put("bank", 1); // 1 = EPC bank
                    jsFilter.put("startaddr", 32); // skip PC bits -> EPC starts at bit 32
                    jsFilter.put("fdata", epc);
                    jsFilter.put("isInvert", 0); // 0 = match, 1 = invert/mask

                    String sValue = jsFilter.toString();
                    Log.d("UHF", "setTagFilter JSON: " + sValue);

                    // Call your existing setParam (unchanged)
                    lastState = setParam(key, paramTagFilter, sValue);
                    Log.i("UHF", "setTagFilter result for " + epc + " : " + lastState);

                    // optional: small delay between calls if firmware needs it
                    try { Thread.sleep(80); } catch (InterruptedException ignored) {}
                } catch (Exception exEpc) {
                    Log.e("UHF", "applyExactEpcFilters per-epc error", exEpc);
                }
            }

            return lastState;

        } catch (Exception e) {
            Log.e("UHF", "applyExactEpcFilters error", e);
            return UHFReader.READER_STATE.CMD_FAILED_ERR;
        }
    }

    public UHFReader.READER_STATE clearTagFilter() {
        try {
            final String key = "TAG_FILTER"; // or UHFParams.TAG_FILTER.KEY
            final String paramClear = "PARAM_CLEAR"; // or UHFParams.TAG_FILTER.PARAM_CLEAR

            // Clear filter by calling the SDK clear param with "1" as sample code showed
            UHFReader.READER_STATE state = setParam(key, paramClear, "1");
            Log.i("UHF", "clearTagFilter result: " + state);
            return state;
        } catch (Exception e) {
            Log.e("UHF", "clearTagFilter error", e);
            return UHFReader.READER_STATE.CMD_FAILED_ERR;
        }
    }



    /**
     * Set a UHF parameter. sValue is typically a string or JSON string depending on param.
     * Example: RF_ANTPOWER, PARAM_RF_ANTPOWER, "[{\"antid\":1,\"readPower\":2700,\"writePower\":2000}]"
     */
    public UHFReader.READER_STATE setParam(String paramKey, String paramName, String sValue) {
        try {
            if (uhfManager == null) {
                Log.e(TAG, "UHFManager is null - cannot setParam");
                return UHFReader.READER_STATE.OP_NOT_SUPPORTED;
            }
            UHFReader.READER_STATE state = uhfManager.setParam(paramKey, paramName, sValue);
            Log.i(TAG, "setParam " + paramKey + " result: " + state);
            return state;
        } catch (Exception e) {
            Log.e(TAG, "setParam error", e);
            return UHFReader.READER_STATE.CMD_FAILED_ERR;
        }
    }


    public UHFReader.READER_STATE setMaxAntennaPower() {
        try {
            JSONArray antArray = new JSONArray();
            JSONObject antObj = new JSONObject();

            antObj.put("antid", 1);
            antObj.put("readPower", 3000);   // MAX VALUE
            antObj.put("writePower", 3000);  // MAX VALUE

            antArray.put(antObj);

            return setParam(
                    "RF_ANTPOWER",
                    "PARAM_RF_ANTPOWER",
                    antArray.toString()
            );

        } catch (Exception e) {
            Log.e(TAG, "setMaxAntennaPower error", e);
            return UHFReader.READER_STATE.CMD_FAILED_ERR;
        }
    }

    public UHFReader.READER_STATE setAllMaxPowerParams() {
        try {

            // 1) Antenna max power
            UHFReader.READER_STATE r1 = setMaxAntennaPower();
            if (r1 != UHFReader.READER_STATE.OK_ERR) return r1;

            // 2) Low battery power limit = 3000
            UHFReader.READER_STATE r2 =
                    setParam("LOWER_POWER", "PARAM_LOWER_POWER_DBM", "3000");
            if (r2 != UHFReader.READER_STATE.OK_ERR) return r2;

            // 3) High temperature fallback power = 3000
            UHFReader.READER_STATE r3 =
                    setParam("HIGH_TEMPERATURE", "PARAM_HIGH_TEMPERATURE_ANT_POWER", "3000");
            if (r3 != UHFReader.READER_STATE.OK_ERR) return r3;

            return UHFReader.READER_STATE.OK_ERR;

        } catch (Exception e) {
            Log.e(TAG, "setAllMaxPowerParams error", e);
            return UHFReader.READER_STATE.CMD_FAILED_ERR;
        }
    }



    /**
     * Get all parameters as Map (direct SDK call)
     */
    public Map<String, Object> GetAllParams() {
        try {
            if (uhfManager == null) {
                Log.e(TAG, "UHFManager is null - cannot getAllParams");
                return null;
            }
            return uhfManager.getAllParams();
        } catch (Exception e) {
            Log.e(TAG, "getAllParams error", e);
            return null;
        }
    }

    public String mapToJson(Map<String, Object> map) {
        try {
            if (map == null) return "{}";

            JSONObject json = new JSONObject();

            for (String key : map.keySet()) {
                Object value = map.get(key);

                // Convert int[] → JSONArray
                if (value instanceof int[]) {
                    json.put(key, new JSONArray(value));
                    continue;
                }

                // Convert long[] → JSONArray
                if (value instanceof long[]) {
                    json.put(key, new JSONArray(value));
                    continue;
                }

                // Convert JSON strings into real JSON
                if (value instanceof String) {
                    String s = value.toString().trim();

                    try {
                        if (s.startsWith("{")) {
                            json.put(key, new JSONObject(s));
                        } else if (s.startsWith("[")) {
                            json.put(key, new JSONArray(s));
                        } else {
                            json.put(key, s);
                        }
                    } catch (Exception ex) {
                        json.put(key, s); // fallback plain text
                    }

                    continue;
                }

                // Add other types directly
                json.put(key, value);
            }

            return json.toString(4); // formatted JSON

        } catch (Exception e) {
            e.printStackTrace();
            return "{}";
        }
    }


    /**
     * Convenience: set trigger on/off (main/left/right/back)
     * Example triggerId: UHFCommonParams.TRIGGER_MODE.TRIGGER_MODE_MAIN
     */
    public boolean setTrigger(String triggerId, boolean on) {
        try {
            if (uhfManager == null) {
                Log.e(TAG, "UHFManager is null - cannot setTrigger");
                return false;
            }
            return uhfManager.setTrigger(triggerId, on);
        } catch (Exception e) {
            Log.e(TAG, "setTrigger error", e);
            return false;
        }
    }

    /**
     * Prompt sound/vibrate helpers
     */
    public boolean setPromptSoundEnable(boolean enable) {
        try {
            if (uhfManager == null) return false;
            return uhfManager.setPromptSoundEnable(enable);
        } catch (Exception e) {
            Log.e(TAG, "setPromptSoundEnable error", e);
            return false;
        }
    }

    public boolean setPromptVibrateEnable(boolean enable) {
        try {
            if (uhfManager == null) return false;
            return uhfManager.setPromptVibrateEnable(enable);
        } catch (Exception e) {
            Log.e(TAG, "setPromptVibrateEnable error", e);
            return false;
        }
    }

    /**
     * Clean up — unregister receiver, shutdown executor, power off reader
     * Call from Activity.onDestroy()
     */
    public void destroy() {
        try {
            try {
                if (receiver != null) {
                    context.unregisterReceiver(receiver);
                    receiver = null;
                }
            } catch (Exception e) {
                Log.w(TAG, "unregisterReceiver error", e);
            }

            try {
                powerOff();
            } catch (Exception ignored) {
            }

            try {
                executor.shutdownNow();
            } catch (Exception ignored) {
            }

        } catch (Exception e) {
            Log.e(TAG, "destroy error", e);
        }
    }

    /**
     * Listener interface for tag read callbacks
     */
    public interface OnTagReadListener {
        /**
         * Called when a tag is read.
         *
         * @param epc  EPC as hex string
         * @param rssi RSSI integer
         * @param raw  original TagInfo object if you need other fields
         */
        void onTagRead(String epc, int rssi, TagInfo raw);
    }


}
