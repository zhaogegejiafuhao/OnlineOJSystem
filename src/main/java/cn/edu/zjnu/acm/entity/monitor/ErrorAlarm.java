package cn.edu.zjnu.acm.entity.monitor;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import javax.persistence.*;
import java.time.Instant;

@Entity
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorAlarm {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    private Instant alarmTime = Instant.now();
    
    @Column(nullable = false, length = 100)
    private String alarmType;
    
    // 告警消息可能包含异常摘要/外部系统信息，长度不可控，避免 VARCHAR(250) 截断
    @Column(nullable = false, columnDefinition = "TEXT")
    private String alarmMessage;
    
    @Column(columnDefinition = "VARCHAR(50) DEFAULT 'PENDING'")
    private String status = "PENDING";
    
    @Column(columnDefinition = "VARCHAR(50) DEFAULT 'HIGH'")
    private String priority = "HIGH";
    
    @Column(columnDefinition = "TEXT")
    private String details;
    
    @Column(columnDefinition = "TEXT")
    private String solution;
    
    @Column(columnDefinition = "DATETIME")
    private Instant handleTime;
    
    @Column(columnDefinition = "VARCHAR(100)")
    private String handler;
    
    public ErrorAlarm() {
    }
    
    public ErrorAlarm(String alarmType, String alarmMessage) {
        this.alarmType = alarmType;
        this.alarmMessage = alarmMessage;
    }
    
    public ErrorAlarm(String alarmType, String alarmMessage, String priority) {
        this.alarmType = alarmType;
        this.alarmMessage = alarmMessage;
        this.priority = priority;
    }
}