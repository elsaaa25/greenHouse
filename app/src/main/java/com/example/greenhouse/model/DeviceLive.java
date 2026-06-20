package com.example.greenhouse.model;

public class DeviceLive {
    private int humidity;
    private boolean lampStatus;
    private long updatedAt;
    private boolean online;
    private boolean pumpStatus;

    public DeviceLive() {}

    public int getHumidity() { return humidity; }
    public void setHumidity(int humidity) { this.humidity = humidity; }

    public boolean isLampStatus() { return lampStatus; }
    public void setLampStatus(boolean lampStatus) { this.lampStatus = lampStatus; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public boolean isOnline() { return online; }
    public void setOnline(boolean online) { this.online = online; }

    public boolean isPumpStatus() { return pumpStatus; }
    public void setPumpStatus(boolean pumpStatus) { this.pumpStatus = pumpStatus; }
}
