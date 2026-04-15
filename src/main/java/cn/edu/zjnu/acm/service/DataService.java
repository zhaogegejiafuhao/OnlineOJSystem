package cn.edu.zjnu.acm.service;

import cn.edu.zjnu.acm.entity.data.DataExportTask;
import cn.edu.zjnu.acm.repo.data.DataExportTaskRepository;
import cn.edu.zjnu.acm.repo.problem.ProblemRepository;
import cn.edu.zjnu.acm.repo.user.UserRepository;
import cn.edu.zjnu.acm.repo.problem.SolutionRepository;
import cn.edu.zjnu.acm.repo.contest.ContestRepository;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

@Slf4j
@Service
public class DataService {
    private final DataExportTaskRepository dataExportTaskRepository;
    private final ProblemRepository problemRepository;
    private final UserRepository userRepository;
    private final SolutionRepository solutionRepository;
    private final ContestRepository contestRepository;
    private final ExecutorService executorService;
    
    public DataService(DataExportTaskRepository dataExportTaskRepository,
                      ProblemRepository problemRepository,
                      UserRepository userRepository,
                      SolutionRepository solutionRepository,
                      ContestRepository contestRepository) {
        this.dataExportTaskRepository = dataExportTaskRepository;
        this.problemRepository = problemRepository;
        this.userRepository = userRepository;
        this.solutionRepository = solutionRepository;
        this.contestRepository = contestRepository;
        this.executorService = Executors.newFixedThreadPool(5);
    }
    
    // 导出数据
    @Transactional
    public DataExportTask exportData(String type, String format, Map<String, Object> parameters, Long userId) {
        DataExportTask task = new DataExportTask();
        task.setType(type);
        task.setFormat(format);
        task.setStatus("PENDING");
        task.setParameters(JSON.toJSONString(parameters));
        task.setUserId(userId);
        task = dataExportTaskRepository.save(task);
        
        // 异步执行导出
        final DataExportTask finalTask = task;
        executorService.submit(() -> {
            try {
                executeExport(finalTask);
            } catch (Exception e) {
                log.error("Export failed: " + e.getMessage(), e);
                finalTask.setStatus("FAILED");
                finalTask.setErrorMessage(e.getMessage());
                dataExportTaskRepository.save(finalTask);
            }
        });
        
        return task;
    }
    
    // 执行导出
    private void executeExport(DataExportTask task) throws Exception {
        task.setStatus("PROCESSING");
        task.setStartTime(Instant.now());
        dataExportTaskRepository.save(task);
        
        long startTime = System.currentTimeMillis();
        
        // 创建导出目录
        String exportDir = "exports";
        File dir = new File(exportDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        String filename = String.format("%s_%s_%d.%s", task.getType(), task.getFormat(), System.currentTimeMillis(), task.getFormat());
        String filePath = exportDir + File.separator + filename;
        
        // 根据类型执行导出
        switch (task.getType()) {
            case "problems":
                exportProblems(task, filePath);
                break;
            case "users":
                exportUsers(task, filePath);
                break;
            case "solutions":
                exportSolutions(task, filePath);
                break;
            case "contests":
                exportContests(task, filePath);
                break;
            default:
                throw new IllegalArgumentException("Invalid export type: " + task.getType());
        }
        
        // 更新任务状态
        File exportFile = new File(filePath);
        task.setStatus("COMPLETED");
        task.setFilePath(filePath);
        task.setFileSize(exportFile.length());
        task.setCompleteTime(Instant.now());
        task.setDuration((System.currentTimeMillis() - startTime) / 1000.0);
        dataExportTaskRepository.save(task);
        
        log.info("Export completed: " + task.getType() + " to " + task.getFormat());
    }
    
    // 导出题目
    private void exportProblems(DataExportTask task, String filePath) throws Exception {
        List<?> problems = problemRepository.findAll();
        
        if ("json".equals(task.getFormat())) {
            try (FileWriter writer = new FileWriter(filePath)) {
                JSON.writeJSONString(writer, problems);
            }
        } else if ("csv".equals(task.getFormat())) {
            try (FileWriter writer = new FileWriter(filePath);
                 CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(
                         "id", "title", "timeLimit", "memoryLimit", "score", "status", "createTime"))) {
                for (Object problem : problems) {
                    // 这里需要根据实际的Problem类结构调整
                    csvPrinter.printRecord(
                            // 提取problem对象的字段
                    );
                }
            }
        } else if ("xlsx".equals(task.getFormat())) {
            try (Workbook workbook = new XSSFWorkbook();
                 FileOutputStream fos = new FileOutputStream(filePath)) {
                Sheet sheet = workbook.createSheet("Problems");
                // 创建表头
                Row headerRow = sheet.createRow(0);
                headerRow.createCell(0).setCellValue("ID");
                headerRow.createCell(1).setCellValue("Title");
                headerRow.createCell(2).setCellValue("Time Limit");
                headerRow.createCell(3).setCellValue("Memory Limit");
                headerRow.createCell(4).setCellValue("Score");
                headerRow.createCell(5).setCellValue("Status");
                headerRow.createCell(6).setCellValue("Create Time");
                
                // 填充数据
                int rowNum = 1;
                for (Object problem : problems) {
                    // 这里需要根据实际的Problem类结构调整
                    Row row = sheet.createRow(rowNum++);
                    // 填充单元格
                }
                
                workbook.write(fos);
            }
        }
    }
    
