package cn.edu.zjnu.acm.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/error")
public class ErrorViewController {
    
    @GetMapping("/book")
    public String errorBook() {
        return "error/error_book";
    }
    
    @GetMapping("/stats")
    public String errorStats() {
        return "error/error_stats";
    }
}