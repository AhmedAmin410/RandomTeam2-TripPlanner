import java.io.Serializable;
package com.randmteam2.tripplanning.contracts.dto;
import java.util.List;
public record BatchItineraryRequest(List<Long> itineraryIds) implements Serializable {}
