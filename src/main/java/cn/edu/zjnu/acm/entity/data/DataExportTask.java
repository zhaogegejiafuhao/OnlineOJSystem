package cn.edu.zjnu.acm.entity.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import javax.persistence.*;
import java.time.Instant;

@Entity
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DataExportTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 50)
    private String type; // problems, users, solutions, contests
    
    @Column(nullable = false, length = 20)
    private String format; // csv, json, xlsx
    
    @Column(nullable = false, length = 50)
    private String status = "PENDING"; // PENDING, PROCESSING, COMPLETED, FAILED
    
    @Column(columnDefinition = "TEXT")
    private String parameters; // 导出参数
    
    // 文件路径可能超过 255
    @Column(columnDefinition = "TEXT")
    private String filePath; // 导出文件路径
    
    @Column
    private Long fileSize; // 文件大小（字节）
    
    @Column
    private Integer downloadCount = 0; // 下载次数
    
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