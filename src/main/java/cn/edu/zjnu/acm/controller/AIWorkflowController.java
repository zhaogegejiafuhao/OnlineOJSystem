package cn.edu.zjnu.acm.controller;

import cn.edu.zjnu.acm.entity.Teacher;
import cn.edu.zjnu.acm.entity.User;
import cn.edu.zjnu.acm.entity.ai.AIGeneration;
import cn.edu.zjnu.acm.entity.oj.Problem;
import cn.edu.zjnu.acm.entity.oj.Tag;
import cn.edu.zjnu.acm.repo.user.TeacherRepository;
import cn.edu.zjnu.acm.exception.NeedLoginException;
import cn.edu.zjnu.acm.repo.problem.ProblemRepository;
import cn.edu.zjnu.acm.repo.problem.TagRepository;
import cn.edu.zjnu.acm.service.AIService;
import cn.edu.zjnu.acm.service.JudgeRunService;
import cn.edu.zjnu.acm.service.ProblemService;
import cn.edu.zjnu.acm.util.RestfulResult;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/ai/workflow")
public class AIWorkflowController {
    private static final Logger log = LoggerFactory.getLogger(AIWorkflowController.class);
    private final AIService aiService;
    private final ProblemRepository problemRepository;
    private final TagRepository tagRepository;
    private final ProblemService problemService;
    private final TeacherRepository teacherRepository;
    private final JudgeRunService judgeRunService;
    private final HttpSession session;
    
    @Value("${ai.quota.admin-daily:100}")
    private int adminDailyQuota;
    
    // Track async generation: key = asyncId (from initial call), value = AIGeneration id
    private final ConcurrentHashMap<Long, Long> asyncGenerationMap = new ConcurrentHashMap<>();
    
    private final ConcurrentHashMap<Long, String> asyncErrorMap = new ConcurrentHashMap<>();
    
    // Store test data preview for admin review before creating files
    private final ConcurrentHashMap<String, TestDataPreview> testDataPreviewMap = new ConcurrentHashMap<>();
    
    // Cleanup expired entries periodically (entries older than 30 minutes)
    private final long ENTRY_TTL_MS = 30 * 60 * 1000;
    
