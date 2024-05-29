package com.example.memo;

public interface OnHttpCallback {
    void onSuccess(String userID);
    void onFailure(Exception e);
}
