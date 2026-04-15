package cn.edu.zjnu.acm.controller;

import cn.edu.zjnu.acm.entity.ai.AIGeneration;
import cn.edu.zjnu.acm.exception.ForbiddenException;
import cn.edu.zjnu.acm.exception.NeedLoginException;
import cn.edu.zjnu.acm.service.AIService;
import cn.edu.zjnu.acm.service.AIModelService;
import cn.edu.zjnu.acm.util.RestfulResult;
import lombok.Data;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/ai")
public class AIController {
    private final AIService aiService;
    private final AIModelService aiModelService;
    private final HttpSession session;
    
    public AIController(AIService aiService, AIModelService aiModelService, HttpSession session) {
        this.aiService = aiService;
        this.aiModelService = aiModelService;
        this.session = session;
    }
    
    // 生成题目
    @PostMapping("/generate/problem")
    public RestfulResult generateProblem(@RequestBody GenerateProblemRequest request) {
        checkPermission();
        AIGeneration generation = aiService.generateProblem(request.getKeywords(), request.getDifficulty());
        return new RestfulResult(200, "success", generation);
    }
    
    // 生成题目解析
    @PostMapping("/generate/analysis")
    public RestfulResult generateAnalysis(@RequestBody GenerateAnalysisRequest request) {
        checkPermission();
        AIGeneration generation = aiService.generateAnalysis(request.getProblemContent());
        return new RestfulResult(200, "success", generation);
    }
    
    // 生成解答
    @PostMapping("/generate/solution")
    public RestfulResult generateSolution(@RequestBody GenerateSolutionRequest request) {
        checkPermission();
        AIGeneration generation = aiService.generateSolution(request.getProblemContent(), request.getLanguage());
        return new RestfulResult(200, "success", generation);
    }
    
    // 获取生成记录
    @GetMapping("/generations")
    public RestfulResult getGenerations(@RequestParam(value = "page", defaultValue = "0") int page,
                                       @RequestParam(value = "size", defaultValue = "20") int size) {
        checkPermission();
        Page<AIGeneration> generations = aiService.getGenerations(page, size);
        return new RestfulResult(200, "success", generations);
    }
    
    // 获取统计信息
    @GetMapping("/stats")
    public RestfulResult getStats() {
        checkPermission();
        AIService.AIGenerationStats stats = aiService.getStats();
        return new RestfulResult(200, "success", stats);
    }
    
    // 辅助方法
    private void checkPermission() {
        // 检查用户权限
        // 这里应该实现具体的权限检查逻辑
        // 暂时简化处理
        Object user = session.getAttribute("currentUser");
        if (user == null) {
            throw new NeedLoginException();
        }
        // 这里应该检查用户是否为管理员或教师
        // 暂时假设所有登录用户都可以使用AI功能
    }
    
    // 批量生成题目
    @PostMapping("/batch/generate/problems")
    public RestfulResult batchGenerateProblems(@RequestBody BatchGenerateProblemsRequest request) {
        checkPermission();
        try {
            Object result = aiModelService.batchGenerateProblems(
                    request.getModelType(),
                    request.getKeywords(),
                    request.getCount(),
                    request.getDifficulty()
            );
            return new RestfulResult(200, "success", result);
        } catch (Exception e) {
            return new RestfulResult(500, "Batch generation failed: " + e.getMessage());
        }
    }
    
    // 生成测试数据
    @PostMapping("/generate/testdata")
    public RestfulResult generateTestData(@RequestBody GenerateTestDataRequest request) {
        checkPermission();
        try {
            Object result = aiModelService.generateTestData(
                    request.getModelType(),
                    request.getProblemDescription(),
                    request.getCaseCount()
            );
            return new RestfulResult(200, "success", result);
        } catch (Exception e) {
            return new RestfulResult(500, "Test data generation failed: " + e.getMessage());
        }
    }
    
    // 请求数据类
    @Data
    static class GenerateProblemRequest {
        private String keywords;
        private String difficulty;
    }
    
    @Data
    static class GenerateAnalysisRequest {
        private String problemContent;
    }
    
    @Data
    static class GenerateSolutionRequest {
        private String problemContent;
        private String language;
    }
    
    @Data
    static class BatchGenerateProblemsRequest {
        private String modelType; // doubao, mimo, openai
        private String keywords;
        private int count;
        private String difficulty;
    }
    
    @Data
    static class GenerateTestDataRequest {
        private String modelType; // doubao, mimo, openai
        private String problemDescription;
        private int caseCount;
    }
}