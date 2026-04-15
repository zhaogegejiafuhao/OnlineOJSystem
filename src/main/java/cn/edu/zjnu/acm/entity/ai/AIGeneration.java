package cn.edu.zjnu.acm.entity.ai;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import javax.persistence.*;
import java.time.Instant;

@Entity
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AIGeneration {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 100)
    private String type; // problem, analysis, solution, etc.
    
    @Column(nullable = false, length = 50)
    private String model;
    
    @Column(columnDefinition = "TEXT")
    private String prompt;
    
    @Column(columnDefinition = "TEXT")
    private String generatedContent;
    
    @Column(columnDefinition = "TEXT")
    private String evaluation;
    
    @Column(columnDefinition = "INTEGER DEFAULT 0")
    private Integer difficulty;
    
    @Column(columnDefinition = "VARCHAR(50) DEFAULT 'PENDING'")
    private String status = "PENDING";
    
    @Column(columnDefinition = "VARCHAR(50) DEFAULT 'LOW'")
    private String priority = "LOW";
    
    @Column(nullable = false, columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    private Instant createTime = Instant.now();
    
    @Column(columnDefinition = "DATETIME")
    private Instant completeTime;
    
    @Column(columnDefinition = "DOUBLE DEFAULT 0")
    private Double cost = 0.0;
    
    @Column(columnDefinition = "DOUBLE DEFAULT 0")
    private Double responseTime = 0.0;
    
    public AIGeneration() {
    }
    
    public AIGeneration(String type, String model, String prompt) {
        this.type = type;
        this.model = model;
        this.prompt = prompt;
    }
}