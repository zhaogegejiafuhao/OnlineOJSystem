package cn.edu.zjnu.acm.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Deprecated
public class AIModelService {
    private final RestTemplate restTemplate;
    
    @Value("${ai.api.key}")
    private String apiKey;
    
    @Value("${ai.api.url}")
    private String apiUrl;
    
    @Value("${ai.model.default}")
    private String defaultModel;
    
    // 豆包 API 配置
    @Value("${ai.model.doubao.url:https://api.doubao.com/v1/chat/completions}")
    private String doubaoUrl;
    
    @Value("${ai.model.doubao.key:}")
    private String doubaoKey;
    
    @Value("${ai.model.doubao.model:doubao-pro}")
    private String doubaoModel;
    
    // 小米 Mimo API 配置
    @Value("${ai.model.mimo.url:https://api.mimo.xiaomi.com/v1/chat/completions}")
    private String mimoUrl;
    
    @Value("${ai.model.mimo.key:}")
    private String mimoKey;
    
    @Value("${ai.model.mimo.model:mimo-pro}")
    private String mimoModel;
    
    public AIModelService() {
        this.restTemplate = new RestTemplate();
    }
    
    // 调用指定模型生成内容
    public String generateContent(String modelType, String prompt, Map<String, Object> parameters) throws Exception {
        switch (modelType.toLowerCase()) {
            case "doubao":
                return callDoubaoModel(prompt, parameters);
            case "mimo":
                return callMimoModel(prompt, parameters);
            case "openai":
            default:
                return callOpenAIModel(prompt, parameters);
        }
    }
    
    // 调用豆包模型
    private String callDoubaoModel(String prompt, Map<String, Object> parameters) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + (doubaoKey.isEmpty() ? apiKey : doubaoKey));
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", doubaoModel);
        requestBody.put("messages", new Object[]{
            Map.of("role", "user", "content", prompt)
        });
        requestBody.put("temperature", parameters.getOrDefault("temperature", 0.7));
        requestBody.put("max_tokens", parameters.getOrDefault("max_tokens", 2000));
        
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
        
        ResponseEntity<Map> response = restTemplate.exchange(
                doubaoUrl,
                HttpMethod.POST,
                requestEntity,
                Map.class
        );
        
        return parseDoubaoResponse(response.getBody());
    }
    
    // 调用小米 Mimo 模型
    private String callMimoModel(String prompt, Map<String, Object> parameters) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + (mimoKey.isEmpty() ? apiKey : mimoKey));
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", mimoModel);
        requestBody.put("messages", new Object[]{
            Map.of("role", "user", "content", prompt)
        });
        requestBody.put("temperature", parameters.getOrDefault("temperature", 0.7));
        requestBody.put("max_tokens", parameters.getOrDefault("max_tokens", 2000));
        
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
        
        ResponseEntity<Map> response = restTemplate.exchange(
                mimoUrl,
                HttpMethod.POST,
                requestEntity,
                Map.class
        );
        
        return parseMimoResponse(response.getBody());
    }
    
    // 调用 OpenAI 模型
    private String callOpenAIModel(String prompt, Map<String, Object> parameters) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", defaultModel);
        requestBody.put("messages", new Object[]{
            Map.of("role", "user", "content", prompt)
        });
        requestBody.put("temperature", parameters.getOrDefault("temperature", 0.7));
        requestBody.put("max_tokens", parameters.getOrDefault("max_tokens", 2000));
        
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
        
        ResponseEntity<Map> response = restTemplate.exchange(
                apiUrl,
                HttpMethod.POST,
                requestEntity,
                Map.class
        );
        
        return parseOpenAIResponse(response.getBody());
    }
    
    // 解析豆包响应
    private String parseDoubaoResponse(Map<String, Object> response) {
        // 这里应该实现豆包 API 响应的解析逻辑
        return response.toString();
    }
    
    // 解析小米 Mimo 响应
    private String parseMimoResponse(Map<String, Object> response) {
        // 这里应该实现小米 Mimo API 响应的解析逻辑
        return response.toString();
    }
    
    // 解析 OpenAI 响应
    private String parseOpenAIResponse(Map<String, Object> response) {
        // 这里应该实现 OpenAI API 响应的解析逻辑
        return response.toString();
    }
    
    // 批量生成题目
    public Object batchGenerateProblems(String modelType, String keywords, int count, String difficulty) throws Exception {
        String prompt = String.format("请根据以下关键词生成%d道%d难度的编程题目：\n关键词：%s\n要求每道题目包含：题目描述、输入输出格式、样例输入输出、难度分析、参考解答思路",
                count, difficulty, keywords);
        
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("max_tokens", 4000);
        parameters.put("temperature", 0.8);
        
        return generateContent(modelType, prompt, parameters);
    }
    
    // 生成测试数据
    public Object generateTestData(String modelType, String problemDescription, int caseCount) throws Exception {
        String prompt = String.format("请为以下编程题目生成%d组测试数据：\n题目描述：%s\n要求每组测试数据包含：输入数据、预期输出、测试点说明",
                caseCount, problemDescription);
        
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("max_tokens", 3000);
        parameters.put("temperature", 0.6);
        
        return generateContent(modelType, prompt, parameters);
    }
}