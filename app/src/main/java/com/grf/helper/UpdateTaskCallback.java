package com.grf.helper;

public interface UpdateTaskCallback {
    void onSuccess(String message);
    void onError(String error);
}