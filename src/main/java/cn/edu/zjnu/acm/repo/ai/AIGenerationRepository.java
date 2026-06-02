package cn.edu.zjnu.acm.repo.ai;

import cn.edu.zjnu.acm.entity.ai.AIGeneration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface AIGenerationRepository extends JpaRepository<AIGeneration, Long> {
    
    Page<AIGeneration> findByTypeAndDeletedFalseOrderByCreateTimeDesc(String type, Pageable pageable);
    
    Page<AIGeneration> findByModelAndDeletedFalseOrderByCreateTimeDesc(String model, Pageable pageable);
    
    Page<AIGeneration> findByStatusAndDeletedFalseOrderByCreateTimeDesc(String status, Pageable pageable);
    
    @Query("SELECT ag FROM AIGeneration ag WHERE ag.createTime >= :startTime AND ag.deleted = false ORDER BY ag.createTime DESC")
    Page<AIGeneration> findRecentGenerations(@Param("startTime") Instant startTime, Pageable pageable);
    
    @Query("SELECT COUNT(ag) FROM AIGeneration ag WHERE ag.type = :type AND ag.status = 'COMPLETED' AND ag.deleted = false")
    Long countCompletedByType(@Param("type") String type);

    @Query("SELECT COUNT(ag) FROM AIGeneration ag WHERE ag.status = :status AND ag.deleted = false")
    Long countByStatus(@Param("status") String status);
    
    @Query("SELECT AVG(ag.responseTime) FROM AIGeneration ag WHERE ag.model = :model AND ag.status = 'COMPLETED' AND ag.deleted = false")
    Double getAverageResponseTimeByModel(@Param("model") String model);
    
    @Query("SELECT SUM(ag.cost) FROM AIGeneration ag WHERE ag.createTime >= :startTime AND ag.deleted = false")
    Double getTotalCost(@Param("startTime") Instant startTime);
    
    @Query("SELECT COUNT(ag) FROM AIGeneration ag WHERE ag.userId = :userId AND ag.createTime >= :startTime AND ag.deleted = false")
    Long countByUserIdAndCreateTimeAfter(@Param("userId") Long userId, @Param("startTime") Instant startTime);
    
    Page<AIGeneration> findByDeletedFalseOrderByCreateTimeDesc(Pageable pageable);
    
    @Modifying
    @Query("UPDATE AIGeneration SET deleted = true WHERE id = :id")
    void softDelete(@Param("id") Long id);
}
