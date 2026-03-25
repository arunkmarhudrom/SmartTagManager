package com.grf.model;

import androidx.annotation.NonNull;

public class TagItems {
    public final long id;
    @NonNull
    public final String name;
    public boolean selected;

    public TagItems(long id, @NonNull String name) {
        this.id = id;
        this.name = name;
        this.selected = false;
    }

    @NonNull
    @Override
    public String toString() {
        return name;
    }
}