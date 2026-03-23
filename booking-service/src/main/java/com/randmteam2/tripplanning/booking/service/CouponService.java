package com.randmteam2.tripplanning.booking.service;

import com.randmteam2.tripplanning.booking.model.Coupon;
import com.randmteam2.tripplanning.booking.repository.CouponRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CouponService {

    @Autowired
    private CouponRepository couponRepository;

    public List<Coupon> getAllCoupons() {
        return couponRepository.findAll();
    }

    public Optional<Coupon> getCouponById(Long id) {
        return couponRepository.findById(id);
    }

    public Optional<Coupon> getCouponByCode(String code) {
        return couponRepository.findByCode(code);
    }

    @Transactional
    public Coupon createCoupon(Coupon coupon) {
        return couponRepository.save(coupon);
    }

    @Transactional
    public Coupon updateCoupon(Long id, Coupon couponDetails) {
        return couponRepository.findById(id).map(coupon -> {
            coupon.setCode(couponDetails.getCode());
            coupon.setDiscountType(couponDetails.getDiscountType());
            coupon.setDiscountValue(couponDetails.getDiscountValue());
            coupon.setMaxUses(couponDetails.getMaxUses());
            coupon.setExpiryDate(couponDetails.getExpiryDate());
            coupon.setActive(couponDetails.getActive());
            coupon.setMetadata(couponDetails.getMetadata());
            return couponRepository.save(coupon);
        }).orElseThrow(() -> new RuntimeException("Coupon not found with id " + id));
    }

    @Transactional
    public void deleteCoupon(Long id) {
        couponRepository.deleteById(id);
    }
}
