package cn.edu.zjnu.acm.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
public class AIConfig {
    @Value("${ai.api.key:}")
    private String apiKey;
    
    @Value("${ai.api.url:https://api.openai.com/v1/chat/completions}")
    private String apiUrl;
    
    @Value("${ai.model.default:gpt-3.5-turbo}")
    private String defaultModel;
    
    @Value("${ai.max.tokens:2000}")
    private Integer maxTokens;
    
    @Value("${ai.temperature:0.7}")
    private Double temperature;
    
    @Value("${ai.timeout:30000}")
    private Integer timeout;
}