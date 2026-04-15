package cn.edu.zjnu.acm.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/ai")
public class AIVIewController {
    
    @GetMapping("/assistant")
    public String aiAssistant() {
        return "ai/assistant";
    }
    
    @GetMapping("/generate")
    public String aiGenerate() {
        return "ai/generate";
    }
}