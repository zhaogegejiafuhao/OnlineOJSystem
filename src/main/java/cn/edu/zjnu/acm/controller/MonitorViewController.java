package cn.edu.zjnu.acm.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/monitor")
public class MonitorViewController {
    
    @GetMapping("/dashboard")
    public String monitorDashboard() {
        return "monitor/dashboard";
    }
    
    @GetMapping("/system")
    public String systemMonitor() {
        return "monitor/system";
    }
    
    @GetMapping("/alarms")
    public String alarms() {
        return "monitor/alarms";
    }
    
    @GetMapping("/performance")
    public String performance() {
        return "monitor/performance";
    }
    
    @GetMapping("/logs")
    public String logs() {
        return "monitor/logs";
    }
}