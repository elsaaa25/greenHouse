package com.example.greenhouse.model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class UserPlant {
    @DocumentId
    private String id;
    private String displayName;
    private String location;
    private int humidityMin;
    private int humidityMax;
    private String lampMode;
    private String pumpMode;
    private DocumentReference deviceId;
    private DocumentReference ownerId;
    private DocumentReference plantId;
    @ServerTimestamp
    private Date createdAt;

    public UserPlant() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public int getHumidityMin() { return humidityMin; }
    public void setHumidityMin(int humidityMin) { this.humidityMin = humidityMin; }

    public int getHumidityMax() { return humidityMax; }
    public void setHumidityMax(int humidityMax) { this.humidityMax = humidityMax; }

    public String getLampMode() { return lampMode; }
    public void setLampMode(String lampMode) { this.lampMode = lampMode; }

    public String getPumpMode() { return pumpMode; }
    public void setPumpMode(String pumpMode) { this.pumpMode = pumpMode; }

    public DocumentReference getDeviceId() { return deviceId; }
    public void setDeviceId(DocumentReference deviceId) { this.deviceId = deviceId; }

    public DocumentReference getOwnerId() { return ownerId; }
    public void setOwnerId(DocumentReference ownerId) { this.ownerId = ownerId; }

    public DocumentReference getPlantId() { return plantId; }
    public void setPlantId(DocumentReference plantId) { this.plantId = plantId; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}
