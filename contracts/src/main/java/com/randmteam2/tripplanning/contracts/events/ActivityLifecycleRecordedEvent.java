package com.randmteam2.tripplanning.contracts.events;
public record ActivityLifecycleRecordedEvent(Long activityId, Long itineraryId, String status) {}
