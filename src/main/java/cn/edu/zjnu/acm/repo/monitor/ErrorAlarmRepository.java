package cn.edu.zjnu.acm.repo.monitor;

import cn.edu.zjnu.acm.entity.monitor.ErrorAlarm;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface ErrorAlarmRepository extends JpaRepository<ErrorAlarm, Long> {
    
    Page<ErrorAlarm> findByStatusOrderByAlarmTimeDesc(String status, Pageable pageable);
    
    Page<ErrorAlarm> findByPriorityOrderByAlarmTimeDesc(String priority, Pageable pageable);
    
    Page<ErrorAlarm> findByAlarmTimeAfterOrderByAlarmTimeDesc(Instant startTime, Pageable pageable);
    
    @Query("SELECT COUNT(ea) FROM ErrorAlarm ea WHERE ea.status = 'PENDING'")
    Long countPendingAlarms();
    
    @Query("SELECT COUNT(ea) FROM ErrorAlarm ea WHERE ea.priority = 'HIGH' AND ea.status = 'PENDING'")
    Long countHighPriorityPendingAlarms();
    
    @Query("SELECT ea FROM ErrorAlarm ea WHERE ea.status = 'PENDING' ORDER BY " +
            "CASE WHEN ea.priority = 'HIGH' THEN 1 WHEN ea.priority = 'MEDIUM' THEN 2 ELSE 3 END, " +
            "ea.alarmTime DESC")
    Page<ErrorAlarm> findPendingAlarmsByPriority(Pageable pageable);
}