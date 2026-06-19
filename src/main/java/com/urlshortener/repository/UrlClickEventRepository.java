package com.urlshortener.repository;

import com.urlshortener.entity.UrlClickEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UrlClickEventRepository extends JpaRepository<UrlClickEvent, Long> {
    
    // Custom query to aggregate clicks per day for the last 7 days
    @Query(value = "SELECT DATE(clicked_at) as click_date, COUNT(*) as click_count " +
                   "FROM url_click_events " +
                   "WHERE clicked_at >= :since " +
                   "GROUP BY DATE(clicked_at) " +
                   "ORDER BY click_date ASC", nativeQuery = true)
    List<Object[]> countClicksPerDaySince(LocalDateTime since);
}
