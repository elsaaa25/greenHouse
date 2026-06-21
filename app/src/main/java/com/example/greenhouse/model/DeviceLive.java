package com.example.greenhouse.model;

import com.google.firebase.database.IgnoreExtraProperties;
import com.google.firebase.database.PropertyName;

@IgnoreExtraProperties
public class DeviceLive {
    private int humidity;
    private boolean lampStatus;
    private boolean pumpStatus;
    private boolean online;

    public DeviceLive() {} // Wajib ada!

    @PropertyName("humidity")
    public int getHumidity() { return humidity; }

    @PropertyName("humidity")
    public void setHumidity(int humidity) { this.humidity = humidity; }

    @PropertyName("lampStatus")
    public boolean isLampStatus() { return lampStatus; }

    @PropertyName("lampStatus")
    public void setLampStatus(boolean lampStatus) { this.lampStatus = lampStatus; }

    @PropertyName("pumpStatus")
    public boolean isPumpStatus() { return pumpStatus; }

    @PropertyName("pumpStatus")
    public void setPumpStatus(boolean pumpStatus) { this.pumpStatus = pumpStatus; }

    @PropertyName("online")
    public boolean isOnline() { return online; }

    @PropertyName("online")
    public void setOnline(boolean online) { this.online = online; }
}