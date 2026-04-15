package cn.edu.zjnu.acm.repo.ai;

import cn.edu.zjnu.acm.entity.ai.AIGeneration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface AIGenerationRepository extends JpaRepository<AIGeneration, Long> {
    
    Page<AIGeneration> findByTypeOrderByCreateTimeDesc(String type, Pageable pageable);
    
    Page<AIGeneration> findByModelOrderByCreateTimeDesc(String model, Pageable pageable);
    
    Page<AIGeneration> findByStatusOrderByCreateTimeDesc(String status, Pageable pageable);
    
    @Query("SELECT ag FROM AIGeneration ag WHERE ag.createTime >= :startTime ORDER BY ag.createTime DESC")
    Page<AIGeneration> findRecentGenerations(@Param("startTime") Instant startTime, Pageable pageable);
    
    @Query("SELECT COUNT(ag) FROM AIGeneration ag WHERE ag.type = :type AND ag.status = 'COMPLETED'")
    Long countCompletedByType(@Param("type") String type);

    @Query("SELECT COUNT(ag) FROM AIGeneration ag WHERE ag.status = :status")
    Long countByStatus(@Param("status") String status);
    
    @Query("SELECT AVG(ag.responseTime) FROM AIGeneration ag WHERE ag.model = :model AND ag.status = 'COMPLETED'")
    Double getAverageResponseTimeByModel(@Param("model") String model);
    
    @Query("SELECT SUM(ag.cost) FROM AIGeneration ag WHERE ag.createTime >= :startTime")
    Double getTotalCost(@Param("startTime") Instant startTime);
}