    // 导出用户
    private void exportUsers(DataExportTask task, String filePath) throws Exception {
        List<?> users = userRepository.findAll();
        
        if ("json".equals(task.getFormat())) {
            try (FileWriter writer = new FileWriter(filePath)) {
                JSON.writeJSONString(writer, users);
            }
        } else if ("csv".equals(task.getFormat())) {
            try (FileWriter writer = new FileWriter(filePath);
                 CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(
                         "id", "username", "email", "role", "createTime"))) {
                for (Object user : users) {
                    // 这里需要根据实际的User类结构调整
                    csvPrinter.printRecord(
                            // 提取user对象的字段
                    );
                }
            }
        } else if ("xlsx".equals(task.getFormat())) {
            try (Workbook workbook = new XSSFWorkbook();
                 FileOutputStream fos = new FileOutputStream(filePath)) {
                Sheet sheet = workbook.createSheet("Users");
                // 创建表头
                Row headerRow = sheet.createRow(0);
                headerRow.createCell(0).setCellValue("ID");
                headerRow.createCell(1).setCellValue("Username");
                headerRow.createCell(2).setCellValue("Email");
                headerRow.createCell(3).setCellValue("Role");
                headerRow.createCell(4).setCellValue("Create Time");
                
                // 填充数据
                int rowNum = 1;
                for (Object user : users) {
                    // 这里需要根据实际的User类结构调整
                    Row row = sheet.createRow(rowNum++);
                    // 填充单元格
                }
                
                workbook.write(fos);
            }
        }
    }
    
    // 导出提交记录
    private void exportSolutions(DataExportTask task, String filePath) throws Exception {
        List<?> solutions = solutionRepository.findAll();
        
        if ("json".equals(task.getFormat())) {
            try (FileWriter writer = new FileWriter(filePath)) {
                JSON.writeJSONString(writer, solutions);
            }
        } else if ("csv".equals(task.getFormat())) {
            try (FileWriter writer = new FileWriter(filePath);
                 CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(
                         "id", "userId", "problemId", "language", "result", "time", "memory", "createTime"))) {
                for (Object solution : solutions) {
                    // 这里需要根据实际的Solution类结构调整
                    csvPrinter.printRecord(
                            // 提取solution对象的字段
                    );
                }
            }
        } else if ("xlsx".equals(task.getFormat())) {
            try (Workbook workbook = new XSSFWorkbook();
                 FileOutputStream fos = new FileOutputStream(filePath)) {
                Sheet sheet = workbook.createSheet("Solutions");
                // 创建表头
                Row headerRow = sheet.createRow(0);
                headerRow.createCell(0).setCellValue("ID");
                headerRow.createCell(1).setCellValue("User ID");
                headerRow.createCell(2).setCellValue("Problem ID");
                headerRow.createCell(3).setCellValue("Language");
                headerRow.createCell(4).setCellValue("Result");
                headerRow.createCell(5).setCellValue("Time");
                headerRow.createCell(6).setCellValue("Memory");
                headerRow.createCell(7).setCellValue("Create Time");
                
                // 填充数据
                int rowNum = 1;
                for (Object solution : solutions) {
                    // 这里需要根据实际的Solution类结构调整
                    Row row = sheet.createRow(rowNum++);
                    // 填充单元格
                }
                
                workbook.write(fos);
            }
        }
    }
    
    // 导出竞赛
    private void exportContests(DataExportTask task, String filePath) throws Exception {
        List<?> contests = contestRepository.findAll();
        
        if ("json".equals(task.getFormat())) {
            try (FileWriter writer = new FileWriter(filePath)) {
                JSON.writeJSONString(writer, contests);
            }
        } else if ("csv".equals(task.getFormat())) {
            try (FileWriter writer = new FileWriter(filePath);
                 CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(
                         "id", "title", "creator", "startTime", "endTime", "status"))) {
                for (Object contest : contests) {
                    // 这里需要根据实际的Contest类结构调整
                    csvPrinter.printRecord(
                            // 提取contest对象的字段
                    );
                }
            }
        } else if ("xlsx".equals(task.getFormat())) {
            try (Workbook workbook = new XSSFWorkbook();
                 FileOutputStream fos = new FileOutputStream(filePath)) {
                Sheet sheet = workbook.createSheet("Contests");
                // 创建表头
                Row headerRow = sheet.createRow(0);
                headerRow.createCell(0).setCellValue("ID");
                headerRow.createCell(1).setCellValue("Title");
                headerRow.createCell(2).setCellValue("Creator");
                headerRow.createCell(3).setCellValue("Start Time");
                headerRow.createCell(4).setCellValue("End Time");
                headerRow.createCell(5).setCellValue("Status");
                
                // 填充数据
                int rowNum = 1;
                for (Object contest : contests) {
                    // 这里需要根据实际的Contest类结构调整
                    Row row = sheet.createRow(rowNum++);
                    // 填充单元格
                }
                
                workbook.write(fos);
            }
        }
    }
    
    // 获取导出任务
    public DataExportTask getExportTask(Long id) {
        return dataExportTaskRepository.findById(id).orElse(null);
    }
    
    // 获取导出任务列表
    public Page<DataExportTask> getExportTasks(int page, int size) {
        return dataExportTaskRepository.findAll(PageRequest.of(page, size));
    }
    
    // 增加下载次数
    public void incrementDownloadCount(Long id) {
        DataExportTask task = dataExportTaskRepository.findById(id).orElse(null);
        if (task != null) {
            task.setDownloadCount(task.getDownloadCount() + 1);
            dataExportTaskRepository.save(task);
        }
    }
    
    // 清理过期任务
    public void cleanupOldTasks(int days) {
        Instant cutoff = Instant.now().minusSeconds(days * 24 * 3600);
        // 这里可以实现清理逻辑
    }
}