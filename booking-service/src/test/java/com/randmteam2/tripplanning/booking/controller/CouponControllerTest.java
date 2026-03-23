package com.randmteam2.tripplanning.booking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.randmteam2.tripplanning.booking.model.Coupon;
import com.randmteam2.tripplanning.booking.model.DiscountType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class CouponControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private Coupon coupon;

    @BeforeEach
    public void setup() {
        coupon = new Coupon();
        coupon.setCode("SUMMER2026");
        coupon.setDiscountType(DiscountType.PERCENTAGE);
        coupon.setDiscountValue(15.0);
        coupon.setMaxUses(100);
        coupon.setExpiryDate(LocalDateTime.now().plusDays(30));
        coupon.setActive(true);
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("category", "summer");
        coupon.setMetadata(metadata);
    }

    @Test
    public void testCreateCoupon() throws Exception {
        mockMvc.perform(post("/api/coupons")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(coupon)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is("SUMMER2026")))
                .andExpect(jsonPath("$.discountValue", is(15.0)));
    }

    @Test
    public void testGetAllCoupons() throws Exception {
        mockMvc.perform(post("/api/coupons")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(coupon)));

        mockMvc.perform(get("/api/coupons"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].code", is("SUMMER2026")));
    }

    @Test
    public void testGetCouponById() throws Exception {
        String response = mockMvc.perform(post("/api/coupons")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(coupon)))
                .andReturn().getResponse().getContentAsString();
        
        Coupon createdCoupon = objectMapper.readValue(response, Coupon.class);

        mockMvc.perform(get("/api/coupons/{id}", createdCoupon.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(createdCoupon.getId().intValue())));
    }

    @Test
    public void testUpdateCoupon() throws Exception {
        String response = mockMvc.perform(post("/api/coupons")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(coupon)))
                .andReturn().getResponse().getContentAsString();

        Coupon createdCoupon = objectMapper.readValue(response, Coupon.class);
        createdCoupon.setDiscountValue(20.0);

        mockMvc.perform(put("/api/coupons/{id}", createdCoupon.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createdCoupon)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.discountValue", is(20.0)));
    }

    @Test
    public void testDeleteCoupon() throws Exception {
        String response = mockMvc.perform(post("/api/coupons")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(coupon)))
                .andReturn().getResponse().getContentAsString();

        Coupon createdCoupon = objectMapper.readValue(response, Coupon.class);

        mockMvc.perform(delete("/api/coupons/{id}", createdCoupon.getId()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/coupons/{id}", createdCoupon.getId()))
                .andExpect(status().isNotFound());
    }
}