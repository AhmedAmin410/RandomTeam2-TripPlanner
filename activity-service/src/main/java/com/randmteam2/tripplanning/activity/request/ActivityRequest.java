package com.randmteam2.tripplanning.activity.request;

import com.randmteam2.tripplanning.activity.model.Activity;
import java.time.LocalDateTime;
import java.util.Map;

public class ActivityRequest {

    private String name;
    private Activity.Category category;
    private Double lat;
    private Double lon;
    private LocalDateTime scheduledTime;
    private Map<String, Object> metadata;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Activity.Category getCategory() { return category; }
    public void setCategory(Activity.Category category) { this.category = category; }

    public Double getLat() { return lat; }
    public void setLat(Double lat) { this.lat = lat; }

    public Double getLon() { return lon; }
    public void setLon(Double lon) { this.lon = lon; }

    public LocalDateTime getScheduledTime() { return scheduledTime; }
    public void setScheduledTime(LocalDateTime scheduledTime) { this.scheduledTime = scheduledTime; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
}