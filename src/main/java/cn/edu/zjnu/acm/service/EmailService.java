package cn.edu.zjnu.acm.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
public class EmailService {
    private final Optional<JavaMailSender> mailSender;
    
    public EmailService(Optional<JavaMailSender> mailSender) {
        this.mailSender = mailSender;
    }
    
    public void sendEmail(String to, String subject, String content) {
        if (mailSender.isPresent()) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(to);
                message.setSubject(subject);
                message.setText(content);
                message.setFrom("system@onlinejudge.com");
                
                mailSender.get().send(message);
                log.info("Email sent successfully to: {}", to);
            } catch (Exception e) {
                log.error("Error sending email to {}: {}", to, e.getMessage());
                throw new RuntimeException("Failed to send email", e);
            }
        } else {
            log.warn("Email service not configured, skipping email to: {}", to);
            log.warn("Subject: {}", subject);
            log.warn("Content: {}", content);
        }
    }
    
    public void sendAlarmEmail(String to, String alarmType, String alarmMessage, String details) {
        String subject = "【系统报警】" + alarmType;
        String content = "报警信息: " + alarmMessage + "\n" +
                       "详细信息: " + (details != null ? details : "无") + "\n" +
                       "时间: " + new java.util.Date();
        
        sendEmail(to, subject, content);
    }
}