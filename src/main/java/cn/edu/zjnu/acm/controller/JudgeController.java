package cn.edu.zjnu.acm.controller;

import cn.edu.zjnu.acm.config.Config;
import cn.edu.zjnu.acm.entity.oj.Solution;
import cn.edu.zjnu.acm.exception.NotFoundException;
import cn.edu.zjnu.acm.service.JudgeService;
import cn.edu.zjnu.acm.service.SolutionService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;

@Slf4j
@RestController
public class JudgeController {
    private final JudgeService judgeService;
    private final SolutionService solutionService;

    public JudgeController(JudgeService judgeService, Config config, SolutionService solutionService) {
        this.judgeService = judgeService;
        this.solutionService = solutionService;
    }

    @PostMapping("/judge/callback")
    public String judgeCallback(@RequestBody JudgeController.JudgeCallback callback) {
        try {
            log.info("Judge callback received: {}", callback.toString());
            if (callback == null || callback.getSubmit_id() == null) {
                log.error("Invalid callback: callback or submit_id is null");
                return "invalid callback";
            }
            Solution solution = solutionService.getSolutionById(callback.getSubmit_id());
            if (solution == null) {
                log.error("Solution not found for id: {}", callback.getSubmit_id());
                return "no this id";
            }
            if (!solution.getResult().equals(Solution.PENDING)) {
                // 只更新PENDING状态的solution
                // 如果rejudge 需要设置solution 为PENDING
                log.debug("Solution {} is not in PENDING state (current: {}), skipping update", solution.getId(), solution.getResult());
                return "success";
            }
            if (callback.getErr() != null) {
                log.warn("Judge service returned error for solution {}: err={}, info={}", 
                        solution.getId(), callback.getErr(), callback.getInfo());
                if (callback.getErr().equals("CE")) {
                    solution.setResult(Solution.CE);
                    log.info("Setting solution {} to Compilation Error", solution.getId());
                } else {
                    solution.setResult(Solution.SE);
                    log.info("Setting solution {} to System Error (err={})", solution.getId(), callback.getErr());
                }
                String info = callback.getInfo();
                if (info == null || info.trim().isEmpty() || info.equals("No Data")) {
                    // 提供更友好的错误信息
                    if (callback.getErr().equals("CE")) {
                        info = "Compilation failed. Please check your code syntax.";
                    } else {
                        info = "Judge service error: " + callback.getErr() + ". Please contact administrator.";
                    }
                }
                solution.setInfo(info);
                solution.setTime(0);
                solution.setMemory(0);
                solution.setCaseNumber(0); // 保持0，表示未运行到测试用例
                // Update result info first
                solutionService.updateSolutionResultInfo(solution);
                // Then update statistics (submitted count, etc.)
                judgeService.update(solution);
                log.info("Updated solution {} result to {} with info: {}", solution.getId(), solution.getResult(), info);
                return "success";
            } else if (callback.getResults() == null || callback.getResults().size() == 0) {
                log.warn("Judge service returned no results for solution {}", solution.getId());
                solution.setResult(Solution.SE);
                solution.setInfo("No results from judge service");
                solution.setTime(0);
                solution.setMemory(0);
                solution.setCaseNumber(0);
                // Update result info first
                solutionService.updateSolutionResultInfo(solution);
                // Then update statistics (submitted count, etc.)
                judgeService.update(solution);
                log.info("Updated solution {} result to System Error (no results)", solution.getId());
                return "success";
            }
            log.info("Processing {} test case results for solution {}", callback.getResults().size(), solution.getId());
            int cpu = 0;
            int memory = 0;
            int caseNumber = 0;
            for (int i = 0; i < callback.getResults().size(); i++) {
                caseNumber = i + 1;
                JudgeController.JudgeCallback.RunMessage runMessage = callback.getResults().get(i);
                if (runMessage == null) {
                    log.warn("Null run message at index {} for solution {}", i, solution.getId());
                    continue;
                }
                String result = runMessage.getRunResult();
                solution.setResult(result);
                log.debug("Test case {} for solution {}: result={}, cpu_time={}, memory={}", 
                        caseNumber, solution.getId(), result, runMessage.getCpu_time(), runMessage.getMemory());
                // Always record actual time and memory usage, even if the test case failed
                cpu = Math.max(cpu, runMessage.getCpu_time());
                memory = Math.max(memory, runMessage.getMemory());
                if (runMessage.getResult() > 3) {
                    log.info("Solution {} failed at test case {} with result: {}, time: {}ms, memory: {}B", 
                            solution.getId(), caseNumber, result, runMessage.getCpu_time(), runMessage.getMemory());
                    break;
                }
            }
            solution.setCaseNumber(caseNumber);
            solution.setTime(cpu);
            solution.setMemory(memory);
            log.info("Updating solution {} with result: {}, case: {}, time: {}ms, memory: {}B", 
                    solution.getId(), solution.getResult(), caseNumber, cpu, memory);
            judgeService.update(solution);
            return "success";
        } catch (Exception e) {
            log.error("Error processing judge callback", e);
            return "internal error";
        }
    }

    @Data
    static class JudgeCallback {
        @NotNull
        private Long submit_id;
        private String err;
        private String info;
        private ArrayList<RunMessage> results;

        @Data
        static class RunMessage {
            public static final String[] code = new String[]{Solution.WA, Solution.AC,
                    Solution.TLE,
                    Solution.TLE,
                    Solution.MLE,
                    Solution.RE,
                    Solution.SE};
            private int cpu_time;
            private int real_time;
            private int memory;
            private int signal;
            private int exit_code;
            private int error;
            private int result;

            public String getRunResult() {
                return code[result + 1];
            }
        }
    }
}
