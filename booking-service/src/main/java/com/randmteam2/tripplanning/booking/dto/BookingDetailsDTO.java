package com.randmteam2.tripplanning.booking.dto;

import java.util.List;
import java.util.Map;

public record BookingDetailsDTO(
        Long bookingId,
        Long itineraryId,
        Long userId,
        Double originalAmount,
        String type,
        String status,
        Map<String, Object> bookingDetails,
        List<AppliedCouponDTO> appliedCoupons,
        Double totalDiscount,
        Double finalAmount
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long bookingId;
        private Long itineraryId;
        private Long userId;
        private Double originalAmount;
        private String type;
        private String status;
        private Map<String, Object> bookingDetails;
        private List<AppliedCouponDTO> appliedCoupons;
        private Double totalDiscount;
        private Double finalAmount;

        public Builder bookingId(Long bookingId) {
            this.bookingId = bookingId;
            return this;
        }

        public Builder itineraryId(Long itineraryId) {
            this.itineraryId = itineraryId;
            return this;
        }

        public Builder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public Builder originalAmount(Double originalAmount) {
            this.originalAmount = originalAmount;
            return this;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public Builder bookingDetails(Map<String, Object> bookingDetails) {
            this.bookingDetails = bookingDetails;
            return this;
        }

        public Builder appliedCoupons(List<AppliedCouponDTO> appliedCoupons) {
            this.appliedCoupons = appliedCoupons;
            return this;
        }

        public Builder totalDiscount(Double totalDiscount) {
            this.totalDiscount = totalDiscount;
            return this;
        }

        public Builder finalAmount(Double finalAmount) {
            this.finalAmount = finalAmount;
            return this;
        }

        public BookingDetailsDTO build() {
            return new BookingDetailsDTO(
                    bookingId,
                    itineraryId,
                    userId,
                    originalAmount,
                    type,
                    status,
                    bookingDetails,
                    appliedCoupons,
                    totalDiscount,
                    finalAmount
            );
        }
    }
}
