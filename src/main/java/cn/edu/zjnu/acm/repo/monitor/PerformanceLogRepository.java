package cn.edu.zjnu.acm.repo.monitor;

import cn.edu.zjnu.acm.entity.monitor.PerformanceLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface PerformanceLogRepository extends JpaRepository<PerformanceLog, Long> {
    
    Page<PerformanceLog> findByEndpointContainingOrderByLogTimeDesc(String endpoint, Pageable pageable);
    
    Page<PerformanceLog> findByStatusCodeGreaterThanOrderByLogTimeDesc(Integer statusCode, Pageable pageable);
    
    Page<PerformanceLog> findByLogTimeAfterOrderByLogTimeDesc(Instant startTime, Pageable pageable);
    
    @Query("SELECT pl FROM PerformanceLog pl WHERE pl.responseTime > :threshold ORDER BY pl.responseTime DESC")
    Page<PerformanceLog> findSlowRequests(@Param("threshold") Double threshold, Pageable pageable);
    
    @Query("SELECT AVG(pl.responseTime) FROM PerformanceLog pl WHERE pl.endpoint = :endpoint AND pl.logTime >= :startTime")
    Double getAverageResponseTimeByEndpoint(@Param("endpoint") String endpoint, @Param("startTime") Instant startTime);
    
    @Query("SELECT COUNT(pl) FROM PerformanceLog pl WHERE pl.statusCode >= 500 AND pl.logTime >= :startTime")
    Long countServerErrors(@Param("startTime") Instant startTime);
    
    @Query("SELECT COUNT(pl) FROM PerformanceLog pl WHERE pl.statusCode >= 400 AND pl.statusCode < 500 AND pl.logTime >= :startTime")
    Long countClientErrors(@Param("startTime") Instant startTime);
}