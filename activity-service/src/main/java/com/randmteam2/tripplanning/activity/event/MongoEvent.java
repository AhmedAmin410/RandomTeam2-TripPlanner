package com.randmteam2.tripplanning.activity.event;
import java.time.LocalDateTime;
public interface MongoEvent { String getId(); LocalDateTime getTimestamp(); String getAction(); String getDetails(); }
