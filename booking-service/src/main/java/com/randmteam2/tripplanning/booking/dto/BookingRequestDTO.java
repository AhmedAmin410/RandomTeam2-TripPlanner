package com.randmteam2.tripplanning.booking.dto;

import com.randmteam2.tripplanning.booking.model.BookingType;
import lombok.Data;

@Data
public class BookingRequestDTO {
    private BookingType type;
    private Double amount;
    private String providerName;

    public BookingType getType() {
        return type;
    }

    public void setType(BookingType type) {
        this.type = type;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }
}
