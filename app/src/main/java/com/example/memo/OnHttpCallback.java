package com.example.memo;

public interface OnHttpCallback {
    void onSuccess(String feedBack);
    void onFailure(Exception e);
}
