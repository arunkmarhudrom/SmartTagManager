package com.grf.helper;

import com.grf.model.Task;

import java.util.List;

public interface TaskCallback {
    void onSuccess(List<Task> tasks);
    void onError(String error);
}