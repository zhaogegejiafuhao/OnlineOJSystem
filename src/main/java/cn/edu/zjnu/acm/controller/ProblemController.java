package cn.edu.zjnu.acm.controller;

import cn.edu.zjnu.acm.entity.User;
import cn.edu.zjnu.acm.entity.oj.Analysis;
import cn.edu.zjnu.acm.entity.oj.AnalysisComment;
import cn.edu.zjnu.acm.entity.oj.Problem;
import cn.edu.zjnu.acm.entity.oj.Solution;
import cn.edu.zjnu.acm.exception.ForbiddenException;
import cn.edu.zjnu.acm.exception.NeedLoginException;
import cn.edu.zjnu.acm.exception.NotFoundException;
import cn.edu.zjnu.acm.repo.user.UserProblemRepository;
import cn.edu.zjnu.acm.service.JudgeService;
import cn.edu.zjnu.acm.service.ProblemService;
import cn.edu.zjnu.acm.service.SolutionService;
import cn.edu.zjnu.acm.service.UserService;
import cn.edu.zjnu.acm.util.RestfulResult;
import cn.edu.zjnu.acm.util.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@Slf4j
@RequestMapping("/problems")
@Controller
class ProblemViewController {
    @GetMapping
    public String problemsList() {
        return "problem/problemlist";
    }

    @GetMapping("/{id:[0-9]+}")
    public String showProblem(@PathVariable Long id) {
        return "problem/showproblem";
    }

    @GetMapping("/article/{id:[0-9]+}")
    public String problemArticle() {
        return "problem/article";
    }

    @GetMapping("/article/edit/{id:[0-9]+}")
    public String editArticle() {
        return "problem/edit_analysis";
    }
}

@Slf4j
@RestController
@CrossOrigin
@RequestMapping("/api/problems")
public class ProblemController {
    private static final int PAGE_SIZE = 30;
    private final ProblemService problemService;
    private final UserService userService;
    private final JudgeService judgeService;
    private final SolutionService solutionService;
    private final HttpSession session;

    public ProblemController(ProblemService problemService, UserProblemRepository userProblemRepository, UserService userService, JudgeService judgeService, SolutionService solutionService, HttpSession session) {
        this.problemService = problemService;
        this.userService = userService;
        this.judgeService = judgeService;
        this.solutionService = solutionService;
        this.session = session;
    }

    static String checkSubmitFrequncy(HttpSession session, String source) {
        if (session.getAttribute("last_submit") != null) {
            Instant instant = (Instant) session.getAttribute("last_submit");
            if (Instant.now().minusSeconds(10).compareTo(instant) < 0) {
                return "Don't submitted within 10 seconds";
            } else if (source.length() > 20000) {
                return "Source code too long";
            } else if (source.length() < 2) {
                return "Source code too short";
            }
        }
        session.setAttribute("last_submit", Instant.now());
        return null;
    }

    private Problem checkProblemExist(Long pid) {
        Problem problem = problemService.getProblemById(pid);
        if (problem == null) {
            throw new NotFoundException("No Problem Found");
        }
        return problem;
    }

    @GetMapping("")
    public RestfulResult showProblemList(@RequestParam(value = "page", defaultValue = "0") int page,
                                         @RequestParam(value = "search", defaultValue = "") String search) {
        page = Math.max(page, 0);
        Page<Problem> problemPage;
        if (search != null && search.length() > 0) {
            int spl = search.lastIndexOf("$$");
            if (spl >= 0) {
                String tags = search.substring(spl + 2);
                search = search.substring(0, spl);
                String[] tagNames = tags.split(",");
                List<Problem> _problems = problemService.searchActiveProblem(0, 1, search, true).getContent();
                problemPage = problemService.getByTagName(page, PAGE_SIZE, Arrays.asList(tagNames), _problems);
            } else {
                problemPage = problemService.searchActiveProblem(page, PAGE_SIZE, search, false);
            }
        } else {
            problemPage = problemService.getAllActiveProblems(page, PAGE_SIZE);
        }
        for (Problem p : problemPage.getContent()) {
            p.setInput(null);
            p.setOutput(null);
            p.setHint(null);
            p.setSource(null);
            p.setSampleInput(null);
            p.setSampleOutput(null);
        }
        return new RestfulResult(200, "success", problemPage);
    }

    @GetMapping("/{id:[0-9]+}")
    public RestfulResult showProblem(@PathVariable Long id) {
        Problem problem = problemService.getActiveProblemById(id);
        if (problem == null)
            throw new NotFoundException();
        return new RestfulResult(200, "success", problem);
    }

