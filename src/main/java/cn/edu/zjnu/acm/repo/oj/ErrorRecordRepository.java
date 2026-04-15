package cn.edu.zjnu.acm.repo.oj;

import cn.edu.zjnu.acm.entity.User;
import cn.edu.zjnu.acm.entity.oj.ErrorCategory;
import cn.edu.zjnu.acm.entity.oj.ErrorRecord;
import cn.edu.zjnu.acm.entity.oj.Problem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ErrorRecordRepository extends JpaRepository<ErrorRecord, Long> {
    
    Page<ErrorRecord> findByUserOrderByCreateTimeDesc(User user, Pageable pageable);
    
    Page<ErrorRecord> findByUserAndCategoryOrderByCreateTimeDesc(User user, ErrorCategory category, Pageable pageable);
    
    Page<ErrorRecord> findByUserAndIsMarkedTrueOrderByCreateTimeDesc(User user, Pageable pageable);
    
    Page<ErrorRecord> findByUserAndIsResolvedFalseOrderByCreateTimeDesc(User user, Pageable pageable);
    
    List<ErrorRecord> findByUserAndProblem(User user, Problem problem);
    
    @Query("SELECT COUNT(er) FROM ErrorRecord er WHERE er.user = :user")
    Long countByUser(@Param("user") User user);
    
    @Query("SELECT COUNT(er) FROM ErrorRecord er WHERE er.user = :user AND er.isResolved = false")
    Long countUnresolvedByUser(@Param("user") User user);
    
    @Query("SELECT COUNT(er) FROM ErrorRecord er WHERE er.user = :user AND er.isMarked = true")
    Long countMarkedByUser(@Param("user") User user);
    
    @Query("SELECT er FROM ErrorRecord er WHERE er.user = :user ORDER BY er.reviewCount ASC, er.createTime DESC")
    Page<ErrorRecord> findByUserOrderByReviewCountAsc(User user, Pageable pageable);
    
    @Query("SELECT er FROM ErrorRecord er WHERE er.user = :user AND er.isResolved = false ORDER BY er.lastReviewTime ASC")
    Page<ErrorRecord> findUnresolvedByUserOrderByLastReviewTime(User user, Pageable pageable);
}