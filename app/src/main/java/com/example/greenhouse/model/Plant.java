package com.example.greenhouse.model;

import com.google.firebase.firestore.DocumentId;

public class Plant {
    @DocumentId
    private String id;
    private String plantName;
    private String description;
    private String imageUrl;
    private int humidityMin;
    private int humidityMax;
    private String category;

    // Required empty constructor for Firestore
    public Plant() {}

    public Plant(String id, String plantName, String description, String imageUrl, int humidityMin, int humidityMax, String category) {
        this.id = id;
        this.plantName = plantName;
        this.description = description;
        this.imageUrl = imageUrl;
        this.humidityMin = humidityMin;
        this.humidityMax = humidityMax;
        this.category = category;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPlantName() { return plantName; }
    public void setPlantName(String plantName) { this.plantName = plantName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public int getHumidityMin() { return humidityMin; }
    public void setHumidityMin(int humidityMin) { this.humidityMin = humidityMin; }

    public int getHumidityMax() { return humidityMax; }
    public void setHumidityMax(int humidityMax) { this.humidityMax = humidityMax; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
}
