package cn.edu.zjnu.acm.entity.oj;

import cn.edu.zjnu.acm.entity.User;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import javax.persistence.*;
import java.time.Instant;

@Entity
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(optional = false)
    private User user;
    
    @ManyToOne(optional = false)
    private Problem problem;
    
    @ManyToOne
    private Solution solution;
    
    @ManyToOne
    private ErrorCategory category;
    
    @Column(length = 50)
    private String errorType;
    
    // 错误信息可能很长（尤其包含编译/运行错误摘要），避免 VARCHAR(250) 截断
    @Column(columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(nullable = false, columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    private Instant createTime = Instant.now();
    
    @Column(nullable = false, columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    private Instant lastReviewTime = Instant.now();
    
    @Column(nullable = false, columnDefinition = "INTEGER DEFAULT 0")
    private Integer reviewCount = 0;
    
    @Column(nullable = false, columnDefinition = "INTEGER DEFAULT 0")
    private Integer difficulty = 0;
    
    @Column(nullable = false, columnDefinition = "BIT(1) DEFAULT 0")
    private Boolean isMarked = false;
    
    @Column(nullable = false, columnDefinition = "BIT(1) DEFAULT 0")
    private Boolean isResolved = false;
    
    public ErrorRecord() {
    }
    
    public ErrorRecord(User user, Problem problem, Solution solution, String errorType) {
        this.user = user;
        this.problem = problem;
        this.solution = solution;
        this.errorType = errorType;
    }
    
    public ErrorRecord(User user, Problem problem, Solution solution, String errorType, String errorMessage) {
        this.user = user;
        this.problem = problem;
        this.solution = solution;
        this.errorType = errorType;
        this.errorMessage = errorMessage;
    }
}