package cn.edu.zjnu.acm.entity.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import javax.persistence.*;
import java.time.Instant;

@Entity
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BackupTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 50)
    private String type; // full, database, config, media
    
    @Column(nullable = false, length = 50)
    private String status = "PENDING"; // PENDING, PROCESSING, COMPLETED, FAILED
    
    // 存储路径可能超过 255
    @Column(columnDefinition = "TEXT")
    private String storageLocation; // 存储位置
    
    @Column
    private Long fileSize; // 文件大小（字节）
    
    @Column
    private Boolean encrypted = false; // 是否加密
    
    @Column
    private Boolean verified = false; // 是否验证
    
    @Column(length = 100)
    private String version; // 版本信息
    
    @Column(columnDefinition = "TEXT")
    private String parameters; // 备份参数
    
    @Column(columnDefinition = "TEXT")
    private String errorMessage; // 错误信息
    
    @Column
    private Long userId; // 操作用户ID
    
    @Column(nullable = false)
    private Instant createTime = Instant.now(); // 创建时间
    
    @Column
    private Instant startTime; // 开始时间
    
    @Column
    private Instant completeTime; // 完成时间
    
    @Column
    private Double duration; // 执行时长（秒）
}