package cn.edu.zjnu.acm.service;

import cn.edu.zjnu.acm.config.Config;
import cn.edu.zjnu.acm.entity.oj.*;
import cn.edu.zjnu.acm.repo.contest.ContestProblemRepository;
import cn.edu.zjnu.acm.repo.problem.ProblemRepository;
import cn.edu.zjnu.acm.repo.user.UserProblemRepository;
import cn.edu.zjnu.acm.repo.user.UserProfileRepository;
import cn.edu.zjnu.acm.repo.oj.ErrorRecordRepository;
import com.alibaba.fastjson.JSON;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class JudgeService {
    private final Config config;
    private final RESTService restService;
    private final SolutionService solutionService;
    private final UserProfileRepository userProfileRepository;
    private final ProblemRepository problemRepository;
    private final ContestProblemRepository contestProblemRepository;
    private final ContestService contestService;
    private final UserProblemRepository userProblemRepository;
    private final ErrorRecordRepository errorRecordRepository;
    private static List<HostLoad> judgerLoadScore = new ArrayList<>();

    public JudgeService(Config config, RESTService restService, SolutionService solutionService, UserProfileRepository userProfileRepository, ProblemRepository problemRepository, ContestProblemRepository contestProblemRepository, ContestService contestService, UserProblemRepository userProblemRepository, ErrorRecordRepository errorRecordRepository) {
        this.config = config;
        this.restService = restService;
        this.solutionService = solutionService;
        this.userProfileRepository = userProfileRepository;
        this.problemRepository = problemRepository;
        this.contestProblemRepository = contestProblemRepository;
        this.contestService = contestService;
        this.userProblemRepository = userProblemRepository;
        this.errorRecordRepository = errorRecordRepository;
    }

    public String submitCode(Solution solution) throws Exception {
        if (solution == null) {
            log.error("Cannot submit null solution");
            throw new IllegalArgumentException("Solution cannot be null");
        }
        if (solution.getProblem() == null) {
            log.error("Solution {} has no problem", solution.getId());
            throw new IllegalArgumentException("Solution must have a problem");
        }
        String host = getNextJudger(solution);
        if (host == null || host.isEmpty()) {
            log.error("No judge service available for solution {}", solution.getId());
            throw new IllegalStateException("No judge service available");
        }
        Config.LanguageConfig language;
        switch (solution.getLanguage()) {
            case "c":
                language = config.getC();
                break;
            case "cpp":
                language = config.getCpp();
                break;
            case "java":
                language = config.getJava();
                break;
            case "py2":
                language = config.getPython2();
                break;
            case "py3":
                language = config.getPython3();
                break;
            case "go":
                language = config.getGo();
                break;
            default:
                log.error("Unsupported language: {} for solution {}", solution.getLanguage(), solution.getId());
                throw new IllegalArgumentException("Unsupported language: " + solution.getLanguage());
        }
        if (language == null) {
            log.error("Language config is null for language: {}", solution.getLanguage());
            throw new IllegalStateException("Language config not found for: " + solution.getLanguage());
        }
        // Calculate time and memory limits
        // 1. Convert DB limit (KB) to Bytes. 
        // Standard OJ convention: DB stores KB, Judger expects Bytes.
        long baseMemoryBytes = solution.getProblem().getMemoryLimit() * 1024L;
        long baseTimeLimit = solution.getProblem().getTimeLimit();

        int timeMultiplier = 1;
        int memoryMultiplier = 1;
        
        switch (solution.getLanguage()) {
            case "c":
            case "cpp":
                // C/C++: Baseline
                break;
            case "py2":
            case "py3":
                // Python: 3x Time, 2x Memory
                timeMultiplier = 3;
                memoryMultiplier = 2;
                break;
            case "java":
                // Java: 2x Time, 2x Memory
                timeMultiplier = 2;
                memoryMultiplier = 2;
                break;
            case "go":
                // Go: 2x Time, 2x Memory
                timeMultiplier = 2;
                memoryMultiplier = 2;
                break;
            default:
                timeMultiplier = 2;
                memoryMultiplier = 2;
                break;
        }

        long finalTimeLimit = baseTimeLimit * timeMultiplier;
        long finalMemoryLimit = baseMemoryBytes * memoryMultiplier;

        // Apply Minimum Guarantees (in Bytes)
        // Python/Java/Go runtimes need significant memory just to start.
        long minMemory = 0;
        if (solution.getLanguage().startsWith("py")) {
            minMemory = 128L * 1024 * 1024; // 128MB
        } else if (solution.getLanguage().equals("java") || solution.getLanguage().equals("go")) {
            minMemory = 256L * 1024 * 1024; // 256MB
        } else {
            minMemory = 64L * 1024 * 1024;  // 64MB for C/C++ (Safety net)
        }

        finalMemoryLimit = Math.max(finalMemoryLimit, minMemory);
        
        log.info("Submission {}: Lang={}, Time={}ms, Memory={}B (Base: {}KB, Multiplier: {}x)", 
                solution.getId(), solution.getLanguage(), finalTimeLimit, finalMemoryLimit, 
                solution.getProblem().getMemoryLimit(), memoryMultiplier);

        
        // Build callback URL for judge service
        // Use configured callback URL or fallback to default
        String callbackUrl = config.getCallbackUrl();
        if (callbackUrl == null || callbackUrl.isEmpty()) {
            callbackUrl = "http://oj:8080/judge/callback";
        }
        log.debug("Callback URL for solution {}: {}", solution.getId(), callbackUrl);

        JudgeService.SubmitCode submitCode = new JudgeService.SubmitCode(
                solution.getId().intValue(),
                solution.getProblem().getId().intValue(),
                (int) finalTimeLimit,
                (int) finalMemoryLimit,
                language.getMemory_limit_check_only(),
                language.getSrc(),
                language.getSeccomp_rule(),
                language.getRun_command(),
                language.getCompile_command(),
                solution.getSource(),
                callbackUrl
        );
        log.info("Submitting solution {} to judge service: {}", solution.getId(), host);
        log.debug("Language config: {}", language.toString());
        log.debug("SubmitCode data: {}", submitCode.toString());
        String jsonString = JSON.toJSONString(submitCode);
        log.debug("JSON payload for solution {}: {}", solution.getId(), jsonString);
        String result = restService.postJson(jsonString, host);
        if (result == null) {
            log.error("Judge service returned null for solution {} (host: {})", solution.getId(), host);
            throw new IllegalStateException("Judge service unavailable");
        }
        log.info("Judge service response for solution {}: {}", solution.getId(), result);
        return result;
    }

    @PostConstruct
    public void initJudgerQueue() {
        if (config.getJudgerhost() == null || config.getJudgerhost().isEmpty()) {
            log.warn("No judge service configured! Please set JUDGER_SERVICE environment variable.");
            return;
        }
        for (String host : config.getJudgerhost()) {
            if (host != null && !host.trim().isEmpty() && !host.contains("请输入")) {
            judgerLoadScore.add(new HostLoad(host));
                log.info("Added judge service: {}", host);
            }
        }
        if (judgerLoadScore.isEmpty()) {
            log.error("No valid judge service configured!");
        }
    }

    public synchronized String getNextJudger(Solution solution) {
        if (judgerLoadScore.isEmpty()) {
            log.error("No judge service available!");
            throw new IllegalStateException("No judge service configured. Please set JUDGER_SERVICE environment variable.");
        }
        if (judgerLoadScore.size() == 1) {
            return judgerLoadScore.get(0).getHost();
        }
        String host = judgerLoadScore.get(0).getHost();
        judgerLoadScore.get(0).update(solution.getProblem());
        judgerLoadScore.sort((o1, o2) -> (int) (Math.round(o1.getScore()) - Math.round(o2.getScore())));
        return host;
    }

    @Transactional
    public void update(Solution solution) {
        if (solution == null) {
            log.error("Cannot update null solution");
            return;
        }
        try {
        submitSolutionFilter(solution);
        acceptSolutionFilter(solution);
        errorSolutionFilter(solution);
        contestSolutionFilter(solution);
            log.debug("Successfully updated solution {}", solution.getId());
        } catch (Exception e) {
            log.error("Error updating solution {}", solution.getId(), e);
            throw e;
        }
    }

    private void submitSolutionFilter(Solution solution) {
        try {
        solutionService.updateSolutionResultTimeMemoryCase(solution);
            if (solution.getUser() != null && solution.getUser().getUserProfile() != null) {
        userProfileRepository.updateUserSubmitted(solution.getUser().getUserProfile().getId(), 1);
            }
            if (solution.getProblem() != null) {
        problemRepository.updateSubmittedNumber(solution.getProblem().getId(), 1);
            }
        } catch (Exception e) {
            log.error("Error in submitSolutionFilter for solution {}", solution.getId(), e);
            throw e;
        }
    }

    private void errorSolutionFilter(Solution solution) {
        if (!solution.getResult().equals(Solution.AC)) {
            List<ErrorRecord> existingRecords = errorRecordRepository.findByUserAndProblem(solution.getUser(), solution.getProblem());
            boolean alreadyExists = false;
            for (ErrorRecord record : existingRecords) {
                if (!record.getIsResolved()) {
                    alreadyExists = true;
                    break;
                }
            }
            if (!alreadyExists) {
                ErrorRecord errorRecord = new ErrorRecord(
                        solution.getUser(),
                        solution.getProblem(),
                        solution,
                        solution.getResult(),
                        solution.getInfo()
                );
                errorRecordRepository.save(errorRecord);
            }
        }
    }

    private void contestSolutionFilter(Solution solution) {
        if (solution.getContest() != null) {
            try {
            Contest contest = contestService.getContestById(solution.getContest().getId(), true);
                if (contest == null) {
                    log.warn("Contest {} not found for solution {}", solution.getContest().getId(), solution.getId());
                    return;
                }
                if (contest.getProblems() == null) {
                    log.warn("Contest {} has no problems for solution {}", contest.getId(), solution.getId());
                    return;
                }
            for (ContestProblem cp : contest.getProblems()) {
                    if (cp != null && cp.getProblem() != null && 
                        cp.getProblem().getId().equals(solution.getProblem().getId())) {
                    contestProblemRepository.updateSubmittedNumber(cp.getId(), 1);
                    if (solution.getResult().equals(Solution.AC)) {
                        contestProblemRepository.updateAcceptedNumber(cp.getId(), 1);
                    }
                    break;
                }
                }
            } catch (Exception e) {
                log.error("Error in contestSolutionFilter for solution {}", solution.getId(), e);
                // Don't throw, as this is not critical for non-contest solutions
            }
        }
    }

    private void acceptSolutionFilter(Solution solution) {
        if (solution.getResult().equals(Solution.AC)) {
            try {
                if (solution.getUser() == null || solution.getProblem() == null) {
                    log.warn("Solution {} has null user or problem, skipping accept filter", solution.getId());
                    return;
                }
            if (!userProblemRepository.existsAllByUserAndProblem(solution.getUser(), solution.getProblem())) {
                    if (solution.getUser().getUserProfile() != null) {
                userProfileRepository.updateUserScore(solution.getUser().getUserProfile().getId(), solution.getProblem().getScore());
                userProfileRepository.updateUserAccepted(solution.getUser().getUserProfile().getId(), 1);
                    }
                problemRepository.updateAcceptedNumber(solution.getProblem().getId(), 1);
                userProblemRepository.save(new UserProblem(solution.getUser(), solution.getProblem()));
                    log.debug("Updated accept statistics for solution {}", solution.getId());
                }
            } catch (Exception e) {
                log.error("Error in acceptSolutionFilter for solution {}", solution.getId(), e);
                throw e;
            }
        }
    }

    @Data
    public static class SubmitCode {
        private int submit_id;
        private int problem_id;
        private int max_cpu_time;
        private int max_memory;
        private String memory_limit_check_only;
        private String src;
        private String seccomp_rule;
        private String run_command;
        private String compile_command;
        private String source;
        private String callback_url;

        public SubmitCode(int submit_id, int problem_id, int max_cpu_time, int max_memory, String memory_limit_check_only, String src, String seccomp_rule, String run_command, String compile_command, String source, String callback_url) {
            this.submit_id = submit_id;
            this.problem_id = problem_id;
            this.max_cpu_time = max_cpu_time;
            this.max_memory = max_memory;
            this.memory_limit_check_only = memory_limit_check_only;
            this.src = src;
            this.seccomp_rule = seccomp_rule;
            this.run_command = run_command;
            this.compile_command = compile_command;
            this.source = source;
            this.callback_url = callback_url;
        }

        @Override
        public String toString() {
            return "SubmitCode{" +
                    "submit_id=" + submit_id +
                    ", problem_id=" + problem_id +
                    ", max_cpu_time=" + max_cpu_time +
                    ", max_memory=" + max_memory +
                    ", src='" + src + '\'' +
                    ", seccomp_rule='" + seccomp_rule + '\'' +
                    ", run_command='" + run_command + '\'' +
                    ", compile_command='" + compile_command + '\'' +
                    ", callback_url='" + callback_url + '\'' +
                    ", source='" + source + '\'' +
                    '}';
        }
    }
}

@Data
class HostLoad {
    private String host;
    private Double score;
    private Instant last;
    private int cpu=4;

    HostLoad(String host) {
        this.host = host;
        this.score = 0.0;
        this.last = Instant.now();
    }

    double update(Problem p) {
        double dt = Duration.between(this.last, Instant.now()).toMillis();
        if (dt > 60000) {
            score = 0.0;
        } else {
            score = (score + p.getTimeLimit().doubleValue() / dt * p.getMemoryLimit().doubleValue()/1024/1024) / cpu;
        }
        last = Instant.now();
        return score;
    }
}