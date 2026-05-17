package com.randomteam2.tripplanning.destination.dto;

/**
 * Request body for S2-F4 PUT /api/destinations/{id}/status.
 * Example: {"status": "INACTIVE"}
 */
public class DestinationStatusRequest {

    private String status;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}