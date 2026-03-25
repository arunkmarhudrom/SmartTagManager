package com.grf.uhfmanager;

public interface ZebraTagReadListener {
    void onTagRead(String epc,int rssi);
}
