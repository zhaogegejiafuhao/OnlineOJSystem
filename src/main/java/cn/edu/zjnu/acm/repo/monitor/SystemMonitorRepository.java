package cn.edu.zjnu.acm.repo.monitor;

import cn.edu.zjnu.acm.entity.monitor.SystemMonitor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface SystemMonitorRepository extends JpaRepository<SystemMonitor, Long> {
    
    Page<SystemMonitor> findByMonitorTimeAfterOrderByMonitorTimeDesc(Instant startTime, Pageable pageable);
    
    @Query("SELECT sm FROM SystemMonitor sm ORDER BY sm.monitorTime DESC")
    Page<SystemMonitor> findLatestSystemMonitors(Pageable pageable);
    
    @Query("SELECT AVG(sm.cpuUsage) FROM SystemMonitor sm WHERE sm.monitorTime >= :startTime")
    Double getAverageCpuUsage(@Param("startTime") Instant startTime);
    
    @Query("SELECT AVG(sm.memoryUsage) FROM SystemMonitor sm WHERE sm.monitorTime >= :startTime")
    Double getAverageMemoryUsage(@Param("startTime") Instant startTime);
    
    @Query("SELECT AVG(sm.diskUsage) FROM SystemMonitor sm WHERE sm.monitorTime >= :startTime")
    Double getAverageDiskUsage(@Param("startTime") Instant startTime);
}