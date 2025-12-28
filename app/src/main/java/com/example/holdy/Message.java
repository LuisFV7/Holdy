package com.example.holdy;


public class Message {
    private String text;
    private int viewType; // 0 = Usuario, 1 = Bot

    public Message(String text, int viewType) {
        this.text = text;
        this.viewType = viewType;
    }

    public String getText() { return text; }
    public int getViewType() { return viewType; }
}