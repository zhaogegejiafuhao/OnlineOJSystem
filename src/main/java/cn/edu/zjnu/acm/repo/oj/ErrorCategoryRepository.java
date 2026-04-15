package cn.edu.zjnu.acm.repo.oj;

import cn.edu.zjnu.acm.entity.User;
import cn.edu.zjnu.acm.entity.oj.ErrorCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ErrorCategoryRepository extends JpaRepository<ErrorCategory, Long> {
    
    List<ErrorCategory> findByUserOrderByOrderIndexAsc(User user);
    
    List<ErrorCategory> findByUserAndNameOrderByOrderIndexAsc(User user, String name);
    
    @Query("SELECT ec FROM ErrorCategory ec WHERE ec.user = :user ORDER BY ec.orderIndex ASC")
    List<ErrorCategory> findAllByUserOrderByIndex(@Param("user") User user);
    
    @Query("SELECT COUNT(ec) FROM ErrorCategory ec WHERE ec.user = :user")
    Long countByUser(@Param("user") User user);
}