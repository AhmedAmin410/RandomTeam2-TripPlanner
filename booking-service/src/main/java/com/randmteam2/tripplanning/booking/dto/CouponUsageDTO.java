package com.randmteam2.tripplanning.booking.dto;

public record CouponUsageDTO(
        Long couponId,
        String code,
        String discountType,
        Double discountValue,
        Long timesUsed,
        Double totalDiscountGiven,
        Boolean active,
        Boolean expired
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long couponId;
        private String code;
        private String discountType;
        private Double discountValue;
        private Long timesUsed;
        private Double totalDiscountGiven;
        private Boolean active;
        private Boolean expired;

        public Builder couponId(Long couponId) {
            this.couponId = couponId;
            return this;
        }

        public Builder code(String code) {
            this.code = code;
            return this;
        }

        public Builder discountType(String discountType) {
            this.discountType = discountType;
            return this;
        }

        public Builder discountValue(Double discountValue) {
            this.discountValue = discountValue;
            return this;
        }

        public Builder timesUsed(Long timesUsed) {
            this.timesUsed = timesUsed;
            return this;
        }

        public Builder totalDiscountGiven(Double totalDiscountGiven) {
            this.totalDiscountGiven = totalDiscountGiven;
            return this;
        }

        public Builder active(Boolean active) {
            this.active = active;
            return this;
        }

        public Builder expired(Boolean expired) {
            this.expired = expired;
            return this;
        }

        public CouponUsageDTO build() {
            return new CouponUsageDTO(
                    couponId,
                    code,
                    discountType,
                    discountValue,
                    timesUsed,
                    totalDiscountGiven,
                    active,
                    expired
            );
        }
    }
}
