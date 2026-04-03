package com.daniphord.mahanga.Service;

import com.daniphord.mahanga.Model.Shop;
import com.daniphord.mahanga.Repositories.ShopRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
public class ShopService {

    private static final long FREE_PERIOD_DAYS = 32;

    @Autowired
    private ShopRepository shopRepository;

    // GET all shops
    public List<Shop> getAllShops() {
        return shopRepository.findAll();
    }

    public Optional<Shop> getShopById(Long id) {
        return shopRepository.findById(id);
    }

    public Shop saveShop(Shop shop) {
        return shopRepository.save(shop);
    }

    public boolean updatePaymentStatus(Long id, boolean paymentComplete) {
        Optional<Shop> shopOpt = shopRepository.findById(id);
        if (shopOpt.isEmpty()) {
            return false;
        }
        Shop shop = shopOpt.get();
        LocalDate today = LocalDate.now();
        shop.setPaymentComplete(paymentComplete);
        if (paymentComplete) {
            shop.setPaymentDate(today);
            shop.setPaymentExpiryDate(today.plusDays(31));
            shop.setActive(true);
        } else {
            shop.setPaymentDate(null);
            shop.setPaymentExpiryDate(null);
            shop.setActive(isWithinFreePeriod(shop, today));
        }
        shopRepository.save(shop);
        return true;
    }

    public Shop refreshBillingStatus(Shop shop) {
        if (shop == null) {
            return null;
        }
        LocalDate today = LocalDate.now();
        boolean changed = false;

        if (isWithinFreePeriod(shop, today)) {
            if (!shop.isActive()) {
                shop.setActive(true);
                changed = true;
            }
        } else if (shop.isPaymentComplete() && shop.getPaymentExpiryDate() != null) {
            if (today.isAfter(shop.getPaymentExpiryDate())) {
                shop.setPaymentComplete(false);
                shop.setActive(false);
                changed = true;
            } else if (!shop.isActive()) {
                shop.setActive(true);
                changed = true;
            }
        } else if (shop.isActive()) {
            shop.setActive(false);
            changed = true;
        }

        if (changed) {
            shop = shopRepository.save(shop);
        }
        return shop;
    }

    public Shop updatePaymentNumber(Long id, String paymentNumber) {
        Optional<Shop> shopOpt = shopRepository.findById(id);
        if (shopOpt.isEmpty()) {
            return null;
        }
        Shop shop = shopOpt.get();
        shop.setPaymentNumber(paymentNumber == null ? null : paymentNumber.trim());
        return shopRepository.save(shop);
    }

    public boolean isWithinFreePeriod(Shop shop, LocalDate today) {
        if (shop == null || shop.getCreatedDate() == null) {
            return false;
        }
        long days = ChronoUnit.DAYS.between(shop.getCreatedDate(), today);
        return days < FREE_PERIOD_DAYS;
    }

    public long freeDaysRemaining(Shop shop, LocalDate today) {
        if (!isWithinFreePeriod(shop, today)) {
            return 0;
        }
        long days = ChronoUnit.DAYS.between(shop.getCreatedDate(), today);
        return Math.max(0, FREE_PERIOD_DAYS - days);
    }

    public long paymentDaysRemaining(Shop shop, LocalDate today) {
        if (shop == null || shop.getPaymentExpiryDate() == null) {
            return 0;
        }
        long days = ChronoUnit.DAYS.between(today, shop.getPaymentExpiryDate());
        return Math.max(0, days);
    }

    // DELETE shop
    public boolean deleteShop(Long id) {
        if (!shopRepository.existsById(id)) return false;
        shopRepository.deleteById(id);
        return true;
    }
}
