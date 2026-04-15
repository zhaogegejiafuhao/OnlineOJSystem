package cn.edu.zjnu.acm.repo.data;

import cn.edu.zjnu.acm.entity.data.DataExportTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface DataExportTaskRepository extends JpaRepository<DataExportTask, Long> {
    
    Page<DataExportTask> findByStatusOrderByCreateTimeDesc(String status, Pageable pageable);
    
    Page<DataExportTask> findByTypeOrderByCreateTimeDesc(String type, Pageable pageable);
    
    Page<DataExportTask> findByUserIdOrderByCreateTimeDesc(Long userId, Pageable pageable);
    
    @Query("SELECT det FROM DataExportTask det WHERE det.createTime >= :startTime ORDER BY det.createTime DESC")
    Page<DataExportTask> findRecentTasks(@Param("startTime") Instant startTime, Pageable pageable);
    
    @Query("SELECT COUNT(det) FROM DataExportTask det WHERE det.status = 'COMPLETED'")
    Long countCompletedTasks();
    
    @Query("SELECT COUNT(det) FROM DataExportTask det WHERE det.status = 'FAILED'")
    Long countFailedTasks();
    
    @Query("SELECT SUM(det.fileSize) FROM DataExportTask det WHERE det.status = 'COMPLETED' AND det.createTime >= :startTime")
    Long getTotalExportSize(@Param("startTime") Instant startTime);
}