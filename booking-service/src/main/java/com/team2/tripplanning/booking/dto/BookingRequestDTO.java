package com.team2.tripplanning.booking.dto;

import com.team2.tripplanning.booking.model.BookingType;
import lombok.Data;

@Data
public class BookingRequestDTO {
    private BookingType type;
    private Double amount;
    private String providerName;
}