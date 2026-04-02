package com.randmteam2.tripplanning.booking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.randmteam2.tripplanning.booking.dto.BookingRequestDTO;
import com.randmteam2.tripplanning.booking.dto.RevenueReportDTO;
import com.randmteam2.tripplanning.booking.model.Booking;
import com.randmteam2.tripplanning.booking.model.BookingStatus;
import com.randmteam2.tripplanning.booking.model.BookingType;
import com.randmteam2.tripplanning.booking.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BookingService bookingService;

    private BookingRequestDTO requestDTO;
    private Booking booking;

    @BeforeEach
    public void setup() {
        requestDTO = new BookingRequestDTO();
        requestDTO.setType(BookingType.TRANSPORT);
        requestDTO.setAmount(500.0);
        requestDTO.setProviderName("Airline X");

        booking = new Booking();
        booking.setId(1L);
        booking.setItineraryId(10L);
        booking.setUserId(5L);
        booking.setAmount(500.0);
        booking.setType(BookingType.TRANSPORT);
        booking.setStatus(BookingStatus.PENDING);
    }

    @Test
    public void testCreateBookingForItinerary() throws Exception {
        Mockito.when(bookingService.createBooking(eq(10L), any(BookingRequestDTO.class)))
                .thenReturn(booking);

        mockMvc.perform(post("/api/bookings/itinerary/{itineraryId}", 10L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.amount").value(500.0));
    }

    @Test
    public void testRevenueReport() throws Exception {
        RevenueReportDTO report = new RevenueReportDTO(1500.0, 3L, 500.0, 100.0, 1L);
        Mockito.when(bookingService.getRevenueReport(any(), any()))
            .thenReturn(report);

        mockMvc.perform(get("/api/bookings/reports/revenue")
            .param("startDate", "2026-01-01T00:00:00")
            .param("endDate", "2026-12-31T23:59:59"))
                .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalRevenue").value(1500.0))
            .andExpect(jsonPath("$.totalBookings").value(3))
            .andExpect(jsonPath("$.averageBookingAmount").value(500.0))
            .andExpect(jsonPath("$.cancelledAmount").value(100.0))
            .andExpect(jsonPath("$.cancelledCount").value(1));
    }

            @Test
            public void testCancelBooking() throws Exception {
            Booking cancelledBooking = new Booking();
            cancelledBooking.setId(1L);
            cancelledBooking.setStatus(BookingStatus.CANCELLED);

            Mockito.when(bookingService.cancelBooking(eq(1L), eq("change of plans")))
                .thenReturn(cancelledBooking);

            Map<String, String> body = new HashMap<>();
            body.put("reason", "change of plans");

            mockMvc.perform(put("/api/bookings/{id}/cancel", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.status").value("CANCELLED"));
            }

}