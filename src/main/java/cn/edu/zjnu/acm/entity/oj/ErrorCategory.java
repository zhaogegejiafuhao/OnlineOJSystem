package cn.edu.zjnu.acm.entity.oj;

import cn.edu.zjnu.acm.entity.User;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import javax.persistence.*;
import java.time.Instant;

@Entity
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 50)
    private String name;
    
    // 分类描述可能较长，避免 VARCHAR(250) 截断
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @ManyToOne
    private User user;
    
    @Column(nullable = false, columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    private Instant createTime = Instant.now();
    
    @Column(nullable = false, columnDefinition = "INTEGER DEFAULT 0")
    private Integer orderIndex = 0;
    
    public ErrorCategory() {
    }
    
    public ErrorCategory(String name, User user) {
        this.name = name;
        this.user = user;
    }
    
    public ErrorCategory(String name, String description, User user) {
        this.name = name;
        this.description = description;
        this.user = user;
    }
}