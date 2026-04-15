package cn.edu.zjnu.acm.service;

import cn.edu.zjnu.acm.entity.OperationLog;
import cn.edu.zjnu.acm.entity.User;
import cn.edu.zjnu.acm.repo.LogRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LogService {
    private final LogRepository logRepository;

    public LogService(LogRepository logRepository) {
        this.logRepository = logRepository;
    }

    public void logOperation(User user, String operation, String resource, String details, boolean success, String errorMessage) {
        OperationLog log = new OperationLog(
                user.getId(),
                user.getUsername(),
                operation,
                resource,
                details,
                success,
                errorMessage
        );
        logRepository.save(log);
    }

    public List<OperationLog> getRecentLogs(int limit) {
        return logRepository.findTop100ByOrderByOperationTimeDesc();
    }

    public List<OperationLog> getUserLogs(Long userId, int limit) {
        return logRepository.findTop100ByUserIdOrderByOperationTimeDesc(userId);
    }
}