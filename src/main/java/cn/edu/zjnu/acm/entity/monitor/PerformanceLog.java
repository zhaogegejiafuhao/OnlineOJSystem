package cn.edu.zjnu.acm.entity.monitor;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import javax.persistence.*;
import java.time.Instant;

@Entity
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PerformanceLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    private Instant logTime = Instant.now();
    
    @Column(nullable = false, length = 100)
    private String endpoint;
    
    @Column(columnDefinition = "DOUBLE DEFAULT 0")
    private Double responseTime = 0.0;
    
    @Column(columnDefinition = "INTEGER DEFAULT 200")
    private Integer statusCode = 200;
    
    @Column(columnDefinition = "VARCHAR(50) DEFAULT 'GET'")
    private String httpMethod = "GET";
    
    @Column(columnDefinition = "VARCHAR(250)")
    private String clientIp;
    
    // User-Agent 可能非常长（浏览器/爬虫），避免 VARCHAR(100) 截断
    @Column(columnDefinition = "TEXT")
    private String userAgent;
    
    @Column(columnDefinition = "INTEGER DEFAULT 0")
    private Integer requestSize = 0;
    
    @Column(columnDefinition = "INTEGER DEFAULT 0")
    private Integer responseSize = 0;
    
    public PerformanceLog() {
    }
    
    public PerformanceLog(String endpoint, Double responseTime, Integer statusCode) {
        this.endpoint = endpoint;
        this.responseTime = responseTime;
        this.statusCode = statusCode;
    }
    
    public PerformanceLog(String endpoint, Double responseTime, Integer statusCode, String httpMethod) {
        this.endpoint = endpoint;
        this.responseTime = responseTime;
        this.statusCode = statusCode;
        this.httpMethod = httpMethod;
    }
}