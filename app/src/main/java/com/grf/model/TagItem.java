package com.grf.model;



public class TagItem {
    private final String totId;
    private final String tagId;

    public TagItem(String totId, String tagId) {
        this.totId = totId;
        this.tagId = tagId;
    }

    public String getTotId() {
        return totId;
    }

    public String getTagId() {
        return tagId;
    }

    @Override
    public String toString() {
        // Useful fallback if adapter relies on toString()
        return tagId + " (TOT: " + totId + ")";
    }
}
