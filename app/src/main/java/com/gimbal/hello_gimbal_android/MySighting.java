package com.gimbal.hello_gimbal_android;

public class MySighting {
    private Integer rssi;
    private Long startTime;
    public MySighting(){
        this.rssi = null;
        this.startTime = null;
    }
    public MySighting(Integer rssi, Long startTime){
        this.rssi = rssi;
        this.startTime = startTime;
    }

    public Integer getRssi(){
        return this.rssi;
    }

    public Long getStartTime(){
        return this.startTime;
    }
}