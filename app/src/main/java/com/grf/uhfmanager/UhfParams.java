package com.grf.uhfmanager;


import android.text.TextUtils;
import android.util.Log;

import java.util.List;

/**
 * Settings parameters constants
 */
public class UhfParams {

    public int PARAM_LOWER_POWER_LEVEL;
    public int INV_POLICY;
    public int PARAM_HIGH_TEMPERATURE_MONITOR_ENABLE;
    public String PARAM_CONNECT_ON_BOOT;
    public int INV_FIELD_RSSI;
    public String INV_POLICY_DATA;
    public int PARAM_EXTEND_OUTPUT_MODE;
    public List<Integer> POTL_GEN2_TARGET;
    public int PARAM_LOWER_POWER_DM_ENABLE;
    public int PARAM_TAG_SENDER_COUNT;
    public int INV_SMART_MODE;
    public int PARAM_EXTEND_OUTPUT_MODE_BANK;
    public List<Integer> POTL_GEN2_SESSION;
    public int INV_QUICK_MODE_EX;
    public int PARAM_BATTERY_WARNING_ENABLE;
    public String PARAM_HIGH_TEMPERATURE_INV_STRATEGY;
    public String CONTINUE_OR_ONCE_INV_MODE;
    public List<Integer> POTL_GEN2_Q;
    public int INV_TIME_OUT;
    public int PARAM_MAX_RSSI_CAN_SEND;
    public int REGION_CERTIFICATION;
    public int PARAM_ENABLE_TAG_DUPLICATE_FILTER;
    public int version_code;
    public int PARAM_MAX_ANTS_COUNT;
    public int FREQUENCY_REGION;
    public int PARAM_HIGH_TEMPERATURE_ANT_POWER;
    public int PARAM_OPERATE_ANTS;
    public int PARAM_HIGH_TEMPERATURE_VALUE;
    public List<Integer> FREQUENCY_HOPTABLE;
    public int PARAM_LOWER_POWER_DBM;
    public List<Integer> PARAM_ANTS_GROUP;
    public int FAST_ID;
    public int PARAM_TAG_SENDER_TIMEOUT;
    public int OUTPUT_CUSTOM_EMULATE_KEY;

    public List<RfAntPower> RF_ANTPOWER;

    public String URM_ProtocolCfg_Gen2;
    public List<Integer> POTL_GEN2_TAGENCODING;
    public int INV_FIELD_FREQUENCE;
    public List<Integer> RF_MAXPOWER;
    public int INV_FIELD_PROTOCAL;
    public String version_name;
    public int PARAM_BATTERY_WARNING_2;
    public int PARAM_BATTERY_WARNING_1;
    public String MODULE_INFO;
    public int INV_INTERVAL;
    public int INV_QUICK_MODE;

    // Nested class for RF_ANTPOWER
    public static class RfAntPower {
        public String antid;
        public int readPower;
        public int writePower;
    }

    // ========== JSON PARSE ==========
    public static UhfParams fromJson(String json) {
        try {
            return new com.google.gson.Gson().fromJson(json, UhfParams.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // ========== JSON STRINGIFY ==========
    public String toJson() {
        try {
            return new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(this);
        } catch (Exception e) {
            e.printStackTrace();
            return "{}";
        }
    }
}
