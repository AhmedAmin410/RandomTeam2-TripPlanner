package com.randmteam2.tripplanning.booking.dto;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class BookingDtoBuilderTest {

    @Test
    void buildsUserBookingSummaryDto() {
        UserBookingSummaryDTO dto = UserBookingSummaryDTO.builder()
                .userId(68L)
                .totalBookings(3)
                .totalAmount(4500.0)
                .typeBreakdown(Map.of("HOTEL", 3000.0, "FLIGHT", 1500.0))
                .build();

        assertThat(dto.userId()).isEqualTo(68L);
        assertThat(dto.totalBookings()).isEqualTo(3);
        assertThat(dto.totalAmount()).isEqualTo(4500.0);
        assertThat(dto.typeBreakdown()).containsEntry("HOTEL", 3000.0);
    }

    @Test
    void buildsRevenueReportDto() {
        RevenueReportDTO dto = RevenueReportDTO.builder()
                .totalRevenue(9000.0)
                .totalBookings(6)
                .averageBookingAmount(1500.0)
                .cancelledAmount(1000.0)
                .cancelledCount(1)
                .build();

        assertThat(dto.totalRevenue()).isEqualTo(9000.0);
        assertThat(dto.totalBookings()).isEqualTo(6);
        assertThat(dto.averageBookingAmount()).isEqualTo(1500.0);
        assertThat(dto.cancelledAmount()).isEqualTo(1000.0);
        assertThat(dto.cancelledCount()).isEqualTo(1);
    }

    @Test
    void buildsBookingDetailsDto() {
        AppliedCouponDTO coupon = new AppliedCouponDTO("SAVE10", "PERCENTAGE", 100.0, null);

        BookingDetailsDTO dto = BookingDetailsDTO.builder()
                .bookingId(11L)
                .itineraryId(22L)
                .userId(33L)
                .originalAmount(1000.0)
                .type("ACCOMMODATION")
                .status("CONFIRMED")
                .bookingDetails(Map.of("providerName", "Hotel"))
                .appliedCoupons(List.of(coupon))
                .totalDiscount(100.0)
                .finalAmount(900.0)
                .build();

        assertThat(dto.bookingId()).isEqualTo(11L);
        assertThat(dto.itineraryId()).isEqualTo(22L);
        assertThat(dto.userId()).isEqualTo(33L);
        assertThat(dto.appliedCoupons()).containsExactly(coupon);
        assertThat(dto.finalAmount()).isEqualTo(900.0);
    }

    @Test
    void buildsCouponUsageDto() {
        CouponUsageDTO dto = CouponUsageDTO.builder()
                .couponId(44L)
                .code("SPRING")
                .discountType("FIXED")
                .discountValue(200.0)
                .timesUsed(5L)
                .totalDiscountGiven(1000.0)
                .active(true)
                .expired(false)
                .build();

        assertThat(dto.couponId()).isEqualTo(44L);
        assertThat(dto.code()).isEqualTo("SPRING");
        assertThat(dto.timesUsed()).isEqualTo(5L);
        assertThat(dto.active()).isTrue();
        assertThat(dto.expired()).isFalse();
    }
}
