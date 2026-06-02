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
    public AIGeneration generateProblem(String keywords, String difficulty, Long userId) {
        AIGeneration generation = new AIGeneration("problem", defaultModel, buildProblemPrompt(keywords, difficulty));
        generation.setUserId(userId);
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
    public AIGeneration generateAnalysis(String problemContent, Long userId) {
        AIGeneration generation = new AIGeneration("analysis", defaultModel, buildAnalysisPrompt(problemContent));
        generation.setUserId(userId);
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
    public AIGeneration generateSolution(String problemContent, String language, Long userId) {
        AIGeneration generation = new AIGeneration("solution", defaultModel, buildSolutionPrompt(problemContent, language));
        generation.setUserId(userId);
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
    public String callLLM(String prompt) throws Exception {
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
        return "请根据以下关键词生成一道编程题目及其参考解答：\n" +
               "关键词：" + keywords + "\n" +
               "难度级别：" + difficulty + "\n" +
               "\n" +
               "【重要】你必须严格按照以下JSON格式输出，不要输出任何JSON之外的内容：\n" +
               "```json\n" +
               "{\n" +
               "  \"title\": \"题目名称（简短有力，不超过30字）\",\n" +
               "  \"description\": \"题目描述（使用Markdown格式，数学公式用LaTeX：行内公式$...$，独立公式$$...$$）\",\n" +
               "  \"input\": \"输入格式说明（使用Markdown格式，数学公式用LaTeX）\",\n" +
               "  \"output\": \"输出格式说明（使用Markdown格式，数学公式用LaTeX）\",\n" +
               "  \"sampleInput\": \"样例输入（纯文本）\",\n" +
               "  \"sampleOutput\": \"样例输出（纯文本）\",\n" +
               "  \"hint\": \"提示信息（使用Markdown格式，数学公式用LaTeX）\",\n" +
               "  \"source\": \"题目来源\",\n" +
               "  \"timeLimit\": 1000,\n" +
               "  \"memoryLimit\": 65536,\n" +
               "  \"score\": 100,\n" +
               "  \"solution\": {\n" +
               "    \"language\": \"cpp\",\n" +
               "    \"code\": \"完整的C++参考解答代码\"\n" +
               "  }\n" +
               "}\n" +
               "```\n" +
               "\n" +
               "要求：\n" +
               "1. title：简短精炼的题目名称\n" +
               "2. description：完整的题目背景和问题描述，使用Markdown格式\n" +
               "   - 数学公式使用LaTeX语法：行内公式用 $...$ 包裹，独立公式用 $$...$$ 包裹\n" +
               "   - 例如：$1 \\leq n \\leq 10^5$，$a_i$，$\\sum_{i=1}^{n} a_i$\n" +
               "3. input：清晰描述输入格式，包括变量含义、取值范围，数学公式用LaTeX\n" +
               "4. output：清晰描述输出格式和要求，数学公式用LaTeX\n" +
               "5. sampleInput：提供2-3组样例输入，纯文本\n" +
               "6. sampleOutput：与样例输入一一对应的样例输出，纯文本\n" +
               "7. hint：解题提示、算法思路，数学公式用LaTeX\n" +
               "8. solution.language：固定为 \"cpp\"\n" +
               "9. solution.code：完整的C++参考解答代码，要求：\n" +
               "   - 使用标准C++14语法\n" +
               "   - 包含必要的头文件\n" +
               "   - 代码能正确编译运行\n" +
               "   - 从标准输入读取，输出到标准输出\n" +
               "   - 处理所有边界情况\n" +
               "10. timeLimit：时间限制（毫秒），简单题1000-2000，中等1000-3000，困难2000-5000\n" +
               "11. memoryLimit：内存限制（KB），通常65536或131072\n" +
               "12. score：题目分数，简单100，中等150，困难200\n" +
               "\n" +
               "注意：只输出JSON，不要输出任何其他文字说明！";
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
        return aiGenerationRepository.findByDeletedFalseOrderByCreateTimeDesc(org.springframework.data.domain.PageRequest.of(page, size));
    }
    
    public Long getDailyUsageCount(Long userId, Instant since) {
        return aiGenerationRepository.countByUserIdAndCreateTimeAfter(userId, since);
    }
    
    @org.springframework.transaction.annotation.Transactional
    public void softDeleteGeneration(Long id) {
        aiGenerationRepository.softDelete(id);
    }
    
    // 获取统计信息
    public AIGenerationStats getStats() {
        long totalGenerations = aiGenerationRepository.findByDeletedFalseOrderByCreateTimeDesc(
                org.springframework.data.domain.PageRequest.of(0, 1)).getTotalElements();
        long completedGenerations = aiGenerationRepository.countByStatus("COMPLETED");
        
        Double avgResponseTimeObj = aiGenerationRepository.getAverageResponseTimeByModel(defaultModel);
        double avgResponseTime = avgResponseTimeObj != null ? avgResponseTimeObj : 0.0;
        
        Instant oneDayAgo = Instant.now().minus(1, ChronoUnit.DAYS);
        Double totalCostObj = aiGenerationRepository.getTotalCost(oneDayAgo);
        double totalCost = totalCostObj != null ? totalCostObj : 0.0;
        
        return new AIGenerationStats(totalGenerations, completedGenerations, avgResponseTime, totalCost);
    }
    
    // 统计信息数据类
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class AIGenerationStats {
        private long totalGenerations;
        private long completedGenerations;
        private double avgResponseTime;
        private double totalCost;
    }
}