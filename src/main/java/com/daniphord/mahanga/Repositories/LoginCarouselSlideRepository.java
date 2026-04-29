package com.daniphord.mahanga.Repositories;

import com.daniphord.mahanga.Model.LoginCarouselSlide;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoginCarouselSlideRepository extends JpaRepository<LoginCarouselSlide, Long> {

    List<LoginCarouselSlide> findAllByOrderByDisplayOrderAscIdAsc();

    List<LoginCarouselSlide> findByActiveTrueOrderByDisplayOrderAscIdAsc();

    List<LoginCarouselSlide> findByActiveTrueAndTargetPageOrderByDisplayOrderAscIdAsc(String targetPage);
}
