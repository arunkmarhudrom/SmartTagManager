package com.grf.model;

public class Tag {
    public static final String TABLE_NAME = "Tag";

    public static final String COL_ID = "Id";
    public static final String COL_SL_NO = "SlNo";
    public static final String COL_TOTE_BARCODE = "Tote_Barcode";
    public static final String COL_TAG_CODE = "TagCode";
    public static final String COL_DATE_TIME = "DateTime";
    public static final String COL_ACTIVE = "Active";
    public static final String COL_ZONE = "Zone";
    public static final String COL_MODULE = "Module";

    private long id;
    private int slNo;
    private String toteBarcode;
    private String tagCode;
    private String dateTime;
    private int active;
    private int zone;
    private String module;

    public Tag() {}

    // Getters & setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public int getSlNo() { return slNo; }
    public void setSlNo(int slNo) { this.slNo = slNo; }

    public String getToteBarcode() { return toteBarcode; }
    public void setToteBarcode(String toteBarcode) { this.toteBarcode = toteBarcode; }

    public String getTagCode() { return tagCode; }
    public void setTagCode(String tagCode) { this.tagCode = tagCode; }

    public String getDateTime() { return dateTime; }
    public void setDateTime(String dateTime) { this.dateTime = dateTime; }

    public int getActive() { return active; }
    public void setActive(int active) { this.active = active; }

    public int getZone() { return zone; }
    public void setZone(int zone) { this.zone = zone; }

    public String getModule() { return module; }
    public void setModule(String module) { this.module = module; }
}
