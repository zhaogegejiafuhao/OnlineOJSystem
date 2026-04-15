package cn.edu.zjnu.acm.controller;

import cn.edu.zjnu.acm.entity.monitor.ErrorAlarm;
import cn.edu.zjnu.acm.entity.monitor.PerformanceLog;
import cn.edu.zjnu.acm.entity.monitor.SystemMonitor;
import cn.edu.zjnu.acm.exception.ForbiddenException;
import cn.edu.zjnu.acm.exception.NeedLoginException;
import cn.edu.zjnu.acm.exception.NotFoundException;
import cn.edu.zjnu.acm.service.MonitorService;
import cn.edu.zjnu.acm.util.RestfulResult;
import lombok.Data;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@RestController
@RequestMapping("/api/monitor")
public class MonitorController {
    private final MonitorService monitorService;
    private final HttpSession session;
    
    public MonitorController(MonitorService monitorService, HttpSession session) {
        this.monitorService = monitorService;
        this.session = session;
    }
    
    // 系统监控数据
    @GetMapping("/system")
    public RestfulResult getSystemMonitors(@RequestParam(value = "page", defaultValue = "0") int page,
                                          @RequestParam(value = "size", defaultValue = "20") int size) {
        checkAdminPermission();
        Page<SystemMonitor> monitors = monitorService.getSystemMonitors(page, size);
        return new RestfulResult(200, "success", monitors);
    }
    
    @GetMapping("/system/range")
    public RestfulResult getSystemMonitorsByTimeRange(@RequestParam(value = "hours", defaultValue = "24") int hours,
                                                     @RequestParam(value = "page", defaultValue = "0") int page,
                                                     @RequestParam(value = "size", defaultValue = "20") int size) {
        checkAdminPermission();
        Instant startTime = Instant.now().minus(hours, ChronoUnit.HOURS);
        Page<SystemMonitor> monitors = monitorService.getSystemMonitorsByTimeRange(startTime, page, size);
        return new RestfulResult(200, "success", monitors);
    }
    
    @GetMapping("/system/metrics")
    public RestfulResult getSystemMetrics() {
        checkAdminPermission();
        MonitorService.SystemMetrics metrics = monitorService.getSystemMetrics();
        return new RestfulResult(200, "success", metrics);
    }
    
    // 报警管理
    @GetMapping("/alarms")
    public RestfulResult getErrorAlarms(@RequestParam(value = "page", defaultValue = "0") int page,
                                       @RequestParam(value = "size", defaultValue = "20") int size) {
        checkAdminPermission();
        Page<ErrorAlarm> alarms = monitorService.getErrorAlarms(page, size);
        return new RestfulResult(200, "success", alarms);
    }
    
    @GetMapping("/alarms/pending")
    public RestfulResult getPendingErrorAlarms(@RequestParam(value = "page", defaultValue = "0") int page,
                                              @RequestParam(value = "size", defaultValue = "20") int size) {
        checkAdminPermission();
        Page<ErrorAlarm> alarms = monitorService.getPendingErrorAlarms(page, size);
        return new RestfulResult(200, "success", alarms);
    }
    
    @GetMapping("/alarms/high")
    public RestfulResult getHighPriorityErrorAlarms(@RequestParam(value = "page", defaultValue = "0") int page,
                                                  @RequestParam(value = "size", defaultValue = "20") int size) {
        checkAdminPermission();
        Page<ErrorAlarm> alarms = monitorService.getHighPriorityErrorAlarms(page, size);
        return new RestfulResult(200, "success", alarms);
    }
    
    @PostMapping("/alarms/handle/{id}")
    public RestfulResult handleAlarm(@PathVariable("id") Long id,
                                    @RequestBody HandleAlarmRequest request) {
        checkAdminPermission();
        ErrorAlarm alarm = monitorService.handleAlarm(id, request.getSolution(), request.getHandler());
        if (alarm == null) {
            throw new NotFoundException("Alarm not found");
        }
        return RestfulResult.successResult();
    }
    
    // 性能日志
    @GetMapping("/performance")
    public RestfulResult getPerformanceLogs(@RequestParam(value = "page", defaultValue = "0") int page,
                                           @RequestParam(value = "size", defaultValue = "20") int size) {
        checkAdminPermission();
        Page<PerformanceLog> logs = monitorService.getPerformanceLogs(page, size);
        return new RestfulResult(200, "success", logs);
    }
    
    @GetMapping("/performance/slow")
    public RestfulResult getSlowRequests(@RequestParam(value = "threshold", defaultValue = "5000") double threshold,
                                        @RequestParam(value = "page", defaultValue = "0") int page,
                                        @RequestParam(value = "size", defaultValue = "20") int size) {
        checkAdminPermission();
        Page<PerformanceLog> logs = monitorService.getSlowRequests(threshold, page, size);
        return new RestfulResult(200, "success", logs);
    }
    
    @GetMapping("/performance/error")
    public RestfulResult getErrorRequests(@RequestParam(value = "page", defaultValue = "0") int page,
                                         @RequestParam(value = "size", defaultValue = "20") int size) {
        checkAdminPermission();
        Page<PerformanceLog> logs = monitorService.getErrorRequests(page, size);
        return new RestfulResult(200, "success", logs);
    }
    
    // 辅助方法
    private void checkAdminPermission() {
        // 检查是否为管理员
        // 这里应该使用实际的权限检查逻辑
        // 暂时简化处理
        Object user = session.getAttribute("currentUser");
        if (user == null) {
            throw new NeedLoginException();
        }
        // 这里应该检查用户是否为管理员
        // 暂时假设所有登录用户都可以访问监控功能
    }
    
    // 请求数据类
    @Data
    static class HandleAlarmRequest {
        private String solution;
        private String handler;
    }
}