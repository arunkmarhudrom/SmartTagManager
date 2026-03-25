package com.grf.model;

public class PendingTag {
    private final String tagId;
    private int signalPercent;      // 0..100
    private int stateCount;

    public int getRssi() {
        return Rssi;
    }

    public void setRssi(int rssi) {
        Rssi = rssi;
    }

    private int Rssi;

    public String getToteId() {
        return ToteId;
    }

    public void setToteId(String toteId) {
        ToteId = toteId;
    }

    public String getTrayId() {
        return TrayId;
    }

    public void setTrayId(String trayId) {
        TrayId = trayId;
    }

    private String ToteId;      // 0..100
    private String TrayId;      // 0..100


    public PendingTag(String tagId, int stateCount, int signalPercent,
                      String ToteId,String TrayId,int Rssi) {
        this.tagId = tagId;
        this.stateCount = stateCount;
        this.signalPercent = signalPercent;
        this.ToteId = ToteId;
        this.TrayId = TrayId;
        this.Rssi = Rssi;

    }


    private boolean showConfirm;

    public boolean isShowConfirm() {
        return showConfirm;
    }

    public void setShowConfirm(boolean val) {
        showConfirm = val;
    }

    public String getTagId() {
        return tagId;
    }

    public int getStateCount() {
        return stateCount;
    }

    public int getSignalPercent() {
        return signalPercent;
    }

    public void setSignalPercent(int signalPercent) {
        this.signalPercent = signalPercent;
    }
}
