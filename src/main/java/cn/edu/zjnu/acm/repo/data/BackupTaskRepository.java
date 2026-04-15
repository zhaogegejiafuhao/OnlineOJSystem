package cn.edu.zjnu.acm.repo.data;

import cn.edu.zjnu.acm.entity.data.BackupTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BackupTaskRepository extends JpaRepository<BackupTask, Long> {
}