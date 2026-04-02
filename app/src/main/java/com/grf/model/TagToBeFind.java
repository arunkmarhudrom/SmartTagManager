package com.grf.model;

public class TagToBeFind {

    public String trayId;
    public String rfId;
    public String statusName;
    public String findingTime;
    public String zoneName;

    public  TagToBeFind(String trayId, String rfId, String statusName, String findingTime, String zoneName) {
        try {
            this.trayId = trayId;
            this.rfId = rfId;
            this.statusName = statusName;
            this.findingTime = findingTime;
            this.zoneName = zoneName;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
