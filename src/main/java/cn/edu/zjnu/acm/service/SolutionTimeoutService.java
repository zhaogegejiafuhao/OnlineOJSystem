package cn.edu.zjnu.acm.service;

import cn.edu.zjnu.acm.entity.oj.Solution;
import cn.edu.zjnu.acm.repo.problem.SolutionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
public class SolutionTimeoutService {
    private final SolutionRepository solutionRepository;
    private final SolutionService solutionService;
    private final JudgeService judgeService;
    
    // 超时时间：5分钟
    private static final long TIMEOUT_MINUTES = 5;
    
    public SolutionTimeoutService(SolutionRepository solutionRepository, 
                                   SolutionService solutionService, 
                                   JudgeService judgeService) {
        this.solutionRepository = solutionRepository;
        this.solutionService = solutionService;
        this.judgeService = judgeService;
    }
    
    @Scheduled(fixedRate = 60000) // 每分钟检查一次
    public void checkPendingSolutions() {
        try {
            Instant timeoutThreshold = Instant.now().minusSeconds(TIMEOUT_MINUTES * 60);
            List<Solution> pendingSolutions = solutionRepository.findByResultAndSubmitTimeBefore(
                Solution.PENDING, timeoutThreshold);
            
            for (Solution solution : pendingSolutions) {
                log.warn("Solution {} has been pending for more than {} minutes, marking as System Error", 
                        solution.getId(), TIMEOUT_MINUTES);
                solution.setResult(Solution.SE);
                solution.setInfo("Judge timeout: No response from judge service after " + TIMEOUT_MINUTES + " minutes");
                solution.setTime(0);
                solution.setMemory(0);
                solution.setCaseNumber(0);
                solutionService.updateSolutionResultInfo(solution);
                judgeService.update(solution);
            }
            
            if (!pendingSolutions.isEmpty()) {
                log.info("Marked {} pending solutions as System Error due to timeout", pendingSolutions.size());
            }
        } catch (Exception e) {
            log.error("Error checking pending solutions timeout", e);
        }
    }
}
