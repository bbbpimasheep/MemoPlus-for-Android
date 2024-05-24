package com.example.memo;

public class Message {
    public static final int TYPE_SENT = 1;
    public static final int TYPE_RECEIVED = 2;

    private String content;
    private int type;

    public Message(String content, int type) {
        this.content = content;
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public int getType() {
        return type;
    }
}
