package com.example.greenhouse.model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.DocumentReference;
import java.util.List;

public class Device {
    @DocumentId
    private String id;
    private String deviceCode;
    private String deviceName;
    private String status;
    private List<DocumentReference> ownerIds;
    private DocumentReference plantUnitId;

    public Device() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDeviceCode() { return deviceCode; }
    public void setDeviceCode(String deviceCode) { this.deviceCode = deviceCode; }

    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<DocumentReference> getOwnerIds() { return ownerIds; }
    public void setOwnerIds(List<DocumentReference> ownerIds) { this.ownerIds = ownerIds; }

    public DocumentReference getPlantUnitId() { return plantUnitId; }
    public void setPlantUnitId(DocumentReference plantUnitId) { this.plantUnitId = plantUnitId; }
}
