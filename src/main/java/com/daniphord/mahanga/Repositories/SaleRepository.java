package com.daniphord.mahanga.Repositories;

import com.daniphord.mahanga.Model.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface SaleRepository extends JpaRepository<Sale, Long> {

    // 🔹 Find all sales for a specific shop
    @Query("SELECT s FROM Sale s WHERE s.shop.shopId = :shopId")
    List<Sale> findByShopId(Long shopId);

    // 🔹 Find all sales for a specific product
    @Query("SELECT s FROM Sale s WHERE s.product.id = :productId")
    List<Sale> findByProductId(Long productId);

    // 🔹 Find all sales for today
    @Query("SELECT s FROM Sale s WHERE s.dateTime >= :startOfDay AND s.dateTime <= :endOfDay")
    List<Sale> findTodaySales(LocalDateTime startOfDay, LocalDateTime endOfDay);
}
