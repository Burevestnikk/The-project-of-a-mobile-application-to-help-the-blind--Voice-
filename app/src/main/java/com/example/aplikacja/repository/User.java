package com.example.aplikacja.repository;
import com.google.gson.annotations.SerializedName;

public class User {
    public boolean statusToRequest;
    public int group;
    public String id;
    public String time;
    public double longitude;
    public double latitude;

    public User(boolean statusToRequest, int group, double longitude, double latitude, String id, String time) {
        this.statusToRequest = statusToRequest;
        this.group = group;
        this.longitude = longitude;
        this.latitude = latitude;
        this.id = id;
        this.time = time;
    }
}
