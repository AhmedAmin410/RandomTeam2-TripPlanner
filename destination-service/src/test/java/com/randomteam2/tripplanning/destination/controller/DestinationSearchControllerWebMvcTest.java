package com.randomteam2.tripplanning.destination.controller;

import com.randomteam2.tripplanning.destination.exception.GlobalExceptionHandler;
import com.randomteam2.tripplanning.destination.exception.InvalidRatingRangeException;
import com.randomteam2.tripplanning.destination.exception.InvalidSearchParameterException;
import com.randomteam2.tripplanning.destination.model.Destination;
import com.randomteam2.tripplanning.destination.service.DestinationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DestinationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class DestinationSearchControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DestinationService destinationService;

    @Test
    void search_noParams_returns200() throws Exception {
        when(destinationService.searchDestinations(isNull(), isNull(), isNull())).thenReturn(List.of());

        mockMvc.perform(get("/api/destinations/search").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void search_withParams_passedToService() throws Exception {
        Destination d = new Destination();
        d.setId(1L);
        when(destinationService.searchDestinations(eq("BEACH"), eq(3.0), eq(5.0))).thenReturn(List.of(d));

        mockMvc.perform(get("/api/destinations/search")
                        .param("category", "BEACH")
                        .param("minRating", "3")
                        .param("maxRating", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void search_invalidRatingRange_returns400Body() throws Exception {
        when(destinationService.searchDestinations(isNull(), eq(4.0), eq(2.0)))
                .thenThrow(new InvalidRatingRangeException("minRating cannot be greater than maxRating"));

        mockMvc.perform(get("/api/destinations/search")
                        .param("minRating", "4")
                        .param("maxRating", "2"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("minRating cannot be greater than maxRating"));
    }

    @Test
    void search_invalidCategory_returns400() throws Exception {
        when(destinationService.searchDestinations(eq("NOT_REAL"), isNull(), isNull()))
                .thenThrow(new InvalidSearchParameterException("Invalid category"));

        mockMvc.perform(get("/api/destinations/search").param("category", "NOT_REAL"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid category"));
    }
}
