package cn.edu.zjnu.acm.controller;

import cn.edu.zjnu.acm.entity.Teacher;
import cn.edu.zjnu.acm.entity.User;
import cn.edu.zjnu.acm.entity.ai.AIGeneration;
import cn.edu.zjnu.acm.exception.NeedLoginException;
import cn.edu.zjnu.acm.repo.user.TeacherRepository;
import cn.edu.zjnu.acm.service.AIService;
import cn.edu.zjnu.acm.util.RestfulResult;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/ai")
public class AIController {
    private final AIService aiService;
    private final TeacherRepository teacherRepository;
    private final HttpSession session;
    
    @Value("${ai.quota.admin-daily:100}")
    private int adminDailyQuota;
    
    public AIController(AIService aiService, TeacherRepository teacherRepository, HttpSession session) {
        this.aiService = aiService;
        this.teacherRepository = teacherRepository;
        this.session = session;
    }
    
    @PostMapping("/generate/problem")
    public RestfulResult generateProblem(@RequestBody GenerateProblemRequest request) {
        Long userId = checkAdminPermissionAndQuota();
        AIGeneration generation = aiService.generateProblem(request.getKeywords(), request.getDifficulty(), userId);
        return new RestfulResult(200, "success", generation);
    }
    
    @PostMapping("/generate/analysis")
    public RestfulResult generateAnalysis(@RequestBody GenerateAnalysisRequest request) {
        Long userId = checkAdminPermissionAndQuota();
        AIGeneration generation = aiService.generateAnalysis(request.getProblemContent(), userId);
        return new RestfulResult(200, "success", generation);
    }
    
    @PostMapping("/generate/solution")
    public RestfulResult generateSolution(@RequestBody GenerateSolutionRequest request) {
        Long userId = checkAdminPermissionAndQuota();
        AIGeneration generation = aiService.generateSolution(request.getProblemContent(), request.getLanguage(), userId);
        return new RestfulResult(200, "success", generation);
    }
    
    @GetMapping("/generations")
    public RestfulResult getGenerations(@RequestParam(value = "page", defaultValue = "0") int page,
                                       @RequestParam(value = "size", defaultValue = "20") int size) {
        checkAdminPermission();
        Page<AIGeneration> generations = aiService.getGenerations(page, size);
        return new RestfulResult(200, "success", generations);
    }
    
    @GetMapping("/generations/{id}")
    public RestfulResult getGenerationDetail(@PathVariable Long id) {
        checkAdminPermission();
        AIGeneration generation = aiService.getGenerationById(id).orElse(null);
        if (generation == null || (generation.getDeleted() != null && generation.getDeleted())) {
            return new RestfulResult(404, "生成记录不存在");
        }
        return new RestfulResult(200, "success", generation);
    }
    
    @DeleteMapping("/generations/{id}")
    public RestfulResult deleteGeneration(@PathVariable Long id) {
        checkAdminPermission();
        AIGeneration generation = aiService.getGenerationById(id).orElse(null);
        if (generation == null) {
            return new RestfulResult(404, "生成记录不存在");
        }
        aiService.softDeleteGeneration(id);
        return new RestfulResult(200, "success", "已删除");
    }
    
    @GetMapping("/stats")
    public RestfulResult getStats() {
        checkAdminPermission();
        AIService.AIGenerationStats stats = aiService.getStats();
        return new RestfulResult(200, "success", stats);
    }
    
    @GetMapping("/quota")
    public RestfulResult getQuota() {
        User user = checkAdminPermission();
        Map<String, Object> quotaInfo = getAdminQuotaInfo(user);
        return new RestfulResult(200, "success", quotaInfo);
    }
    
    @PostMapping("/batch/generate/problems")
    public RestfulResult batchGenerateProblems(@RequestBody BatchGenerateProblemsRequest request) {
        Long userId = checkAdminPermissionAndQuota();
        if (request.getCount() < 1 || request.getCount() > 10) {
            return new RestfulResult(400, "批量生成数量应在1-10之间");
        }
        try {
            java.util.List<Long> taskIds = new java.util.ArrayList<>();
            for (int i = 0; i < request.getCount(); i++) {
                AIGeneration generation = aiService.generateProblem(
                        request.getKeywords(), request.getDifficulty(), userId);
                taskIds.add(generation.getId());
            }
            Map<String, Object> result = new HashMap<>();
            result.put("taskIds", taskIds);
            result.put("count", request.getCount());
            result.put("message", "已提交" + request.getCount() + "个生成任务");
            return new RestfulResult(200, "success", result);
        } catch (Exception e) {
            return new RestfulResult(500, "批量生成失败: " + e.getMessage());
        }
    }
    
    private User checkAdminPermission() {
        User user = (User) session.getAttribute("currentUser");
        if (user == null) {
            throw new NeedLoginException();
        }
        Teacher teacher = teacherRepository.findByUser(user).orElse(null);
        if (teacher == null || teacher.getPrivilege() != Teacher.ADMIN) {
            throw new RuntimeException("仅管理员可访问AI助手");
        }
        return user;
    }
    
    private Long checkAdminPermissionAndQuota() {
        User user = checkAdminPermission();
        Map<String, Object> quotaInfo = getAdminQuotaInfo(user);
        int remaining = (int) quotaInfo.get("remaining");
        if (remaining <= 0) {
            throw new RuntimeException("今日AI使用配额已用完，配额: " + quotaInfo.get("quota") + "次/天");
        }
        return user.getId();
    }
    
    private Map<String, Object> getAdminQuotaInfo(User user) {
        Instant todayStart = Instant.now().truncatedTo(ChronoUnit.DAYS);
        Long used = aiService.getDailyUsageCount(user.getId(), todayStart);
        int remaining = adminDailyQuota - (used != null ? used.intValue() : 0);
        
        Map<String, Object> info = new HashMap<>();
        info.put("role", "admin");
        info.put("quota", adminDailyQuota);
        info.put("used", used != null ? used : 0);
        info.put("remaining", Math.max(0, remaining));
        return info;
    }
    
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
        private String keywords;
        private int count;
        private String difficulty;
    }
}
