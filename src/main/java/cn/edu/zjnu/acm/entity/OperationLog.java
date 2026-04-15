package cn.edu.zjnu.acm.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import javax.persistence.*;
import java.time.Instant;

@Data
@Entity
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OperationLog extends LogBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private Long userId;
    @Column(nullable = false, length = 50)
    private String username;
    @Column(nullable = false, length = 100)
    private String operation;
    // 资源路径/标识可能超过 255（例如长 URL）
    @Column(nullable = false, columnDefinition = "TEXT")
    private String resource;
    // 详情可能包含 JSON / 请求参数等，长度不可控，避免 VARCHAR(1000) 截断
    @Column(columnDefinition = "TEXT")
    private String details;
    @Column(nullable = false)
    private boolean success;
    // 错误信息可能包含异常摘要/堆栈，可能超过 255
    @Column(columnDefinition = "TEXT")
    private String errorMessage;
    @Column(nullable = false, columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    private Instant operationTime = Instant.now();

    public OperationLog() {
    }

    public OperationLog(Long userId, String username, String operation, String resource, String details, boolean success, String errorMessage) {
        this.userId = userId;
        this.username = username;
        this.operation = operation;
        this.resource = resource;
        this.details = details;
        this.success = success;
        this.errorMessage = errorMessage;
    }
}