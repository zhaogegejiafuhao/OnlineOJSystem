package cn.edu.zjnu.acm.service;

import cn.edu.zjnu.acm.entity.monitor.ErrorAlarm;
import cn.edu.zjnu.acm.entity.monitor.PerformanceLog;
import cn.edu.zjnu.acm.entity.monitor.SystemMonitor;
import cn.edu.zjnu.acm.repo.monitor.ErrorAlarmRepository;
import cn.edu.zjnu.acm.repo.monitor.PerformanceLogRepository;
import cn.edu.zjnu.acm.repo.monitor.SystemMonitorRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class MonitorService {
    private final SystemMonitorRepository systemMonitorRepository;
    private final ErrorAlarmRepository errorAlarmRepository;
    private final PerformanceLogRepository performanceLogRepository;
    private final EmailService emailService;
    
    public MonitorService(SystemMonitorRepository systemMonitorRepository, 
                         ErrorAlarmRepository errorAlarmRepository, 
                         PerformanceLogRepository performanceLogRepository,
                         EmailService emailService) {
        this.systemMonitorRepository = systemMonitorRepository;
        this.errorAlarmRepository = errorAlarmRepository;
        this.performanceLogRepository = performanceLogRepository;
        this.emailService = emailService;
    }
    
    // 系统监控数据采集
    @Scheduled(fixedRate = 60000) // 每分钟采集一次
    @Transactional
    public void collectSystemMetrics() {
        try {
            SystemMonitor monitor = new SystemMonitor();
            
            // 采集真实系统指标
            monitor.setCpuUsage(getCpuUsage());
            monitor.setMemoryUsage(getMemoryUsage());
            monitor.setDiskUsage(getDiskUsage());
            monitor.setActiveThreads(ManagementFactory.getThreadMXBean().getThreadCount());
            
            // 保存监控数据
            systemMonitorRepository.save(monitor);
            
            // 检查阈值，触发报警
            checkSystemThresholds(monitor);
            
        } catch (Exception e) {
            log.error("Error collecting system metrics", e);
        }
    }
    
    // 获取真实CPU使用率
    private double getCpuUsage() {
        try {
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
                com.sun.management.OperatingSystemMXBean sunOsBean = (com.sun.management.OperatingSystemMXBean) osBean;
                double cpuLoad = sunOsBean.getSystemCpuLoad();
                // getSystemCpuLoad returns -1 if not available, or value between 0.0 and 1.0
                if (cpuLoad >= 0) {
                    return cpuLoad * 100;
                }
            }
            // Fallback: use system load average (not percentage, but better than random)
            // Or just return a reasonable default/error value
            return 0.0;
        } catch (Exception e) {
            log.warn("Failed to get CPU usage", e);
            return 0.0;
        }
    }
    
    // 获取真实内存使用率
    private double getMemoryUsage() {
        try {
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
                com.sun.management.OperatingSystemMXBean sunOsBean = (com.sun.management.OperatingSystemMXBean) osBean;
                long totalMem = sunOsBean.getTotalPhysicalMemorySize();
                long freeMem = sunOsBean.getFreePhysicalMemorySize();
                if (totalMem > 0) {
                    return (double)(totalMem - freeMem) / totalMem * 100;
                }
            }
            return 0.0;
        } catch (Exception e) {
            log.warn("Failed to get Memory usage", e);
            return 0.0;
        }
    }
    
    // 获取真实磁盘使用率
    private double getDiskUsage() {
        try {
            File root = new File(System.getProperty("os.name").toLowerCase().contains("win") ? "C:" : "/");
            long totalSpace = root.getTotalSpace();
            long freeSpace = root.getFreeSpace();
            if (totalSpace > 0) {
                return (double)(totalSpace - freeSpace) / totalSpace * 100;
            }
            return 0.0;
        } catch (Exception e) {
            log.warn("Failed to get Disk usage", e);
            return 0.0;
        }
    }
    
    // 检查系统阈值，触发报警
    private void checkSystemThresholds(SystemMonitor monitor) {
        if (monitor.getCpuUsage() > 80) {
            createAlarm("CPU_HIGH", "CPU使用率过高: " + monitor.getCpuUsage() + "%", "HIGH");
        }
        
        if (monitor.getMemoryUsage() > 85) {
            createAlarm("MEMORY_HIGH", "内存使用率过高: " + monitor.getMemoryUsage() + "%", "HIGH");
        }
        
        if (monitor.getDiskUsage() > 90) {
            createAlarm("DISK_HIGH", "磁盘使用率过高: " + monitor.getDiskUsage() + "%", "HIGH");
        }
    }
    
    // 创建报警
    @Transactional
    public ErrorAlarm createAlarm(String alarmType, String alarmMessage, String priority) {
        ErrorAlarm alarm = new ErrorAlarm(alarmType, alarmMessage, priority);
        ErrorAlarm savedAlarm = errorAlarmRepository.save(alarm);
        
        // 发送报警通知
        sendAlarmNotification(savedAlarm);
        
        return savedAlarm;
    }
    
    // 发送报警通知
    private void sendAlarmNotification(ErrorAlarm alarm) {
        try {
            String subject = "【系统报警】" + alarm.getAlarmType() + " - " + alarm.getPriority();
            String content = "报警时间: " + alarm.getAlarmTime() + "\n" +
                           "报警类型: " + alarm.getAlarmType() + "\n" +
                           "报警级别: " + alarm.getPriority() + "\n" +
                           "报警信息: " + alarm.getAlarmMessage() + "\n" +
                           "详细信息: " + (alarm.getDetails() != null ? alarm.getDetails() : "无");
            
            emailService.sendEmail("admin@example.com", subject, content);
        } catch (Exception e) {
            log.error("Error sending alarm notification", e);
        }
    }
    
    // 处理报警
    @Transactional
    public ErrorAlarm handleAlarm(Long id, String solution, String handler) {
        Optional<ErrorAlarm> optionalAlarm = errorAlarmRepository.findById(id);
        if (optionalAlarm.isPresent()) {
            ErrorAlarm alarm = optionalAlarm.get();
            alarm.setStatus("HANDLED");
            alarm.setSolution(solution);
            alarm.setHandler(handler);
            alarm.setHandleTime(Instant.now());
            return errorAlarmRepository.save(alarm);
        }
        return null;
    }
    
    // 性能日志管理
    @Transactional
    public void savePerformanceLog(PerformanceLog performanceLog) {
        performanceLogRepository.save(performanceLog);
        
        // 检查慢请求
        if (performanceLog.getResponseTime() > 5000) { // 5秒以上的请求
            createAlarm("SLOW_REQUEST", 
                       "慢请求: " + performanceLog.getEndpoint() + " - " + performanceLog.getResponseTime() + "ms", 
                       "MEDIUM");
        }
        
        // 检查错误请求
        if (performanceLog.getStatusCode() >= 500) {
            createAlarm("SERVER_ERROR", 
                       "服务器错误: " + performanceLog.getEndpoint() + " - " + performanceLog.getStatusCode(), 
                       "HIGH");
        }
    }
    
    // 系统监控数据查询
    public Page<SystemMonitor> getSystemMonitors(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return systemMonitorRepository.findLatestSystemMonitors(pageable);
    }
    
    public Page<SystemMonitor> getSystemMonitorsByTimeRange(Instant startTime, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return systemMonitorRepository.findByMonitorTimeAfterOrderByMonitorTimeDesc(startTime, pageable);
    }
    
    // 报警管理
    public Page<ErrorAlarm> getErrorAlarms(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return errorAlarmRepository.findAll(pageable);
    }
    
    public Page<ErrorAlarm> getPendingErrorAlarms(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return errorAlarmRepository.findByStatusOrderByAlarmTimeDesc("PENDING", pageable);
    }
    
    public Page<ErrorAlarm> getHighPriorityErrorAlarms(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return errorAlarmRepository.findByPriorityOrderByAlarmTimeDesc("HIGH", pageable);
    }
    
    // 性能日志查询
    public Page<PerformanceLog> getPerformanceLogs(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return performanceLogRepository.findAll(pageable);
    }
    
    public Page<PerformanceLog> getSlowRequests(double threshold, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return performanceLogRepository.findSlowRequests(threshold, pageable);
    }
    
    public Page<PerformanceLog> getErrorRequests(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return performanceLogRepository.findByStatusCodeGreaterThanOrderByLogTimeDesc(400, pageable);
    }
    
    // 统计信息
    public SystemMetrics getSystemMetrics() {
        Instant oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS);
        
        double avgCpu = systemMonitorRepository.getAverageCpuUsage(oneHourAgo);
        double avgMemory = systemMonitorRepository.getAverageMemoryUsage(oneHourAgo);
        double avgDisk = systemMonitorRepository.getAverageDiskUsage(oneHourAgo);
        
        long pendingAlarms = errorAlarmRepository.countPendingAlarms();
        long highPriorityAlarms = errorAlarmRepository.countHighPriorityPendingAlarms();
        
        long serverErrors = performanceLogRepository.countServerErrors(oneHourAgo);
        long clientErrors = performanceLogRepository.countClientErrors(oneHourAgo);
        
        return new SystemMetrics(avgCpu, avgMemory, avgDisk, pendingAlarms, highPriorityAlarms, serverErrors, clientErrors);
    }
    
    // 系统指标数据类
    @lombok.Data
    public static class SystemMetrics {
        private double avgCpuUsage;
        private double avgMemoryUsage;
        private double avgDiskUsage;
        private long pendingAlarms;
        private long highPriorityAlarms;
        private long serverErrors;
        private long clientErrors;
        
        public SystemMetrics(double avgCpuUsage, double avgMemoryUsage, double avgDiskUsage,
                           long pendingAlarms, long highPriorityAlarms,
                           long serverErrors, long clientErrors) {
            this.avgCpuUsage = avgCpuUsage;
            this.avgMemoryUsage = avgMemoryUsage;
            this.avgDiskUsage = avgDiskUsage;
            this.pendingAlarms = pendingAlarms;
            this.highPriorityAlarms = highPriorityAlarms;
            this.serverErrors = serverErrors;
            this.clientErrors = clientErrors;
        }
    }
}