    public AIWorkflowController(AIService aiService, ProblemRepository problemRepository,
                                TagRepository tagRepository, ProblemService problemService,
                                TeacherRepository teacherRepository, JudgeRunService judgeRunService, HttpSession session) {
        this.aiService = aiService;
        this.problemRepository = problemRepository;
        this.tagRepository = tagRepository;
        this.problemService = problemService;
        this.teacherRepository = teacherRepository;
        this.judgeRunService = judgeRunService;
        this.session = session;
        
        // Start cleanup thread
        Thread cleanupThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(5 * 60 * 1000);
                    long now = System.currentTimeMillis();
                    asyncGenerationMap.entrySet().removeIf(e -> (now - e.getKey()) > ENTRY_TTL_MS);
                    asyncErrorMap.entrySet().removeIf(e -> (now - e.getKey()) > ENTRY_TTL_MS);
                    testDataPreviewMap.entrySet().removeIf(e -> (now - Long.parseLong(e.getKey())) > ENTRY_TTL_MS);
                    log.debug("Cleaned up expired entries. asyncMap={}, previewMap={}", 
                            asyncGenerationMap.size(), testDataPreviewMap.size());
                } catch (InterruptedException e) {
                    break;
                }
            }
        }, "AIWorkflow-Cleanup");
        cleanupThread.setDaemon(true);
        cleanupThread.start();
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
    
    private void checkAdminQuota(User user) {
        java.time.Instant todayStart = java.time.Instant.now().truncatedTo(java.time.temporal.ChronoUnit.DAYS);
        Long used = aiService.getDailyUsageCount(user.getId(), todayStart);
        int usedCount = used != null ? used.intValue() : 0;
        if (usedCount >= adminDailyQuota) {
            throw new RuntimeException("今日AI使用配额已用完，配额: " + adminDailyQuota + "次/天");
        }
    }
    
    // 1. Start async problem generation
    @PostMapping("/async-generate")
    public RestfulResult asyncGenerate(@RequestBody AsyncGenerateRequest request) {
        User user = checkAdminPermission();
        checkAdminQuota(user);
        Long userId = user.getId();
        Long asyncId = System.currentTimeMillis();
        
        CompletableFuture.runAsync(() -> {
            try {
                AIGeneration generation = aiService.generateProblem(request.getKeywords(), request.getDifficulty(), userId);
                asyncGenerationMap.put(asyncId, generation.getId());
                log.info("Async generation completed. asyncId={}, generationId={}, status={}", 
                        asyncId, generation.getId(), generation.getStatus());
            } catch (Exception e) {
                log.error("Async generation failed", e);
                asyncGenerationMap.put(asyncId, -1L);
                asyncErrorMap.put(asyncId, e.getMessage());
            }
        });
        
        return new RestfulResult(200, "success", asyncId);
    }
    
    // 2. Poll for async generation result
    @GetMapping("/async-result/{asyncId}")
    public RestfulResult getAsyncResult(@PathVariable Long asyncId) {
        checkAdminPermission();
        
        Long generationId = asyncGenerationMap.get(asyncId);
        if (generationId == null) {
            return new RestfulResult(404, "Generation record not found", Map.of("status", "NOT_FOUND"));
        }
        
        if (generationId == -1L) {
            String errorMsg = asyncErrorMap.getOrDefault(asyncId, "Generation failed");
            asyncErrorMap.remove(asyncId);
            return new RestfulResult(200, "success", Map.of(
                "status", "FAILED",
                "errorMessage", errorMsg,
                "generationId", asyncId
            ));
        }
        
        Optional<AIGeneration> genOpt = aiService.getGenerationById(generationId);
        if (!genOpt.isPresent()) {
            return new RestfulResult(404, "Generation record not found", Map.of("status", "NOT_FOUND"));
        }
        
        AIGeneration generation = genOpt.get();
        Map<String, Object> result = new HashMap<>();
        result.put("status", generation.getStatus());
        result.put("generationId", generationId);
        result.put("problemId", generationId); // AIGeneration ID serves as the "problem" reference for draft
        
        if ("FAILED".equals(generation.getStatus())) {
            result.put("errorMessage", generation.getEvaluation());
        }
        
        return new RestfulResult(200, "success", result);
    }
    
    // 3. Get draft problem by ID (returns the AIGeneration content parsed as a problem)
    @GetMapping("/draft-problem/{generationId}")
    public RestfulResult getDraftProblem(@PathVariable Long generationId) {
        checkAdminPermission();
        
        Optional<AIGeneration> genOpt = aiService.getGenerationById(generationId);
        if (!genOpt.isPresent()) {
            return new RestfulResult(404, "Generation record not found");
        }
        
        AIGeneration generation = genOpt.get();
        String content = generation.getGeneratedContent();
        
        // Parse the AI-generated content to extract problem fields
        Map<String, Object> parsed = parseProblemContent(content);
        
        Map<String, Object> draft = new HashMap<>();
        draft.put("id", generationId);
        draft.put("title", parsed.getOrDefault("title", "AI生成的题目"));
        draft.put("description", parsed.getOrDefault("description", ""));
        draft.put("input", parsed.getOrDefault("input", ""));
        draft.put("output", parsed.getOrDefault("output", ""));
        draft.put("sampleInput", parsed.getOrDefault("sampleInput", ""));
        draft.put("sampleOutput", parsed.getOrDefault("sampleOutput", ""));
        draft.put("hint", parsed.getOrDefault("hint", ""));
        draft.put("source", parsed.getOrDefault("source", "AI Generated"));
        draft.put("timeLimit", parsed.getOrDefault("timeLimit", 1000));
        draft.put("memoryLimit", parsed.getOrDefault("memoryLimit", 65536));
        draft.put("score", parsed.getOrDefault("score", 100));
        draft.put("tags", new ArrayList<>());
        
        Object solutionObj = parsed.get("solution");
        if (solutionObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> solutionMap = (Map<String, Object>) solutionObj;
            draft.put("solutionLanguage", solutionMap.getOrDefault("language", "cpp"));
            draft.put("solutionCode", solutionMap.getOrDefault("code", ""));
        } else {
            draft.put("solutionLanguage", "cpp");
            draft.put("solutionCode", "");
        }
        
        return new RestfulResult(200, "success", draft);
    }
    
    // 4. Edit draft (save as actual Problem entity)
    // Returns the created Problem with its real ID for subsequent operations
    @PostMapping("/edit-draft/{generationId}")
    public RestfulResult editDraft(@PathVariable Long generationId, @RequestBody EditDraftRequest request) {
        checkAdminPermission();
        
        // Check if a Problem was already created for this generation
        Problem problem = null;
        if (request.getProblemId() != null && request.getProblemId() > 0) {
            problem = problemRepository.findById(request.getProblemId()).orElse(null);
        }
        
        if (problem == null) {
            problem = new Problem();
            problem.setStatus(Problem.Status.DRAFT);
        }
        
        problem.setTitle(request.getTitle() != null ? request.getTitle() : "");
        problem.setDescription(request.getDescription() != null ? request.getDescription() : "");
        problem.setInput(request.getInput() != null ? request.getInput() : "");
        problem.setOutput(request.getOutput() != null ? request.getOutput() : "");
        problem.setSampleInput(request.getSampleInput() != null ? request.getSampleInput() : "");
        problem.setSampleOutput(request.getSampleOutput() != null ? request.getSampleOutput() : "");
        problem.setHint(request.getHint() != null ? request.getHint() : "");
        problem.setSource(request.getSource() != null ? request.getSource() : "");
        problem.setTimeLimit(request.getTimeLimit() != null ? request.getTimeLimit() : 1000);
        problem.setMemoryLimit(request.getMemoryLimit() != null ? request.getMemoryLimit() : 65536);
        problem.setScore(request.getScore() != null ? request.getScore() : 100);
        
        String baseTitle = problem.getTitle();
        if (baseTitle != null && !baseTitle.isEmpty()) {
            String uniqueTitle = ensureUniqueTitle(baseTitle, problem.getId());
            if (!uniqueTitle.equals(baseTitle)) {
                log.info("Title '{}' already exists, renamed to '{}'", baseTitle, uniqueTitle);
                problem.setTitle(uniqueTitle);
            }
        }
        
        // Handle tags
        if (request.getTags() != null && !request.getTags().isEmpty()) {
            List<Tag> tags = new ArrayList<>();
            for (String tagName : request.getTags().split(",")) {
                final String trimmedTagName = tagName.trim();
                if (trimmedTagName.isEmpty()) continue;
                final String finalTagName = trimmedTagName;
                Tag tag = tagRepository.findByName(finalTagName).orElseGet(() -> {
                    Tag newTag = new Tag();
                    newTag.setName(finalTagName);
                    return tagRepository.save(newTag);
                });
                tags.add(tag);
            }
            problem.setTags(tags);
        }
        
        problem = problemRepository.save(problem);
        
        // Return both generationId and problemId so frontend can track both
        Map<String, Object> result = new HashMap<>();
        result.put("problemId", problem.getId());
        result.put("generationId", generationId);
        result.put("problem", problem);
        
        return new RestfulResult(200, "success", result);
    }
    
    // 5. Submit problem for review
    @PostMapping("/submit-for-review/{problemId}")
    public RestfulResult submitForReview(@PathVariable Long problemId) {
        checkAdminPermission();
        Problem problem = problemRepository.findById(problemId).orElse(null);
        if (problem == null) {
            return new RestfulResult(404, "Problem not found");
        }
        
        problem.setStatus(Problem.Status.PENDING);
        problemRepository.save(problem);
        return new RestfulResult(200, "success", problem);
    }
    
    // 6. Generate test data - returns preview only, does NOT create files yet
    @PostMapping("/generate-testdata")
    public RestfulResult generateTestData(@RequestBody GenerateTestDataRequest request) {
        checkAdminPermission();
        
        Long problemId = request.getProblemId();
        
        Problem problem = problemRepository.findById(problemId).orElse(null);
        
        String problemDescription;
        String sampleInput = "";
        String sampleOutput = "";
        
        if (problem != null) {
            problemDescription = problem.getTitle() + "\n" + 
                                problem.getDescription() + "\n" + 
                                problem.getInput() + "\n" + 
                                problem.getOutput();
            sampleInput = problem.getSampleInput();
            sampleOutput = problem.getSampleOutput();
        } else {
            Optional<AIGeneration> genOpt = aiService.getGenerationById(problemId);
            if (!genOpt.isPresent()) {
                return new RestfulResult(404, "Problem or generation record not found");
            }
            AIGeneration generation = genOpt.get();
            Map<String, Object> parsed = parseProblemContent(generation.getGeneratedContent());
            problemDescription = String.valueOf(parsed.getOrDefault("description", "")) + "\n" +
                                String.valueOf(parsed.getOrDefault("input", "")) + "\n" +
                                String.valueOf(parsed.getOrDefault("output", ""));
            sampleInput = String.valueOf(parsed.getOrDefault("sampleInput", ""));
            sampleOutput = String.valueOf(parsed.getOrDefault("sampleOutput", ""));
        }
        
        int caseCount = request.getCaseCount();
        if (caseCount < 1) caseCount = 3;
        if (caseCount > 20) caseCount = 20;
        
        String prompt = "请为以下编程题目生成" + caseCount + "组测试输入数据：\n" +
                "题目描述：\n" + problemDescription + "\n";
        
        if (!sampleInput.isEmpty() && !sampleOutput.isEmpty()) {
            prompt += "参考样例输入：\n" + sampleInput + "\n参考样例输出：\n" + sampleOutput + "\n";
        }
        
        prompt += "\n" +
                "【重要】你必须严格按照以下JSON格式输出，不要输出任何JSON之外的内容：\n" +
                "```json\n" +
                "{\n" +
                "  \"testInputs\": [\n" +
                "    \"第一组输入数据\",\n" +
                "    \"第二组输入数据\"\n" +
                "  ]\n" +
                "}\n" +
                "```\n" +
                "\n" +
                "要求：\n" +
                "1. 只需要生成输入数据，不需要输出数据\n" +
                "2. 每组输入数据是一个纯文本字符串，必须包含实际的数据值\n" +
                "3. 第1组应为简单样例（与题目样例一致或类似）\n" +
                "4. 中间组覆盖常规情况和边界情况\n" +
                "5. 最后一组为较大数据规模的压力测试\n" +
                "6. 输入数据必须符合题目描述的输入格式\n" +
                "7. 【严禁】不要在JSON值中写代码（如join、range、for循环等），必须写出完整的数据\n" +
                "8. 【严禁】不要用Python/JS表达式，每个值必须是最终的纯文本数据\n" +
                "9. 【严禁】不要使用省略号（...或…）代替数据，每组数据必须是完整可运行的\n" +
                "10. 对于大数据量测试，控制规模在合理范围内（如n不超过1000），确保能写出完整数据\n" +
                "11. 只输出JSON，不要输出任何其他文字！";
        
        try {
            String aiResponse = aiService.callLLM(prompt);
            log.info("AI test data response received, length: {}", aiResponse != null ? aiResponse.length() : 0);
            log.info("AI raw response: {}", aiResponse != null && aiResponse.length() > 2000 ? aiResponse.substring(0, 2000) + "..." : aiResponse);
            
            if (aiResponse == null || aiResponse.trim().isEmpty()) {
                return new RestfulResult(500, "AI返回为空，请重试");
            }
            
            String jsonStr = extractJsonFromContent(aiResponse);
            if (jsonStr == null) {
                log.warn("AI response is not valid JSON. Response preview: {}", 
                        aiResponse.length() > 300 ? aiResponse.substring(0, 300) + "..." : aiResponse);
                return new RestfulResult(500, "AI返回格式不正确，无法解析测试数据。请减少用例数量或重试");
            }
            
            jsonStr = sanitizeTestDataJson(jsonStr);
            
            JSONObject jsonObj;
            try {
                jsonObj = JSON.parseObject(jsonStr);
            } catch (Exception parseEx) {
                log.error("JSON parse failed. Extracted JSON length: {}, error: {}", jsonStr.length(), parseEx.getMessage());
                log.error("Failed JSON content: {}", jsonStr.length() > 2000 ? jsonStr.substring(0, 2000) + "..." : jsonStr);
                return new RestfulResult(500, "AI返回的JSON格式有误，解析失败。请减少用例数量或重试");
            }
            JSONArray testInputs = jsonObj.getJSONArray("testInputs");
            if (testInputs == null || testInputs.isEmpty()) {
                JSONArray testCases = jsonObj.getJSONArray("testCases");
                if (testCases != null && !testCases.isEmpty()) {
                    testInputs = new JSONArray();
                    for (int i = 0; i < testCases.size(); i++) {
                        JSONObject tc = testCases.getJSONObject(i);
                        testInputs.add(tc.getString("input"));
                    }
                }
            }
            if (testInputs == null || testInputs.isEmpty()) {
                return new RestfulResult(500, "AI未返回有效的测试输入数据");
            }
            
            List<String> cleanedInputs = new ArrayList<>();
            for (int i = 0; i < testInputs.size(); i++) {
                String inputStr = testInputs.getString(i);
                if (containsCodeExpression(inputStr)) {
                    log.warn("Test input #{} contains code expression, skipping: {}", i + 1, 
                            inputStr.length() > 100 ? inputStr.substring(0, 100) + "..." : inputStr);
                    continue;
                }
                cleanedInputs.add(inputStr);
            }
            
            if (cleanedInputs.isEmpty()) {
                return new RestfulResult(500, "AI生成的测试数据全部包含代码表达式，请重新生成");
            }
            
            JSONArray finalInputs = new JSONArray();
            finalInputs.addAll(cleanedInputs);
            
            String previewId = String.valueOf(System.currentTimeMillis());
            testDataPreviewMap.put(previewId, new TestDataPreview(problemId, finalInputs));
            
            List<Map<String, Object>> previewList = new ArrayList<>();
            for (int i = 0; i < finalInputs.size(); i++) {
                String inputStr = finalInputs.getString(i);
                Map<String, Object> item = new HashMap<>();
                item.put("caseNum", i + 1);
                item.put("input", inputStr);
                item.put("output", "");
                previewList.add(item);
            }
            
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("previewId", previewId);
            resultData.put("problemId", problemId);
            resultData.put("count", finalInputs.size());
            resultData.put("testCases", previewList);
            resultData.put("message", "AI已生成" + finalInputs.size() + "组测试输入数据，请运行参考代码获取输出后确认");
            
            return new RestfulResult(200, "success", resultData);
            
        } catch (Exception e) {
            log.error("Test data generation failed", e);
            return new RestfulResult(500, "测试数据生成失败: " + e.getMessage());
        }
    }
    
    // 7. Confirm and create test data files after admin review
    @PostMapping("/confirm-testdata")
    public RestfulResult confirmTestData(@RequestBody ConfirmTestDataRequest request) {
        checkAdminPermission();
        
        Long problemId = request.getProblemId();
        String previewId = request.getPreviewId();
        JSONArray testInputs = null;
        
        if (previewId != null) {
            TestDataPreview preview = testDataPreviewMap.get(previewId);
            if (preview != null) {
                problemId = preview.getProblemId();
                testInputs = preview.getTestInputs();
            }
        }
        
        if (problemId == null) {
            return new RestfulResult(400, "缺少题目ID");
        }
        
        List<ConfirmTestDataRequest.TestCaseOutput> outputs = request.getOutputs();
        List<String> requestInputs = request.getInputs();
        
        if (testInputs == null && previewId != null && (requestInputs == null || requestInputs.isEmpty())) {
            return new RestfulResult(404, "预览数据不存在或已过期，请重新生成");
        }
        
        if (testInputs == null && requestInputs != null && !requestInputs.isEmpty()) {
            testInputs = new JSONArray();
            testInputs.addAll(requestInputs);
        }
        
        if (testInputs == null) {
            testInputs = new JSONArray();
            for (int i = 0; i < outputs.size(); i++) {
                testInputs.add("");
            }
        }
        
        try {
            String testDataBasePath = getTestDataBasePath();
            Path problemDir = Paths.get(testDataBasePath, String.valueOf(problemId));
            
            if (!Files.exists(problemDir)) {
                Files.createDirectories(problemDir);
                log.info("Created test data directory: {}", problemDir);
            }
            
            int writtenCount = 0;
            StringBuilder summary = new StringBuilder();
            int caseCount = Math.min(testInputs.size(), outputs != null ? outputs.size() : 0);
            
            for (int i = 0; i < caseCount; i++) {
                String inputData = testInputs.getString(i);
                String outputData = outputs.get(i).getOutput();
                
                if (inputData == null || outputData == null) continue;
                
                if (!inputData.endsWith("\n")) inputData += "\n";
                if (!outputData.endsWith("\n")) outputData += "\n";
                
                int caseNum = i + 1;
                Path inputFile = problemDir.resolve(caseNum + ".in");
                Path outputFile = problemDir.resolve(caseNum + ".out");
                
                Files.write(inputFile, inputData.getBytes(StandardCharsets.UTF_8));
                Files.write(outputFile, outputData.getBytes(StandardCharsets.UTF_8));
                
                writtenCount++;
                summary.append("测试用例").append(caseNum).append(": ")
                       .append(caseNum + ".in").append(" / ").append(caseNum + ".out")
                       .append(" (输入").append(inputData.length()).append("字节, 输出").append(outputData.length()).append("字节)\n");
            }
            
            testDataPreviewMap.remove(previewId);
            
            if (writtenCount == 0) {
                return new RestfulResult(500, "未能创建任何测试数据文件");
            }
            
            String resultMsg = "成功创建" + writtenCount + "组测试数据文件到 " + problemDir + "\n" + summary.toString();
            log.info(resultMsg);
            
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("message", resultMsg);
            resultData.put("count", writtenCount);
            resultData.put("path", problemDir.toString());
            
            return new RestfulResult(200, "success", resultData);
            
        } catch (Exception e) {
            log.error("Test data file creation failed", e);
            return new RestfulResult(500, "测试数据文件创建失败: " + e.getMessage());
        }
    }
    
    // 8. Read existing test data files for a problem
    @GetMapping("/testdata/{problemId}")
    public RestfulResult getTestData(@PathVariable Long problemId) {
        checkAdminPermission();
        try {
            String testDataBasePath = getTestDataBasePath();
            Path problemDir = Paths.get(testDataBasePath, String.valueOf(problemId));
            if (!Files.exists(problemDir) || !Files.isDirectory(problemDir)) {
                return new RestfulResult(404, "该题目暂无测试数据");
            }
            List<Map<String, Object>> testCases = new ArrayList<>();
            int caseNum = 1;
            while (true) {
                Path inputFile = problemDir.resolve(caseNum + ".in");
                Path outputFile = problemDir.resolve(caseNum + ".out");
                if (!Files.exists(inputFile)) break;
                Map<String, Object> tc = new HashMap<>();
                tc.put("caseNum", caseNum);
                tc.put("input", new String(Files.readAllBytes(inputFile), StandardCharsets.UTF_8));
                tc.put("output", new String(Files.readAllBytes(outputFile), StandardCharsets.UTF_8));
                testCases.add(tc);
                caseNum++;
            }
            Map<String, Object> result = new HashMap<>();
            result.put("problemId", problemId);
            result.put("count", testCases.size());
            result.put("testCases", testCases);
            return new RestfulResult(200, "success", result);
        } catch (Exception e) {
            log.error("Failed to read test data", e);
            return new RestfulResult(500, "读取测试数据失败: " + e.getMessage());
        }
    }
    
    // 9. Update a single test case
    @PutMapping("/testdata/{problemId}/{caseNum}")
    public RestfulResult updateTestData(@PathVariable Long problemId, @PathVariable int caseNum,
                                        @RequestBody UpdateTestDataRequest request) {
        checkAdminPermission();
        try {
            String testDataBasePath = getTestDataBasePath();
            Path problemDir = Paths.get(testDataBasePath, String.valueOf(problemId));
            Path inputFile = problemDir.resolve(caseNum + ".in");
            Path outputFile = problemDir.resolve(caseNum + ".out");
            if (!Files.exists(inputFile)) {
                return new RestfulResult(404, "测试用例" + caseNum + "不存在");
            }
            String inputData = request.getInput();
            String outputData = request.getOutput();
            if (inputData != null) {
                if (!inputData.endsWith("\n")) inputData += "\n";
                Files.write(inputFile, inputData.getBytes(StandardCharsets.UTF_8));
            }
            if (outputData != null) {
                if (!outputData.endsWith("\n")) outputData += "\n";
                Files.write(outputFile, outputData.getBytes(StandardCharsets.UTF_8));
            }
            return new RestfulResult(200, "success", "测试用例" + caseNum + "已更新");
        } catch (Exception e) {
            log.error("Failed to update test data", e);
            return new RestfulResult(500, "更新测试数据失败: " + e.getMessage());
        }
    }
    
    // 10. Delete a single test case
    @DeleteMapping("/testdata/{problemId}/{caseNum}")
    public RestfulResult deleteTestData(@PathVariable Long problemId, @PathVariable int caseNum) {
        checkAdminPermission();
        try {
            String testDataBasePath = getTestDataBasePath();
            Path problemDir = Paths.get(testDataBasePath, String.valueOf(problemId));
            Path inputFile = problemDir.resolve(caseNum + ".in");
            Path outputFile = problemDir.resolve(caseNum + ".out");
            boolean deleted = false;
            if (Files.exists(inputFile)) { Files.delete(inputFile); deleted = true; }
            if (Files.exists(outputFile)) { Files.delete(outputFile); deleted = true; }
            if (!deleted) {
                return new RestfulResult(404, "测试用例" + caseNum + "不存在");
            }
            int nextCase = caseNum + 1;
            while (Files.exists(problemDir.resolve(nextCase + ".in"))) {
                Files.move(problemDir.resolve(nextCase + ".in"), problemDir.resolve((nextCase - 1) + ".in"));
                Path outPath = problemDir.resolve(nextCase + ".out");
                if (Files.exists(outPath)) {
                    Files.move(outPath, problemDir.resolve((nextCase - 1) + ".out"));
                }
                nextCase++;
            }
            return new RestfulResult(200, "success", "测试用例" + caseNum + "已删除");
        } catch (Exception e) {
            log.error("Failed to delete test data", e);
            return new RestfulResult(500, "删除测试数据失败: " + e.getMessage());
        }
    }
    
    @PostMapping("/run-solution")
    public RestfulResult runSolution(@RequestBody RunSolutionRequest request) {
        checkAdminPermission();
        try {
            String source = request.getSource();
            if (source != null) {
                source = source.replace("\\n", "\n").replace("\\t", "\t");
            }
            String language = request.getLanguage() != null ? request.getLanguage() : "cpp";
            List<String> inputs = request.getInputs();
            int timeLimit = request.getTimeLimit() != null ? request.getTimeLimit() : 3000;
            int memoryLimitKB = request.getMemoryLimit() != null ? request.getMemoryLimit() : 65536;
            
            if (source == null || source.isEmpty()) {
                return new RestfulResult(400, "参考代码不能为空");
            }
            if (inputs == null || inputs.isEmpty()) {
                return new RestfulResult(400, "测试输入数据不能为空");
            }
            
            int maxCpuTime = timeLimit;
            long maxMemoryBytes = memoryLimitKB * 1024L;
            
            List<JudgeRunService.RunResult> runResults = judgeRunService.runCodeWithMultipleInputs(
                    source, language, inputs, maxCpuTime, maxMemoryBytes);
            
            List<Map<String, Object>> results = new ArrayList<>();
            int acCount = 0;
            for (JudgeRunService.RunResult rr : runResults) {
                Map<String, Object> item = new HashMap<>();
                item.put("caseNum", rr.getCaseNum());
                item.put("status", rr.getStatus());
                item.put("output", rr.getOutput() != null ? rr.getOutput() : "");
                item.put("error", rr.getError() != null ? rr.getError() : "");
                item.put("cpuTime", rr.getCpuTime());
                item.put("memory", rr.getMemory());
                results.add(item);
                if ("AC".equals(rr.getStatus())) {
                    acCount++;
                }
            }
            
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("results", results);
            resultData.put("totalCases", inputs.size());
            resultData.put("acCases", acCount);
            resultData.put("allPassed", acCount == inputs.size());
            
            return new RestfulResult(200, "success", resultData);
        } catch (Exception e) {
            log.error("Run solution failed", e);
            return new RestfulResult(500, "运行参考代码失败: " + e.getMessage());
        }
    }
    
    private String getTestDataBasePath() {
        String envPath = System.getenv("TEST_DATA_PATH");
        if (envPath != null && !envPath.isEmpty()) {
            return envPath;
        }
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            String projectDataPath = "D:\\OJSystem\\data\\ojdata";
            if (new File(projectDataPath).exists()) {
                return projectDataPath;
            }
            return "D:\\OnlineOJData\\ojdata";
        }
        File ojContainerPath = new File("/onlinejudge/ojdata");
        if (ojContainerPath.exists() || new File("/onlinejudge").exists()) {
            return "/onlinejudge/ojdata";
        }
        File judgerContainerPath = new File("/ojdata");
        if (judgerContainerPath.exists() && judgerContainerPath.isDirectory()) {
            return "/ojdata";
        }
        return "/ojdata";
    }
    
    // Parse AI-generated content into problem fields
    // Supports JSON format (preferred) and falls back to Markdown section extraction
    private Map<String, Object> parseProblemContent(String content) {
        Map<String, Object> result = new HashMap<>();
        if (content == null) return result;
        
        // Try JSON parsing first
        String jsonStr = extractJsonFromContent(content);
        if (jsonStr != null) {
            try {
                JSONObject json = JSON.parseObject(jsonStr);
                result.put("title", json.getString("title"));
                result.put("description", json.getString("description"));
                result.put("input", json.getString("input"));
                result.put("output", json.getString("output"));
                result.put("sampleInput", json.getString("sampleInput"));
                result.put("sampleOutput", json.getString("sampleOutput"));
                result.put("hint", json.getString("hint"));
                result.put("source", json.getString("source") != null ? json.getString("source") : "AI Generated");
                result.put("timeLimit", json.getInteger("timeLimit") != null ? json.getInteger("timeLimit") : 1000);
                result.put("memoryLimit", json.getInteger("memoryLimit") != null ? json.getInteger("memoryLimit") : 65536);
                result.put("score", json.getInteger("score") != null ? json.getInteger("score") : 100);
                JSONObject solution = json.getJSONObject("solution");
                if (solution != null) {
                    Map<String, Object> solutionMap = new HashMap<>();
                    solutionMap.put("language", solution.getString("language") != null ? solution.getString("language") : "cpp");
                    solutionMap.put("code", solution.getString("code") != null ? solution.getString("code") : "");
                    result.put("solution", solutionMap);
                }
                log.info("Successfully parsed AI response as JSON");
                return result;
            } catch (Exception e) {
                log.warn("Failed to parse AI response as JSON, falling back to Markdown extraction", e);
            }
        }
        
        // Fallback: Markdown section extraction
        result.put("title", extractTitle(content));
        result.put("description", extractSection(content, "题目描述", "输入格式|输入说明|输入"));
        result.put("input", extractSection(content, "输入格式", "输出格式|输出说明|输出"));
        result.put("output", extractSection(content, "输出格式", "样例输入|输入样例"));
        result.put("sampleInput", extractSection(content, "样例输入", "样例输出|输出样例"));
        result.put("sampleOutput", extractSection(content, "样例输出", "提示|说明|约束"));
        result.put("hint", extractSection(content, "提示", "来源|数据范围|复杂度"));
        result.put("source", "AI Generated");
        result.put("timeLimit", 1000);
        result.put("memoryLimit", 65536);
        result.put("score", 100);
        
        return result;
    }
    
    private String extractJsonFromContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return null;
        }
        
        int jsonStart = content.indexOf("```json");
        if (jsonStart != -1) {
            int jsonBodyStart = content.indexOf("{", jsonStart);
            if (jsonBodyStart != -1) {
                int depth = 0;
                int jsonEnd = -1;
                for (int i = jsonBodyStart; i < content.length(); i++) {
                    char c = content.charAt(i);
                    if (c == '{') depth++;
                    else if (c == '}') {
                        depth--;
                        if (depth == 0) {
                            jsonEnd = i;
                            break;
                        }
                    }
                }
                if (jsonEnd != -1) {
                    String extracted = content.substring(jsonBodyStart, jsonEnd + 1).trim();
                    log.info("Extracted JSON from ```json block, length: {}", extracted.length());
                    return extracted;
                }
                int jsonEndFallback = content.indexOf("```", jsonBodyStart);
                if (jsonEndFallback != -1) {
                    String extracted = content.substring(jsonBodyStart, jsonEndFallback).trim();
                    log.info("Extracted JSON from ```json block (fallback), length: {}", extracted.length());
                    return extracted;
                }
            }
        }
        
        int braceStart = content.indexOf("{");
        if (braceStart != -1) {
            int depth = 0;
            int braceEnd = -1;
            for (int i = braceStart; i < content.length(); i++) {
                char c = content.charAt(i);
                if (c == '{') depth++;
                else if (c == '}') {
                    depth--;
                    if (depth == 0) {
                        braceEnd = i;
                        break;
                    }
                }
            }
            if (braceEnd != -1) {
                String candidate = content.substring(braceStart, braceEnd + 1);
                if (candidate.contains("\"testInputs\"") || candidate.contains("\"testCases\"") ||
                    (candidate.contains("\"title\"") && candidate.contains("\"description\""))) {
                    log.info("Extracted JSON from raw content (brace matching), length: {}", candidate.length());
                    return candidate;
                }
            }
            
            int braceEndFallback = content.lastIndexOf("}");
            if (braceEndFallback > braceStart) {
                String candidate = content.substring(braceStart, braceEndFallback + 1);
                if (candidate.contains("\"testInputs\"") || candidate.contains("\"testCases\"")) {
                    log.info("Extracted JSON from raw content (fallback), length: {}", candidate.length());
                    return candidate;
                }
            }
        }
        
        log.warn("Failed to extract JSON from AI response. Content preview: {}", 
                content.length() > 500 ? content.substring(0, 500) + "..." : content);
        return null;
    }
    
    private String sanitizeTestDataJson(String jsonStr) {
        jsonStr = jsonStr.replaceAll("\"\\s*\\+\\s*\"[^\"]*\\.join\\([^)]*\\)[^\"]*\"", "\"<data_placeholder>\"");
        jsonStr = jsonStr.replaceAll("\"[^\"]*\\.join\\([^)]*\\)[^\"]*\"", "\"<data_placeholder>\"");
        jsonStr = jsonStr.replaceAll("\"[^\"]*range\\([^)]*\\)[^\"]*\"", "\"<data_placeholder>\"");
        jsonStr = jsonStr.replaceAll("\"[^\"]*for\\s+\\w+\\s+in\\s+[^\"\\]]*\"", "\"<data_placeholder>\"");
        jsonStr = jsonStr.replaceAll("\\+\\s*\"\\\\n\"\\.join\\([^)]*\\)", "");
        jsonStr = jsonStr.replaceAll("\"\\s*\\+\\s*\"", "");
        jsonStr = jsonStr.replaceAll(",\\s*\\]", "]");
        jsonStr = jsonStr.replaceAll(",\\s*\\}", "}");
        jsonStr = jsonStr.replaceAll("[\\x00-\\x1f&&[^\\n\\r\\t]]", " ");
        return jsonStr;
    }
    
    private String ensureUniqueTitle(String baseTitle, Long excludeId) {
        String candidate = baseTitle;
        if (candidate.length() > 45) {
            candidate = candidate.substring(0, 45);
        }
        int suffix = 2;
        while (true) {
            Optional<Problem> existingOpt = problemRepository.findByTitle(candidate);
            if (!existingOpt.isPresent() || (excludeId != null && existingOpt.get().getId().equals(excludeId))) {
                return candidate;
            }
            String suffixStr = "(" + suffix + ")";
            int maxBaseLen = 50 - suffixStr.length();
            String truncatedBase = baseTitle.length() > maxBaseLen ? baseTitle.substring(0, maxBaseLen) : baseTitle;
            candidate = truncatedBase + suffixStr;
            suffix++;
            if (suffix > 100) {
                return candidate;
            }
        }
    }
    
    private boolean containsCodeExpression(String input) {
        if (input == null) return false;
        return input.contains(".join(") ||
               input.contains("range(") ||
               input.contains("for i in") ||
               input.contains("for x in") ||
               input.contains("list(") ||
               input.contains("map(") ||
               input.contains("filter(") ||
               input.contains("lambda ") ||
               (input.contains("[") && input.contains("for") && input.contains("in") && input.contains("]")) ||
               input.matches(".*\\b\\w+\\.join\\(.*\\).*") ||
               input.contains("...") ||
               input.contains("…");
    }
    
    private String extractTitle(String text) {
        String title = extractBetweenMarkers(text, "# ", "\n");
        if (title == null || title.isEmpty()) {
            title = extractBetweenMarkers(text, "## ", "\n");
        }
        return title != null ? title.trim() : "AI生成的题目";
    }
    
    private String extractBetweenMarkers(String text, String startMarker, String endMarker) {
        int startIdx = text.indexOf(startMarker);
        if (startIdx == -1) return "";
        startIdx += startMarker.length();
        int endIdx = text.indexOf(endMarker, startIdx);
        if (endIdx == -1) return text.substring(startIdx).trim();
        return text.substring(startIdx, endIdx).trim();
    }
    
    private String extractSection(String text, String sectionStart, String sectionEndPattern) {
        int startIdx = text.indexOf(sectionStart);
        if (startIdx == -1) return "";
        
        // Find the end: either the next section header or end of text
        int endIdx = text.length();
        String[] endMarkers = sectionEndPattern.split("\\|");
        for (String marker : endMarkers) {
            int idx = text.indexOf(marker, startIdx + sectionStart.length());
            if (idx != -1 && idx < endIdx) {
                endIdx = idx;
            }
        }
        
        String section = text.substring(startIdx + sectionStart.length(), endIdx).trim();
        // Remove leading colon or space
        if (section.startsWith(":") || section.startsWith("：")) {
            section = section.substring(1).trim();
        }
        return section;
    }
    
    @Data
    static class AsyncGenerateRequest {
        private String keywords;
        private String difficulty;
    }
    
    @Data
    static class EditDraftRequest {
        private Long problemId;
        private String title;
        private String description;
        private String input;
        private String output;
        private String sampleInput;
        private String sampleOutput;
        private String hint;
        private String source;
        private Integer timeLimit;
        private Integer memoryLimit;
        private Integer score;
        private String tags;
    }
    
    @Data
    static class GenerateTestDataRequest {
        private Long problemId;
        private int caseCount;
    }
    
    @Data
    static class ConfirmTestDataRequest {
        private String previewId;
        private Long problemId;
        private List<String> inputs;
        private List<TestCaseOutput> outputs;
        
        @Data
        static class TestCaseOutput {
            private String output;
        }
    }
    
    @Data
    static class RunSolutionRequest {
        private String source;
        private String language;
        private List<String> inputs;
        private Integer timeLimit;
        private Integer memoryLimit;
    }
    
    @Data
    static class UpdateTestDataRequest {
        private String input;
        private String output;
    }
    
    @Data
    static class TestDataPreview {
        private Long problemId;
        private JSONArray testInputs;
        
        public TestDataPreview(Long problemId, JSONArray testInputs) {
            this.problemId = problemId;
            this.testInputs = testInputs;
        }
    }
}
