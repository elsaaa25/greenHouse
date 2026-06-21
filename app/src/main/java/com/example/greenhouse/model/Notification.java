package com.example.greenhouse.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.PropertyName;

public class Notification {
    @DocumentId
    private String id;
    private String title;
    private String message;
    private String type;

    // Masalah utama ada di sini. Firestore mapping untuk boolean 'is...'
    // memerlukan penanganan khusus pada getter dan setter-nya.
    private boolean isRead;

    private Timestamp createdAt;
    private DocumentReference ownerId;
    private DocumentReference deviceId;

    // Konstruktor kosong wajib untuk Firestore
    public Notification() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    // PERBAIKAN PADA BOOLEAN:
    // Tambahkan @PropertyName pada Getter DAN Setter agar Firestore
    // secara eksplisit tahu field mana yang dimaksud.

    @PropertyName("isRead")
    public boolean isRead() {
        return isRead;
    }

    @PropertyName("isRead")
    public void setRead(boolean read) {
        this.isRead = read;
    }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public DocumentReference getOwnerId() { return ownerId; }
    public void setOwnerId(DocumentReference ownerId) { this.ownerId = ownerId; }

    public DocumentReference getDeviceId() { return deviceId; }
    public void setDeviceId(DocumentReference deviceId) { this.deviceId = deviceId; }
}