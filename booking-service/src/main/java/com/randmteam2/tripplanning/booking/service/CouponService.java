package com.randmteam2.tripplanning.booking.service;

import com.randmteam2.tripplanning.booking.model.Coupon;
import com.randmteam2.tripplanning.booking.repository.CouponRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class CouponService {

    private final CouponRepository couponRepository;

    public CouponService(CouponRepository couponRepository) {
        this.couponRepository = couponRepository;
    }

    public List<Coupon> getAllCoupons() {
        return couponRepository.findAll();
    }

    public Coupon getCouponById(Long id) {
        return couponRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Coupon not found"));
    }

    public Coupon createCoupon(Coupon coupon) {
        return couponRepository.save(coupon);
    }

    public Coupon updateCoupon(Long id, Coupon coupon) {
        getCouponById(id);
        coupon.setId(id);
        return couponRepository.save(coupon);
    }

    public void deleteCoupon(Long id) {
        getCouponById(id);
        couponRepository.deleteById(id);
    }
}