package cn.edu.zjnu.acm.entity.monitor;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import javax.persistence.*;
import java.time.Instant;

@Entity
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SystemMonitor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    private Instant monitorTime = Instant.now();
    
    @Column(columnDefinition = "DOUBLE DEFAULT 0")
    private Double cpuUsage = 0.0;
    
    @Column(columnDefinition = "DOUBLE DEFAULT 0")
    private Double memoryUsage = 0.0;
    
    @Column(columnDefinition = "DOUBLE DEFAULT 0")
    private Double diskUsage = 0.0;
    
    @Column(columnDefinition = "INTEGER DEFAULT 0")
    private Integer activeThreads = 0;
    
    @Column(columnDefinition = "INTEGER DEFAULT 0")
    private Integer httpRequests = 0;
    
    @Column(columnDefinition = "DOUBLE DEFAULT 0")
    private Double avgResponseTime = 0.0;
    
    @Column(columnDefinition = "INTEGER DEFAULT 0")
    private Integer errorCount = 0;
    
    @Column(columnDefinition = "VARCHAR(250) DEFAULT ''")
    private String status = "NORMAL";
    
    @Column(columnDefinition = "TEXT")
    private String details;
    
    public SystemMonitor() {
    }
    
    public SystemMonitor(Double cpuUsage, Double memoryUsage, Double diskUsage) {
        this.cpuUsage = cpuUsage;
        this.memoryUsage = memoryUsage;
        this.diskUsage = diskUsage;
    }
}