package com.randmteam2.tripplanning.activity.request;

public class BatchActivityResponse {

    private int count;

    public BatchActivityResponse(int count) {
        this.count = count;
    }

    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }
}