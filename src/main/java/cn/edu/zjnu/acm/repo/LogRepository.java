package cn.edu.zjnu.acm.repo;

import cn.edu.zjnu.acm.entity.OperationLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LogRepository extends JpaRepository<OperationLog, Long> {

    List<OperationLog> findTop100ByOrderByOperationTimeDesc();

    List<OperationLog> findTop100ByUserIdOrderByOperationTimeDesc(Long userId);
}