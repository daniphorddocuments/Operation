package com.daniphord.mahanga.Repositories;

import com.daniphord.mahanga.Model.Shop;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShopRepository extends JpaRepository<Shop, Long> {
    boolean existsByName(String name);
}
