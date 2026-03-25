package com.grf.model;

public class ModuleItem {

    public String title; // like ZoneA  ,ZoneB etc
    public int count;// total count 5 , 20  etc
    public String subtitle;
    public int actionId;// ZoneID
    public int[] dotColors;   // <-- added

    // Constructor WITHOUT custom subtitle, WITH dot colors
    public ModuleItem(String title, int actionId,String subtitle, int[] dotColors, int count) {
        try {
            this.title = title;
            this.subtitle =subtitle;
            this.actionId = actionId;
            this.dotColors = dotColors;

            this.count = count;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


}
