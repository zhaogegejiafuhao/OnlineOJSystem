package cn.edu.zjnu.acm.service;

import cn.edu.zjnu.acm.entity.User;
import cn.edu.zjnu.acm.entity.oj.*;
import cn.edu.zjnu.acm.repo.problem.AnalysisCommentRepository;
import cn.edu.zjnu.acm.repo.problem.AnalysisRepository;
import cn.edu.zjnu.acm.repo.problem.ProblemRepository;
import cn.edu.zjnu.acm.repo.problem.TagRepository;
import cn.edu.zjnu.acm.repo.user.UserProblemRepository;
import cn.edu.zjnu.acm.util.PageHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ProblemService {
    private final ProblemRepository problemRepository;
    private final TagRepository tagRepository;
    private final UserProblemRepository userProblemRepository;
    private final AnalysisRepository analysisRepository;
    private final AnalysisCommentRepository analysisCommentRepository;

    public ProblemService(ProblemRepository problemRepository, TagRepository tagRepository, UserProblemRepository userProblemRepository, AnalysisCommentRepository analysisCommentRepository, AnalysisRepository analysisRepository) {
        this.problemRepository = problemRepository;
        this.tagRepository = tagRepository;
        this.userProblemRepository = userProblemRepository;
        this.analysisCommentRepository = analysisCommentRepository;
        this.analysisRepository = analysisRepository;
    }

    private String getTestDataBasePath() {
        // 优先使用环境变量
        String envPath = System.getenv("TEST_DATA_PATH");
        if (envPath != null && !envPath.isEmpty()) {
            return envPath;
        }
        // 检查是否在 oj 容器内运行（oj 容器映射：D:\OnlineOJData:/onlinejudge）
        File ojContainerPath = new File("/onlinejudge/ojdata");
        if (ojContainerPath.exists() || new File("/onlinejudge").exists()) {
            // oj 容器内路径：/onlinejudge/ojdata
            // 这个路径会通过 Docker volume 映射到 Windows 的 D:\OnlineOJData\ojdata
            return "/onlinejudge/ojdata";
        }
        // 检查是否在 judger 容器内运行（judger 容器映射：D:\OnlineOJData\ojdata:/ojdata）
        File judgerContainerPath = new File("/ojdata");
        if (judgerContainerPath.exists() && judgerContainerPath.isDirectory()) {
            return "/ojdata";
        }
        // Windows 开发环境默认路径（直接运行，不在容器内）
        String windowsPath = "D:\\OnlineOJData\\ojdata";
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            return windowsPath;
        }
        // Linux 环境默认路径
        return "/ojdata";
    }

    @PostConstruct
    public void initializeTags() {
        String[] names = {"初级",
                "中级",
                "高级",
                "数据结构",
                "动态规划",
                "搜索",
                "图论",
                "概率论",
                "数论",
                "字符串",
                "计算几何"
        };
        for (String s : names) {
            Tag t = new Tag(s);
            try {
                tagRepository.save(t);
            }
            catch (Exception e){
                ;
            }
        }
        // 启动时为所有现有题目创建缺失的测试数据目录
        createMissingTestDataDirectories();
    }

    public Page<Problem> getAllActiveProblems(int page, int size) {
        return problemRepository.findProblemsByStatus(PageRequest.of(page, size), Problem.Status.APPROVED);
    }

    public Page<Problem> getAllProblems(int page, int size, String search) {
        return problemRepository.findAllByTitleContaining(PageRequest.of(page, size), search);
    }

    public List<Problem> getProblemList() {
        return problemRepository.findAll();
    }

    public Page<Problem> getByTagName(int page, int size, List<String> tagNames, List<Problem> problems) {
        problems = new ArrayList<>(problems);
        for (String tagName : tagNames) {
            Tag tag = tagRepository.findByName(tagName).orElse(null);
            if (tag == null) {
                continue;
            }
            List<Problem> _tags = problemRepository.findAllByTags(tag);
            problems.retainAll(_tags);
        }
        page = Math.min((problems.size() - 1) / size, page);
        return new PageHolder<Problem>(problems, PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id")));
    }

    public Page<Problem> searchActiveProblem(int page, int size, String search, boolean allInOnePage) {
        List<Problem> problems = new LinkedList<>();
        try {
            Long pid = Long.parseLong(search);
            problems.addAll(problemRepository.findProblemsByStatusAndId(Problem.Status.APPROVED, pid));
        } catch (Exception e) {
            log.debug("parse int failed");
        }
        try {
            problems.addAll(problemRepository.findAllByStatusAndTitleContaining(Problem.Status.APPROVED, search));
        } catch (Exception e) {
            log.debug("search problem by title failed");
        }
        HashSet<Problem> set = new HashSet<>(problems);
        List<Problem> _problems = new ArrayList<>(set);
        if (allInOnePage) {
            size = Math.max(_problems.size(), 1);
            page = 0;
        }
        return new PageHolder<>(_problems, PageRequest.of(page, size));
    }

    public Problem getActiveProblemById(Long id) {
        return problemRepository.findProblemByIdAndStatus(id, Problem.Status.APPROVED).orElse(null);
    }

    public List<Tag> getAllTags() {
        return tagRepository.findAll();
    }

    public Problem insertNewProblem(Problem problem) {
        Problem savedProblem = problemRepository.save(problem);
        // 自动创建测试数据目录
        createTestDataDirectory(savedProblem.getId());
        return savedProblem;
    }
    
    /**
     * 为题目创建测试数据目录
     * @param problemId 题目ID
     */
    private void createTestDataDirectory(Long problemId) {
        try {
            String testDataBasePath = getTestDataBasePath();
            Path problemDir = Paths.get(testDataBasePath, String.valueOf(problemId));
            
            // 如果目录不存在，创建它
            if (!Files.exists(problemDir)) {
                Files.createDirectories(problemDir);
                log.info("Created test data directory for problem {}: {}", problemId, problemDir);
                
                // 创建一个示例测试文件（可选）
                Path sampleInput = problemDir.resolve("1.in");
                Path sampleOutput = problemDir.resolve("1.out");
                
                if (!Files.exists(sampleInput)) {
                    Files.createFile(sampleInput);
                    log.debug("Created sample input file: {}", sampleInput);
                }
                if (!Files.exists(sampleOutput)) {
                    Files.createFile(sampleOutput);
                    log.debug("Created sample output file: {}", sampleOutput);
                }
            } else {
                log.debug("Test data directory already exists for problem {}: {}", problemId, problemDir);
            }
        } catch (IOException e) {
            log.error("Failed to create test data directory for problem {}", problemId, e);
            // 不抛出异常，避免影响题目创建流程
        }
    }
    
    /**
     * 为所有现有题目创建缺失的测试数据目录（用于修复现有数据）
     */
    public void createMissingTestDataDirectories() {
        try {
            String testDataBasePath = getTestDataBasePath();
            Path baseDir = Paths.get(testDataBasePath);
            
            // 确保基础目录存在
            if (!Files.exists(baseDir)) {
                Files.createDirectories(baseDir);
                log.info("Created base test data directory: {}", baseDir);
            }
            
            List<Problem> allProblems = problemRepository.findAll();
            int created = 0;
            for (Problem problem : allProblems) {
                try {
                    Path problemDir = baseDir.resolve(String.valueOf(problem.getId()));
                    if (!Files.exists(problemDir)) {
                        createTestDataDirectory(problem.getId());
                        created++;
                    }
                } catch (Exception e) {
                    log.error("Failed to create test data directory for problem {}", problem.getId(), e);
                }
            }
            if (created > 0) {
                log.info("Created {} missing test data directories for existing problems", created);
            } else {
                log.debug("All problems already have test data directories");
            }
        } catch (Exception e) {
            log.error("Failed to create missing test data directories", e);
        }
    }

    public Boolean isProblemRepeated(String title) {
        return problemRepository.findByTitle(title).isPresent();
    }

    public Problem getProblemById(Long id) {
        return problemRepository.findById(id).orElse(null);
    }

    public List<Tag> convertString2Tag(String s) {
        String[] ts = s.split("[,，]");
        ArrayList<Tag> tags = new ArrayList<>();
        for (int i = 0; i < ts.length; i++) {
            Tag t = tagRepository.findByName(ts[i]).orElse(null);
            if (t != null) {
                tags.add(t);
            }
        }
        return tags;
    }

    public Tag getTagByName(String name) {
        return tagRepository.findByName(name).orElse(null);
    }

    public Boolean isUserAcProblem(User user, Problem problem) {
        return userProblemRepository.existsAllByUserAndProblem(user, problem);
    }

    public List<Problem> allUserAcProblems(User user) {
        return userProblemRepository.findAllByUser(user).stream()
                .map(UserProblem::getProblem).collect(Collectors.toList());
    }

    public List<Analysis> getAnalysisByProblem(Problem problem) {
        List<Analysis> analyses = analysisRepository.findAllByProblem(problem);
        analyses.forEach(a -> a.setComment(analysisCommentRepository.findAllByAnalysis(a)));
        return analyses;
    }

    public Analysis getAnalysisById(Long id) {
        return analysisRepository.findById(id).orElse(null);
    }

    public Analysis postAnalysis(Analysis analysis) {
        return analysisRepository.save(analysis);
    }

    public AnalysisComment postAnalysisComment(AnalysisComment comment) {
        return analysisCommentRepository.save(comment);
    }

    public AnalysisComment getFatherComment(Long id) {
        return analysisCommentRepository.findById(id).orElse(null);
    }

    public Integer countSolveProblemByTag(User user, Tag tag, boolean isScore) {
        return isScore ?
                userProblemRepository.userSolveTagScore(user.getId(), tag.getId()) :
                userProblemRepository.userSolveTagCount(user.getId(), tag.getId());
    }
    
    /**
     * Create problem with DRAFT status for normal admins
     */
    public Problem createDraftProblem(Problem problem) {
        problem.setStatus(Problem.Status.DRAFT);
        problem.setSubmitted(0);
        problem.setAccepted(0);
        return problemRepository.save(problem);
    }
    
    /**
     * Get problem submission statistics
     */
    public Map<String, Object> getProblemStatistics(Long problemId) {
        Problem problem = problemRepository.findById(problemId).orElse(null);
        if (problem == null) {
            return null;
        }
        Map<String, Object> stats = new HashMap<>();
        stats.put("problem_id", problem.getId());
        stats.put("title", problem.getTitle());
        stats.put("submitted", problem.getSubmitted());
        stats.put("accepted", problem.getAccepted());
        stats.put("acceptance_rate", problem.getRatio());
        // 可以添加更多统计信息
        return stats;
    }
    
    /**
     * Get user's own problems (for normal admins)
     */
    public Page<Problem> getUserProblems(User user, int page, int size) {
        // 这里需要根据实际情况实现，可能需要在Problem实体中添加creator字段
        // 暂时返回所有草稿状态的题目
        return problemRepository.findProblemsByStatus(PageRequest.of(page, size), Problem.Status.DRAFT);
    }
}
