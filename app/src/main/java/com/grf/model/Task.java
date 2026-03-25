package com.grf.model;


public class Task {

    public static final String TABLE_NAME = "TaskList";
    public static final String COL_TID = "id";
    public static final String COL_ID = "id";
    public static final String COL_TITLE = "title";
    public static final String COL_TAG_COUNT = "tagCount";
    public static final String COL_TAG_ID = "tagId";
    public static final String COL_BOX_ID = "boxId";
    public static final String COL_RSS_VALUE = "rssValue";
    public static final String COL_ZONE_ID = "zoneId";
    public static final String COL_MODULE_ID = "moduleId";
    public static final String COL_TAG_FOUND = "tagFound";
    public static final String COL_TAG_COMPLETE = "taskComplete";
    public static final String COL_DATE_TIME = "dateTime";
    public static final String COL_TOTE_ID = "toteId";
    public static final String COL_TRAY_ID = "trayeId";

    public Task(long id, String title, int tagCount, String tagId, int boxId,
                double rssValue, String zoneId, String moduleId, int tagFound, int taskComplete, String dateTime) {
        this.id = id;
        this.title = title;
        this.tagCount = tagCount;
        this.tagId = tagId;
        this.boxId = boxId;
        this.rssValue = rssValue;
        this.zoneId = zoneId;
        this.moduleId = moduleId;
        this.tagFound = tagFound;
        this.taskComplete = taskComplete;
        this.dateTime = dateTime;
    }

    private long id;          // INTEGER → long
    private String title;     // TEXT → String
    private int tagCount;     // INTEGER → int
    private int tagFound;     // INTEGER → int
    private int taskComplete;     // INTEGER → int
    private String tagId;     // TEXT → String
    private int boxId;     // TEXT → String
    private double rssValue;  // REAL → double
    private String zoneId;    // TEXT → String
    private String moduleId;  // TEXT → String

    public String getDateTime() {
        return dateTime;
    }

    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }

    private String dateTime;  // TEXT → String

    // getters / setters

    public int getTagFound() {
        return tagFound;
    }

    public void setTagFound(int tagFound) {
        this.tagFound = tagFound;
    }

    public int getTaskComplete() {
        return taskComplete;
    }

    public void setTaskComplete(int taskComplete) {
        this.taskComplete = taskComplete;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getTagCount() {
        return tagCount;
    }

    public void setTagCount(int tagCount) {
        this.tagCount = tagCount;
    }

    public String getTagId() {
        return tagId;
    }

    public void setTagId(String tagId) {
        this.tagId = tagId;
    }

    public int getBoxId() {
        return boxId;
    }

    public void setBoxId(int boxId) {
        this.boxId = boxId;
    }

    public double getRssValue() {
        return rssValue;
    }

    public void setRssValue(double rssValue) {
        this.rssValue = rssValue;
    }

    public String getZoneId() {
        return zoneId;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }

    public String getModuleId() {
        return moduleId;
    }

    public void setModuleId(String moduleId) {
        this.moduleId = moduleId;
    }

    public Task() {

    }


}
