package com.randomteam2.tripplanning.destination.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.randomteam2.tripplanning.destination.exception.GlobalExceptionHandler;
import com.randomteam2.tripplanning.destination.exception.ResourceNotFoundException;
import com.randomteam2.tripplanning.destination.model.Destination;
import com.randomteam2.tripplanning.destination.model.DestinationReview;
import com.randomteam2.tripplanning.destination.model.DestinationReviewType;
import com.randomteam2.tripplanning.destination.service.DestinationReviewService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DestinationReviewController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class DestinationReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DestinationReviewService destinationReviewService;

    @Test
    void postCreate_returns201AndBody() throws Exception {
        Destination dest = new Destination();
        dest.setId(1L);
        DestinationReview saved = reviewEntity(10L, dest);
        when(destinationReviewService.createReview(eq(1L), any(DestinationReview.class))).thenReturn(saved);

        mockMvc.perform(post("/api/destinations/1/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "VISITOR",
                                  "content": "Great",
                                  "rating": 5,
                                  "visitDate": "2026-04-01"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.destinationId").value(1))
                .andExpect(jsonPath("$.rating").value(5));

        verify(destinationReviewService).createReview(eq(1L), any(DestinationReview.class));
    }

    @Test
    void postCreate_invalidRating_returns400() throws Exception {
        mockMvc.perform(post("/api/destinations/1/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "VISITOR",
                                  "content": "Great",
                                  "rating": 9
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void getById_notFound_returns404Json() throws Exception {
        when(destinationReviewService.getReviewById(99L)).thenThrow(new ResourceNotFoundException("Review not found: 99"));

        mockMvc.perform(get("/api/reviews/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void getAll_returnsArray() throws Exception {
        Destination dest = new Destination();
        dest.setId(2L);
        when(destinationReviewService.getAllReviews()).thenReturn(List.of(reviewEntity(1L, dest)));

        mockMvc.perform(get("/api/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].destinationId").value(2));
    }

    @Test
    void getByDestination_returnsList() throws Exception {
        Destination dest = new Destination();
        dest.setId(3L);
        when(destinationReviewService.getReviewsByDestination(3L)).thenReturn(List.of(reviewEntity(5L, dest)));

        mockMvc.perform(get("/api/destinations/3/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(5));
    }

    @Test
    void putUpdate_returns200() throws Exception {
        Destination dest = new Destination();
        dest.setId(1L);
        DestinationReview updated = reviewEntity(8L, dest);
        updated.setContent("Updated");
        when(destinationReviewService.updateReview(eq(8L), any(DestinationReview.class))).thenReturn(updated);

        mockMvc.perform(put("/api/reviews/8")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("content", "Updated"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Updated"));
    }

    @Test
    void delete_returns204() throws Exception {
        mockMvc.perform(delete("/api/reviews/8"))
                .andExpect(status().isNoContent());
        verify(destinationReviewService).deleteReview(8L);
    }

    private static DestinationReview reviewEntity(Long id, Destination destination) {
        DestinationReview r = new DestinationReview();
        r.setId(id);
        r.setDestination(destination);
        r.setType(DestinationReviewType.EXPERT);
        r.setContent("Ok");
        r.setRating(4);
        r.setVisitDate(LocalDate.of(2026, 2, 1));
        r.setVerified(false);
        r.setMetadata(Map.of("k", "v"));
        r.setCreatedAt(LocalDateTime.of(2026, 3, 1, 12, 0));
        return r;
    }
}
