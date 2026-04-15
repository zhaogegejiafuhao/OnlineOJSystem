package cn.edu.zjnu.acm.service;

import cn.edu.zjnu.acm.entity.ai.AIGeneration;
import cn.edu.zjnu.acm.repo.ai.AIGenerationRepository;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class AIService {
    private final AIGenerationRepository aiGenerationRepository;
    private final RestTemplate restTemplate;
    
    @Value("${ai.api.key}")
    private String apiKey;

    @Value("${ai.api.url}")
    private String apiUrl;

    @Value("${ai.model.default}")
    private String defaultModel;
    
    public AIService(AIGenerationRepository aiGenerationRepository) {
        this.aiGenerationRepository = aiGenerationRepository;
        this.restTemplate = new RestTemplate();
    }
    
    // 生成题目
    @Transactional
    public AIGeneration generateProblem(String keywords, String difficulty) {
        AIGeneration generation = new AIGeneration("problem", defaultModel, buildProblemPrompt(keywords, difficulty));
        generation.setStatus("PROCESSING");
        generation = aiGenerationRepository.save(generation);
        
        try {
            long startTime = System.currentTimeMillis();
            String generatedContent = callLLM(generation.getPrompt());
            long endTime = System.currentTimeMillis();
            
            generation.setGeneratedContent(generatedContent);
            generation.setStatus("COMPLETED");
            generation.setCompleteTime(Instant.now());
            generation.setResponseTime((endTime - startTime) / 1000.0);
            
            // 解析生成的题目内容，提取难度等信息
            parseProblemContent(generation);
            
        } catch (Exception e) {
            log.error("Error generating problem", e);
            generation.setStatus("FAILED");
            generation.setEvaluation("Error: " + e.getMessage());
        }
        
        return aiGenerationRepository.save(generation);
    }
    
    // 生成题目解析
    @Transactional
    public AIGeneration generateAnalysis(String problemContent) {
        AIGeneration generation = new AIGeneration("analysis", defaultModel, buildAnalysisPrompt(problemContent));
        generation.setStatus("PROCESSING");
        generation = aiGenerationRepository.save(generation);
        
        try {
            long startTime = System.currentTimeMillis();
            String generatedContent = callLLM(generation.getPrompt());
            long endTime = System.currentTimeMillis();
            
            generation.setGeneratedContent(generatedContent);
            generation.setStatus("COMPLETED");
            generation.setCompleteTime(Instant.now());
            generation.setResponseTime((endTime - startTime) / 1000.0);
            
        } catch (Exception e) {
            log.error("Error generating analysis", e);
            generation.setStatus("FAILED");
            generation.setEvaluation("Error: " + e.getMessage());
        }
        
        return aiGenerationRepository.save(generation);
    }
    
    // 生成参考解答
    @Transactional
    public AIGeneration generateSolution(String problemContent, String language) {
        AIGeneration generation = new AIGeneration("solution", defaultModel, buildSolutionPrompt(problemContent, language));
        generation.setStatus("PROCESSING");
        generation = aiGenerationRepository.save(generation);
        
        try {
            long startTime = System.currentTimeMillis();
            String generatedContent = callLLM(generation.getPrompt());
            long endTime = System.currentTimeMillis();
            
            generation.setGeneratedContent(generatedContent);
            generation.setStatus("COMPLETED");
            generation.setCompleteTime(Instant.now());
            generation.setResponseTime((endTime - startTime) / 1000.0);
            
        } catch (Exception e) {
            log.error("Error generating solution", e);
            generation.setStatus("FAILED");
            generation.setEvaluation("Error: " + e.getMessage());
        }
        
        return aiGenerationRepository.save(generation);
    }
    
    // 调用大语言模型API
    private String callLLM(String prompt) throws Exception {
        // 这里实现具体的LLM API调用
        // 不同的模型服务提供商有不同的API接口
        // 这里使用通用的实现，具体需要根据实际使用的模型服务调整
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Accept", "application/json");
        headers.set("Authorization", "Bearer " + apiKey);
        
        // 优先尝试responses端点，因为用户提到API提供gemini-2.0-flash模型
        String[] possibleEndpoints = {
            apiUrl + "/responses",
            apiUrl + "/chat/completions",
            apiUrl + "/completions"
        };
        
        // 存储所有错误信息，用于最终诊断
        StringBuilder allErrors = new StringBuilder();
        
        for (String endpoint : possibleEndpoints) {
            try {
                // 根据端点选择不同的请求格式
                String requestBodyJson;
                if (endpoint.endsWith("/responses")) {
                    // 使用responses格式 - 针对gemini-2.0-flash模型
                    Map<String, Object> requestBody = new HashMap<>();
                    requestBody.put("model", defaultModel);
                    
                    // 创建消息对象
                    Map<String, String> message = new HashMap<>();
                    message.put("role", "user");
                    message.put("content", prompt);
                    
                    // 创建input数组
                    java.util.List<Map<String, String>> inputArray = new java.util.ArrayList<>();
                    inputArray.add(message);
                    
                    requestBody.put("input", inputArray);
                    requestBodyJson = JSON.toJSONString(requestBody);
                    
                    log.info("Using /responses format for endpoint: {}", endpoint);
                } else if (endpoint.endsWith("/chat/completions")) {
                    // 使用chat/completions格式
                    Map<String, Object> requestBody = new HashMap<>();
                    requestBody.put("model", defaultModel);
                    
                    // 创建消息对象
                    Map<String, String> message = new HashMap<>();
                    message.put("role", "user");
                    message.put("content", prompt);
                    
                    // 创建messages数组
                    java.util.List<Map<String, String>> messagesArray = new java.util.ArrayList<>();
                    messagesArray.add(message);
                    
                    requestBody.put("messages", messagesArray);
                    requestBody.put("max_tokens", 2000);
                    requestBody.put("temperature", 0.7);
                    requestBodyJson = JSON.toJSONString(requestBody);
                    
                    log.info("Using /chat/completions format for endpoint: {}", endpoint);
                } else {
                    // 使用传统completions格式
                    Map<String, Object> requestBody = new HashMap<>();
                    requestBody.put("model", defaultModel);
                    requestBody.put("prompt", prompt);
                    requestBody.put("max_tokens", 2000);
                    requestBody.put("temperature", 0.7);
                    requestBodyJson = JSON.toJSONString(requestBody);
                    
                    log.info("Using traditional format for endpoint: {}", endpoint);
                }
                
                HttpEntity<String> requestEntity = new HttpEntity<>(requestBodyJson, headers);
                
                log.info("Calling AI API with URL: {}", endpoint);
                log.info("Request body: {}", requestBodyJson);
                log.info("Request headers: {}", headers);
                
                ResponseEntity<String> response = restTemplate.exchange(
                        endpoint,
                        HttpMethod.POST,
                        requestEntity,
                        String.class
                );
                
                log.info("API Response status: {}", response.getStatusCode());
                log.info("API Response headers: {}", response.getHeaders());
                log.info("API Response body length: {}", response.getBody() != null ? response.getBody().length() : 0);
                
                // 检查响应是否为HTML
                String responseBody = response.getBody();
                if (responseBody != null && (responseBody.startsWith("<!DOCTYPE html>") || responseBody.startsWith("<html"))) {
                    String errorMsg = "API returned HTML instead of JSON: " + responseBody.substring(0, Math.min(500, responseBody.length()));
                    log.error(errorMsg);
                    allErrors.append("\n- ").append(endpoint).append(": ").append(errorMsg);
                    continue; // 尝试下一个端点
                }
                
                // 解析API响应
                try {
                    JSONObject responseJson = JSON.parseObject(responseBody);
                    log.info("Response JSON keys: {}", responseJson.keySet());
                    log.info("Full response JSON: {}", responseBody);
                    
                    // 尝试从不同的响应结构中提取内容
                    if (responseJson.containsKey("content")) {
                        return responseJson.getString("content");
                    } else if (responseJson.containsKey("choices") && responseJson.getJSONArray("choices").size() > 0) {
                        JSONObject choice = responseJson.getJSONArray("choices").getJSONObject(0);
                        if (choice.containsKey("text")) {
                            return choice.getString("text");
                        } else if (choice.containsKey("message") && choice.getJSONObject("message").containsKey("content")) {
                            return choice.getJSONObject("message").getString("content");
                        } else if (choice.containsKey("content")) {
                            return choice.getString("content");
                        }
                    }
                    
                    String errorMsg = "Unexpected API response structure: " + responseBody;
                    log.error(errorMsg);
                    allErrors.append("\n- ").append(endpoint).append(": ").append(errorMsg);
                    continue; // 尝试下一个端点
                } catch (JSONException e) {
                    String errorMsg = "Failed to parse API response as JSON: " + responseBody;
                    log.error(errorMsg, e);
                    allErrors.append("\n- ").append(endpoint).append(": ").append(errorMsg);
                    continue; // 尝试下一个端点
                }
            } catch (Exception e) {
                String errorMsg = "Error calling API endpoint: " + e.getMessage();
                log.error(errorMsg, e);
                allErrors.append("\n- ").append(endpoint).append(": ").append(errorMsg);
                continue; // 尝试下一个端点
            }
        }
        
        // 如果所有端点都失败，抛出详细的错误信息
        throw new Exception("All API endpoints failed. Please check your API URL and key configuration. Possible issues:\n" +
                "1. API URL is incorrect\n" +
                "2. API key is invalid\n" +
                "3. API endpoint path is missing\n" +
                "4. Network connection issues\n\n" +
                "Detailed error information:" + allErrors.toString());
    }
    
    // 构建题目生成提示
    private String buildProblemPrompt(String keywords, String difficulty) {
        return "请根据以下关键词生成一道编程题目：\n" +
               "关键词：" + keywords + "\n" +
               "难度级别：" + difficulty + "\n" +
               "要求：\n" +
               "1. 题目描述清晰完整\n" +
               "2. 包含输入输出格式说明\n" +
               "3. 提供2-3个样例输入输出\n" +
               "4. 给出题目的难度分析\n" +
               "5. 提供参考解答思路\n" +
               "6. 确保题目具有实际编程价值\n" +
               "7. 格式要求：\n" +
               "   a. 使用Markdown格式\n" +
               "   b. 每个部分之间使用空行分隔\n" +
               "   c. 代码块使用```包裹\n" +
               "   d. 确保生成的内容分段清晰，易于阅读";
    }
    
    // 构建题目解析提示
    private String buildAnalysisPrompt(String problemContent) {
        return "请对以下编程题目进行详细解析：\n" +
               problemContent + "\n" +
               "要求：\n" +
               "1. 分析题目的核心考点\n" +
               "2. 提供详细的解题思路\n" +
               "3. 给出优化建议\n" +
               "4. 分析时间复杂度和空间复杂度\n" +
               "5. 提供至少一种参考实现\n" +
               "6. 解析过程要深入浅出，适合不同水平的学习者";
    }
    
    // 构建解答生成提示
    private String buildSolutionPrompt(String problemContent, String language) {
        return "请为以下编程题目提供" + language + "语言的参考解答：\n" +
               problemContent + "\n" +
               "要求：\n" +
               "1. 代码要正确无误\n" +
               "2. 代码风格良好，有适当的注释\n" +
               "3. 算法效率要合理\n" +
               "4. 处理边界情况\n" +
               "5. 提供简洁的解题思路说明\n" +
               "6. 确保代码可以直接运行";
    }
    
    // 解析生成的题目内容
    private void parseProblemContent(AIGeneration generation) {
        String content = generation.getGeneratedContent();
        if (content == null) return;
        
        // 这里可以实现更复杂的解析逻辑
        // 例如提取题目难度、输入输出格式等信息
        
        // 简单的难度提取示例
        if (content.contains("难度：简单") || content.contains("难度级别：简单")) {
            generation.setDifficulty(1);
        } else if (content.contains("难度：中等") || content.contains("难度级别：中等")) {
            generation.setDifficulty(2);
        } else if (content.contains("难度：困难") || content.contains("难度级别：困难")) {
            generation.setDifficulty(3);
        }
        
        // 生成评估
        generation.setEvaluation("题目生成成功，包含完整的题目描述和样例输入输出");
    }
    
    // 获取生成记录
    public Optional<AIGeneration> getGenerationById(Long id) {
        return aiGenerationRepository.findById(id);
    }
    
    // 获取生成记录列表
    public org.springframework.data.domain.Page<AIGeneration> getGenerations(int page, int size) {
        return aiGenerationRepository.findAll(org.springframework.data.domain.PageRequest.of(page, size));
    }
    
    // 获取统计信息
    public AIGenerationStats getStats() {
        long totalGenerations = aiGenerationRepository.count();
        long completedGenerations = aiGenerationRepository.countByStatus("COMPLETED");
        
        Double avgResponseTimeObj = aiGenerationRepository.getAverageResponseTimeByModel(defaultModel);
        double avgResponseTime = avgResponseTimeObj != null ? avgResponseTimeObj : 0.0;
        
        return new AIGenerationStats(totalGenerations, completedGenerations, avgResponseTime);
    }
    
    // 统计信息数据类
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class AIGenerationStats {
        private long totalGenerations;
        private long completedGenerations;
        private double avgResponseTime;
    }
}