    @GetMapping("/name/{id:[0-9]+}")
    public String getProblemName(@PathVariable(value = "id") Long id) {
        try {
            return ((Problem) showProblem(id).getData()).getTitle();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 代码格式检查结果
     * severe=true 表示严重问题（只提交函数/缺少入口与输入输出），需要直接拒绝本次提交
     * severe=false 且 message!=null 表示仅作为警告记录日志，不阻止提交
     */
    public static class CodeFormatCheckResult {
        private final boolean severe;
        private final String message;

        private CodeFormatCheckResult(boolean severe, String message) {
            this.severe = severe;
            this.message = message;
        }

        public boolean isSevere() {
            return severe;
        }

        public String getMessage() {
            return message;
        }

        public static CodeFormatCheckResult ok() {
            return new CodeFormatCheckResult(false, null);
        }

        public static CodeFormatCheckResult error(boolean severe, String message) {
            return new CodeFormatCheckResult(severe, message);
        }
    }

    /**
     * 检查代码格式，验证是否包含主程序入口以及标准输入输出处理
     * - 对 Python 语言：若高度疑似只提交了函数/类定义，则返回严重错误，必须拒绝提交
     * - 对其他语言：只要发现 main/入口函数即视为通过，否则仅记录警告，不强制拒绝
     */
    private CodeFormatCheckResult checkCodeFormat(String source, String language) {
        if (source == null || source.trim().isEmpty()) {
            return CodeFormatCheckResult.ok();
        }
        String sourceLower = source.toLowerCase();

        boolean hasMainEntry = false;
        boolean hasIoCall = false;
        boolean hasFunctionLike = false;
        String warning = null;
        boolean severe = false;

        switch (language) {
            case "c":
            case "cpp":
                // 入口：main 函数
                if (sourceLower.contains(" main(") || sourceLower.contains(" main (") ||
                        sourceLower.contains("int main(") || sourceLower.contains("void main(")) {
                    hasMainEntry = true;
                }
                // 标准 IO：scanf/printf/cin/cout
                if (sourceLower.contains("scanf(") || sourceLower.contains("printf(") ||
                        sourceLower.contains(" cin") || sourceLower.contains("cout") ||
                        sourceLower.contains("std::cin") || sourceLower.contains("std::cout")) {
                    hasIoCall = true;
                }
                // 大致判断是否写了函数/声明
                if (sourceLower.contains("void ") || sourceLower.contains("int ") ||
                        sourceLower.contains("double ") || sourceLower.contains("bool ")) {
                    hasFunctionLike = true;
                }
                if (!hasMainEntry && !hasIoCall && hasFunctionLike) {
                    // 对 C/C++ 做严格拦截：高度疑似只提交了函数/模板
                    severe = true;
                    warning = "检测到你的 C/C++ 代码可能只写了函数或模板，而没有 main() 和标准输入输出。" +
                            "本 OJ 不会自动调用你的函数，请提交完整可执行程序，包含 main() 函数并处理标准输入输出。";
                } else if (!hasMainEntry) {
                    warning = "代码中可能缺少 main() 函数。请确保提交完整的可执行程序。";
                }
                break;
            case "java":
                // 入口：public static void main
                if (sourceLower.contains("public static void main")) {
                    hasMainEntry = true;
                }
                // 标准 IO：Scanner/System.in/System.out
                if (sourceLower.contains("scanner ") || sourceLower.contains(" new scanner(") ||
                        sourceLower.contains("system.in") || sourceLower.contains("system.out.print")) {
                    hasIoCall = true;
                }
                if (sourceLower.contains(" class ") || sourceLower.contains("interface ")) {
                    hasFunctionLike = true;
                }
                if (!hasMainEntry && !hasIoCall && hasFunctionLike) {
                    // 对 Java 做严格拦截：高度疑似只提交了类/方法定义
                    severe = true;
                    warning = "检测到你的 Java 代码可能只写了类/方法定义，而没有 public static void main(String[] args) 和标准输入输出。" +
                            "本 OJ 不会自动调用你的方法，请提交完整可执行程序，包含 main 方法并处理标准输入输出。";
                } else if (!hasMainEntry) {
                    warning = "代码中可能缺少 public static void main(String[] args) 方法。请确保提交完整的可执行程序。";
                }
                break;
            case "py2":
            case "py3":
                // 入口：if __name__ == "__main__"
                if (sourceLower.contains("if __name__") && sourceLower.contains("__main__")) {
                    hasMainEntry = true;
                }
                // 标准 IO：input/raw_input/sys.stdin
                if (sourceLower.contains("input(") || sourceLower.contains("raw_input(") ||
                        sourceLower.contains("sys.stdin")) {
                    hasIoCall = true;
                }
                // 函数/类定义
                if (sourceLower.contains("def ") || sourceLower.contains("class ")) {
                    hasFunctionLike = true;
                }
                if (!hasMainEntry && !hasIoCall && hasFunctionLike) {
                    // 对 Python 做严格拦截：高度疑似只提交了函数（如 LeetCode 风格）
                    severe = true;
                    warning = "检测到你的 Python 代码可能只定义了函数/类，而没有主程序入口和标准输入输出。" +
                            "本 OJ 不会自动调用你的函数，请提交完整程序，例如包含 if __name__ == '__main__': 并从标准输入读取数据。";
                } else if (!hasMainEntry && !hasIoCall) {
                    warning = "代码中可能缺少主程序入口（if __name__ == '__main__'）或标准输入处理。请确保提交完整的可执行程序。";
                }
                break;
            case "go":
                // 入口：func main()
                if (sourceLower.contains("func main()") || sourceLower.contains("func main (")) {
                    hasMainEntry = true;
                }
                // 标准 IO：fmt.Scan / fmt.Println
                if (sourceLower.contains("fmt.scan") || sourceLower.contains("fmt.fscan") ||
                        sourceLower.contains("fmt.println") || sourceLower.contains("fmt.printf")) {
                    hasIoCall = true;
                }
                if (sourceLower.contains("func ")) {
                    hasFunctionLike = true;
                }
                if (!hasMainEntry && !hasIoCall && hasFunctionLike) {
                    // 对 Go 做严格拦截：高度疑似只提交了函数定义
                    severe = true;
                    warning = "检测到你的 Go 代码可能只写了函数，而没有 func main() 和标准输入输出。" +
                            "本 OJ 不会自动调用你的函数，请提交完整可执行程序，包含 func main() 并处理标准输入输出。";
                } else if (!hasMainEntry) {
                    warning = "代码中可能缺少 func main() 函数。请确保提交完整的可执行程序。";
                }
                break;
            default:
                // 未知语言，不检查
                return CodeFormatCheckResult.ok();
        }

        if (warning != null) {
            log.warn("Code format check for language {} (severe={}): {}", language, severe, warning);
            return new CodeFormatCheckResult(severe, warning);
        }
        return CodeFormatCheckResult.ok();
    }

    @PostMapping("/submit/{id:[0-9]+}")
    public Result submitProblem(@PathVariable("id") Long id,
                                @RequestBody SubmitCodeObject submitCodeObject,
                                HttpServletRequest request) {
        String source = submitCodeObject.getSource();
        boolean share = submitCodeObject.isShare();
        String language = submitCodeObject.getLanguage();
        String _temp = checkSubmitFrequncy(session, source);
        if (_temp != null)
            return new Result(403, _temp);
        User user = (User) session.getAttribute("currentUser");
        if (user == null || userService.getUserById(user.getId()) == null) {
            throw new NeedLoginException();
        }
        Problem problem = problemService.getActiveProblemById(id);
        if (problem == null) {
            throw new NotFoundException("Problem Not Exist");
        }
        // 检查代码格式
        CodeFormatCheckResult formatResult = checkCodeFormat(source, language);
        if (formatResult != null && formatResult.isSevere() && formatResult.getMessage() != null) {
            // 严重格式问题：直接拒绝提交，避免用户误以为是算法错误
            return new Result(400, formatResult.getMessage());
        }
        //null检验完成
        Solution solution = solutionService.insertSolution(new Solution(user, problem, language, source, request.getRemoteAddr(), share));
        if (solution == null) {
            log.error("Failed to insert solution for user: {}, problem: {}", user.getId(), id);
            return new Result(400, "Failed to save solution");
        }
        try {
            String result = judgeService.submitCode(solution);
            if (result == null) {
                log.error("Judge service returned null for solution: {}", solution.getId());
                return new Result(500, "Judge service unavailable");
            }
            return new Result(200, "success");
        } catch (IllegalArgumentException e) {
            log.error("Invalid language or configuration: {}", e.getMessage());
            return new Result(400, "Invalid language: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error submitting code to judge service", e);
            return new Result(500, "Internal error: " + e.getMessage());
        }
    }

    @GetMapping("/tags")
    public RestfulResult showTags() {
        return new RestfulResult(200, "success", problemService.getAllTags());
    }

    @GetMapping("/is/accepted/{pid:[0-9]+}")
    public RestfulResult checkUserHasAc(@SessionAttribute User currentUser, @PathVariable Long pid) {
        Problem problem = checkProblemExist(pid);
        return new RestfulResult(200,
                "success",
                problemService.isUserAcProblem(currentUser, problem));
    }

    @GetMapping("/analysis/{pid:[0-9]+}")
    public RestfulResult getProblemArticle(@PathVariable Long pid,
                                           @SessionAttribute User currentUser) {
        Problem problem = checkProblemExist(pid);
        if (userService.getUserPermission(currentUser) == -1) {
            if (!problemService.isUserAcProblem(currentUser, problem)) {
                throw new ForbiddenException("Access after passing the question");
            }
        }
        List<Analysis> analyses = problemService.getAnalysisByProblem(problem);
        analyses.forEach(a -> a.getUser().hideInfo());
        return new RestfulResult(200, "success", analyses);
    }

    @PostMapping("/analysis/post/{pid:[0-9]+}")
    public RestfulResult postAnalysis(@PathVariable Long pid,
                                      @SessionAttribute User currentUser,
                                      @RequestBody @Validated Analysis analysis) {
        Problem problem = checkProblemExist(pid);
        if (userService.getUserPermission(currentUser) == -1) {
            if (!problemService.isUserAcProblem(currentUser, problem)) {
                throw new ForbiddenException("Access after passing the question");
            }
        }
        analysis.setUser(currentUser);
        analysis.setComment(null);
        analysis.setPostTime(Instant.now());
        analysis.setProblem(problem);
        problemService.postAnalysis(analysis);
        return new RestfulResult(200, "success", null);
    }

    @GetMapping("/analysis/edit/{aid:[0-9]+}")
    public RestfulResult getOneAnalysis(@PathVariable Long aid, @SessionAttribute User currentUser) {
        Analysis analysis = getAnalysisById(aid, currentUser);
        analysis.getUser().hideInfo();
        analysis.setProblem(null);
        analysis.setComment(null);
        return new RestfulResult(200, "success", analysis);
    }

    private Analysis getAnalysisById(Long aid, User currentUser) {
        Analysis analysis = problemService.getAnalysisById(aid);
        if (analysis == null) {
            throw new NotFoundException("Analysis not found");
        }
        if (userService.getUserPermission(currentUser) == -1) {
            if (analysis.getUser().getId() != currentUser.getId()) {
                throw new ForbiddenException("Permission denied");
            }
        }
        return analysis;
    }

    @PostMapping("/analysis/edit/{aid:[0-9]+}")
    public RestfulResult editAnalysis(@PathVariable Long aid,
                                      @SessionAttribute User currentUser,
                                      @RequestBody @Validated Analysis postAnalysis) {
        Analysis analysis = getAnalysisById(aid, currentUser);
        analysis.setText(postAnalysis.getText());
        problemService.postAnalysis(analysis);
        return new RestfulResult(200, "success", null);
    }

    @PostMapping("/analysis/post/comment/{aid:[0-9]+}")
    public RestfulResult postAnalysisComment(@PathVariable Long aid,
                                             @SessionAttribute User currentUser,
                                             @RequestBody ContestController.CommentPost commentPost) {
        if (commentPost.replyText.length() < 4) {
            return new RestfulResult(400, "bad request", "too short!");
        }
        Analysis analysis = problemService.getAnalysisById(aid);
        if (analysis == null) {
            throw new NotFoundException("Analysis not found");
        }
        if (userService.getUserPermission(currentUser) == -1) {
            if (!problemService.isUserAcProblem(currentUser,
                    checkProblemExist(analysis.getProblem().getId()))) {
                throw new ForbiddenException("Access after passing the question");
            }
        }
        AnalysisComment father = problemService.getFatherComment(commentPost.getReplyId());
        problemService.postAnalysisComment(new AnalysisComment(currentUser, commentPost.replyText, father, analysis));
        return new RestfulResult(200, "success", null);
    }

    public static class SubmitCodeObject {
        private String source;
        private String language;
        private String share;

        public SubmitCodeObject() {
        }

        public Boolean isShare() {
            return share.equals("true");
        }

        public void setShare(String share) {
            this.share = share;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public String getLanguage() {
            return language;
        }

        public void setLanguage(String language) {
            this.language = language;
        }
    }
}
