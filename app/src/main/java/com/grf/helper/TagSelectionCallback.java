package com.grf.helper;

import java.util.List;

public interface TagSelectionCallback {
    void onTagsSelected(List<Long> selectedTagIds);
}