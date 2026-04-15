package cn.edu.zjnu.acm.controller;

import cn.edu.zjnu.acm.entity.Teacher;
import cn.edu.zjnu.acm.entity.User;
import cn.edu.zjnu.acm.entity.oj.*;
import cn.edu.zjnu.acm.exception.ForbiddenException;
import cn.edu.zjnu.acm.exception.NeedLoginException;
import cn.edu.zjnu.acm.exception.NotFoundException;
import cn.edu.zjnu.acm.repo.CommentRepository;
import cn.edu.zjnu.acm.repo.contest.ContestProblemRepository;
import cn.edu.zjnu.acm.service.*;
import cn.edu.zjnu.acm.util.Rank;
import cn.edu.zjnu.acm.util.RestfulResult;
import cn.edu.zjnu.acm.util.Result;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Controller
@RequestMapping("/contest")
class ContestViewController {
    @GetMapping
    public String contestPage() {
        return "contest/contests";
    }

    @GetMapping("/problem/{id:[0-9]+}")
    public String showContest(@PathVariable(value = "id") Long id) {
        return "contest/contestinfo";
    }

    @GetMapping("/status/{id:[0-9]+}")
    public String showContestStatus(@PathVariable(value = "id") Long id) {
        return "contest/conteststatus";
    }

    @GetMapping("/ranklist/{id:[0-9]+}")
    public String showContestRanklist(@PathVariable(value = "id") Long id) {
        return "contest/contestrank";
    }

    @GetMapping("/comment/{id:[0-9]+}")
    public String showContestComment(@PathVariable(value = "id") Long id) {
        return "contest/contestcomment";
    }

    @GetMapping("/{id:[0-9]+}")
    public String contestGate(@PathVariable(value = "id") Long id) {
        return "contest/contestgate";
    }

    @GetMapping("/create/{tid:[0-9]+}")
    public String createContest() {
        return "contest/create_contest";
    }

    @GetMapping("/edit/{tid:[0-9]+}")
    public String editContest() {
        return "contest/edit_contest";
    }
}

@Slf4j
@RestController
@CrossOrigin
@RequestMapping("/api/contest")
public class ContestController {
    private static final int PAGE_SIZE = 30;
    private final HttpSession session;
    private final UserService userService;
    private final ProblemService problemService;
    private final SolutionService solutionService;
    private final ContestService contestService;
    private final JudgeService judgeService;
    private final ContestProblemRepository contestProblemRepository;
    private final TeamService teamService;

    public ContestController(HttpSession session, UserService userService, ProblemService problemService, SolutionService solutionService, ContestService contestService, JudgeService judgeService, ContestProblemRepository contestProblemRepository, TeamService teamService, CommentRepository commentRepository) {
        this.session = session;
        this.userService = userService;
        this.problemService = problemService;
        this.solutionService = solutionService;
        this.contestService = contestService;
        this.judgeService = judgeService;
        this.contestProblemRepository = contestProblemRepository;
        this.teamService = teamService;
    }

    @GetMapping("")
    public Page<Contest> showContests(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "search", defaultValue = "") String search) {
        page = Math.max(0, page);
        // Page<Contest> currentPage = contestService.getContestWithoutTeam(page, PAGE_SIZE, search);
        Page<Contest> currentPage = contestService.getPublicContests(page, PAGE_SIZE, search);
        for (Contest c : currentPage.getContent()) {
            c.clearLazyRoles();
            c.setProblems(null);
            c.setSolutions(null);
            c.setContestComments(null);
            c.setPassword(null);
            c.setCreator(null);
            c.setFreezeRank(null);
            c.setCreateTime(null);
            if (c.getTeam() != null) {
                c.getTeam().setCreator(null);
                c.getTeam().clearLazyRoles();
            }
        }
        return currentPage;
    }

    @GetMapping("/clone/{id:[0-9]+}")
    public RestfulResult cloneContest(@PathVariable Long id) {
        Contest c = contestService.getContestById(id, true);
        if (c == null) {
            return new RestfulResult(404, "no contest found", null);
        }
        c.setSolutions(null);
        c.setContestComments(null);
        c.setCreator(null);
        c.setPassword(null);
        c.setTeam(null);
        return new RestfulResult(200, "success", c);
    }

    @GetMapping("/gate/{cid:[0-9]+}")
    public String contestReady(@PathVariable("cid") Long cid) {
        User user = (User) session.getAttribute("currentUser");
        if (user == null)
            throw new NeedLoginException();
        Contest contest = contestService.getContestById(cid);
        if (contest == null)
            throw new NotFoundException();
        if (!contest.isStarted())
            return "未开始 not started";
        if (userService.getUserPermission(user) == -1) {
            if (contest.getPrivilege().equals(Contest.TEAM)) {
                Team team = contest.getTeam();
                if (teamService.isUserInTeam(user, team)) {
                    return "success";
                } else {
                    return "没有权限";
                }
            }
        }
        return "success";
    }

    private Boolean isContestCreator(Contest contest, User currentUser) {
        if (contest == null) {
            throw new NotFoundException();
        }
        return contest.getCreator().getId() == currentUser.getId();
    }

    @GetMapping("/background/access/{cid:[0-9]+}")
    public String isAccessContestBackground(@PathVariable("cid") Long cid,
                                            @SessionAttribute User currentUser) {
        Contest contest = contestService.getContestById(cid);
        if (isContestCreator(contest, currentUser) ||
                userService.getUserPermission(currentUser) == Teacher.ADMIN) {
            return "success";
        }
        return "negative";
    }

    @GetMapping("/background/{cid:[0-9]+}")
    public Contest getUpdateContestInfo(@PathVariable("cid") Long cid,
                                        @SessionAttribute User currentUser) {
        Contest contest = contestService.getContestById(cid);
        if (!isContestCreator(contest, currentUser)) {
            throw new ForbiddenException();
        }
        contest.setCreator(null);
        contest.setProblems(null);
        contest.setSolutions(null);
        contest.setTeam(null);
        contest.setContestComments(null);
        return contest;
    }

    @PostMapping("/background/{cid:[0-9]+}")
    public String updateContest(@PathVariable("cid") Long cid,
                                @SessionAttribute User currentUser,
                                @RequestBody @Valid EditContest editContest) {
        Contest contest = contestService.getContestById(cid);
        if (!isContestCreator(contest, currentUser) && userService.getUserPermission(currentUser) != Teacher.ADMIN) {
            throw new ForbiddenException("Permission denied!");
        }
        contest.setTitle(editContest.getTitle());
        contest.setDescription(editContest.getDescription());
        if (!contest.getPrivilege().equals(Contest.TEAM)) {
            if (!editContest.getPrivilege().equals(Contest.TEAM)) {
                contest.setPrivilege(editContest.getPrivilege());
            }
        }
        contest.setStartAndEndTime(editContest.getStartTime(), editContest.getLength());
        if (contest.getPrivilege().equals(Contest.PRIVATE)) {
            contest.setPassword(editContest.getPassword());
        }
        contestService.saveContest(contest);
        return "success";
    }

    @GetMapping("/{cid:[0-9]+}")
    public Contest getContestDetail(@PathVariable("cid") Long cid,
                                    @RequestParam(value = "password", defaultValue = "") String password) {
        Contest c = contestService.getContestById(cid, false);
        if (c == null)
            throw new NotFoundException();
        Contest scontest = (Contest) session.getAttribute("contest" + c.getId());
        if (scontest == null || scontest.getId() != c.getId() || !c.isStarted()) {
            if (!c.isStarted() ||
                    (c.getPassword() != null && c.getPassword().length() > 0 &&
                            c.getPrivilege().equals("private") &&
                            !c.getPassword().equals(password))) {
                c.clearLazyRoles();
                c.setProblems(null);
                c.setSolutions(null);
                c.setContestComments(null);
                c.setCreator(null);
                c.setFreezeRank(null);
                c.setCreateTime(null);
                c.setPassword("password");
                return c;
            } else {
                session.setAttribute("contest" + c.getId(), c);
            }
        }
        try {
            c = contestService.getContestById(cid, true);
            c.setSolutions(null);
            c.setCreator(null);
            c.setCreateTime(null);
            c.setPassword(null);
            c.setFreezeRank(null);
            c.setCreateTime(null);
            c.setContestComments(null);
            c.setTeam(null);
            for (ContestProblem cp : c.getProblems()) {
                Problem p = cp.getProblem();
                p.setId(null);
                p.setAccepted(null);
                p.setSubmitted(null);
                p.setAccepted(null);
                p.setTags(null);
                p.setTitle(null);
                p.setScore(null);
                p.setSource(null);
            }
            c.getProblems().sort((a, b) -> (int) (a.getTempId() - b.getTempId()));
        } catch (Exception e) {
            throw new NotFoundException();
        }
        return c;
    }

    /**
     * 检查代码格式，验证是否包含主程序入口
     * 这是一个非强制性的检查，只记录警告，不阻止提交
     */
    /**
     * 检查代码格式，复用 ProblemController 的检查逻辑
     * 返回 CodeFormatCheckResult，严重问题需要直接拒绝提交
     */
    private ProblemController.CodeFormatCheckResult checkCodeFormat(String source, String language) {
        // 直接调用 ProblemController 的静态方法（如果存在）或复用相同逻辑
        // 为了保持一致性，这里实现相同的检查逻辑
        if (source == null || source.trim().isEmpty()) {
            return ProblemController.CodeFormatCheckResult.ok();
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
                if (sourceLower.contains(" main(") || sourceLower.contains(" main (") ||
                        sourceLower.contains("int main(") || sourceLower.contains("void main(")) {
                    hasMainEntry = true;
                }
                if (sourceLower.contains("scanf(") || sourceLower.contains("printf(") ||
                        sourceLower.contains(" cin") || sourceLower.contains("cout") ||
                        sourceLower.contains("std::cin") || sourceLower.contains("std::cout")) {
                    hasIoCall = true;
                }
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
                if (sourceLower.contains("public static void main")) {
                    hasMainEntry = true;
                }
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
                if (sourceLower.contains("if __name__") && sourceLower.contains("__main__")) {
                    hasMainEntry = true;
                }
                if (sourceLower.contains("input(") || sourceLower.contains("raw_input(") ||
                        sourceLower.contains("sys.stdin")) {
                    hasIoCall = true;
                }
                if (sourceLower.contains("def ") || sourceLower.contains("class ")) {
                    hasFunctionLike = true;
                }
                if (!hasMainEntry && !hasIoCall && hasFunctionLike) {
                    severe = true;
                    warning = "检测到你的 Python 代码可能只定义了函数/类，而没有主程序入口和标准输入输出。" +
                            "本 OJ 不会自动调用你的函数，请提交完整程序，例如包含 if __name__ == '__main__': 并从标准输入读取数据。";
                } else if (!hasMainEntry && !hasIoCall) {
                    warning = "代码中可能缺少主程序入口（if __name__ == '__main__'）或标准输入处理。请确保提交完整的可执行程序。";
                }
                break;
            case "go":
                if (sourceLower.contains("func main()") || sourceLower.contains("func main (")) {
                    hasMainEntry = true;
                }
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
                return ProblemController.CodeFormatCheckResult.ok();
        }

        if (warning != null) {
            log.warn("Code format check for language {} (severe={}): {}", language, severe, warning);
            return ProblemController.CodeFormatCheckResult.error(severe, warning);
        }
        return ProblemController.CodeFormatCheckResult.ok();
    }

    @PostMapping("/submit/{pid:[0-9]+}/{cid:[0-9]+}")
    public Result submitProblemInContest(@PathVariable("pid") Long pid,
                                         @PathVariable("cid") Long cid,
                                         HttpServletRequest request,
                                         @RequestBody ProblemController.SubmitCodeObject submitCodeObject) {
        log.info("Submit: {}", Instant.now());
        String source = submitCodeObject.getSource();
        boolean share = submitCodeObject.isShare();
        String language = submitCodeObject.getLanguage();
        String _temp = ProblemController.checkSubmitFrequncy(session, source);
        if (_temp != null)
            return new Result(403, _temp);
        @NotNull User user;
        try {
            user = (User) session.getAttribute("currentUser");
            if (user == null || userService.getUserById(user.getId()) == null) {// user doesn't login
                throw new NeedLoginException();
            }
        } catch (NeedLoginException e) {
            throw e;
        } catch (Exception e) {
            throw new NeedLoginException();
        }
        try {
            Contest contest = contestService.getContestById(cid);
            if (contest == null) {
                return new Result(404, "Contest Not Found");
            }
            Contest scontest = (Contest) session.getAttribute("contest" + cid);
            if (scontest == null || scontest.getId() != contest.getId()) {
                return new Result(403, "Need attendance!");
            }
            if (contest.isEnded() || !contest.isStarted()) {
                return new Result(403, "The contest is not Running!");
            }
            ContestProblem cproblem = contestProblemRepository.findByContestAndTempId(contest, pid);
            if (cproblem == null) {
                return new Result(404, "Problem Not Exist");
            }
            Problem problem = cproblem.getProblem();
            // 检查代码格式
            ProblemController.CodeFormatCheckResult formatResult = checkCodeFormat(source, language);
            if (formatResult != null && formatResult.isSevere() && formatResult.getMessage() != null) {
                // 严重格式问题：直接拒绝提交，避免用户误以为是算法错误
                return new Result(400, formatResult.getMessage());
            }
            Solution solution = new Solution(user, problem, language, source, request.getRemoteAddr(), share);
            solution.setContest(contest);
            solution = solutionService.insertSolution(solution);
            if (solution == null) {
                log.error("Failed to insert solution for user: {}, problem: {}, contest: {}", user.getId(), pid, cid);
                return new Result(500, "Failed to save solution");
            }
            try {
                String result = judgeService.submitCode(solution);
                if (result == null) {
                    log.error("Judge service returned null for solution: {}", solution.getId());
                    return new Result(500, "Judge service unavailable");
                }
            } catch (IllegalArgumentException e) {
                log.error("Invalid language or configuration for contest submission: {}", e.getMessage());
                return new Result(400, "Invalid language: " + e.getMessage());
            } catch (IllegalStateException e) {
                log.error("Judge service state error for contest submission", e);
                return new Result(503, "Judge service unavailable: " + e.getMessage());
            } catch (Exception e) {
                log.error("Error submitting code to judge service in contest", e);
                return new Result(500, "Failed to submit code to judge service: " + e.getMessage());
            }
            return RestfulResult.successResult();
        } catch (NeedLoginException e) {
            throw e;
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error submitting problem in contest", e);
            return new Result(500, "Internal server error: " + e.getMessage());
        }
    }

    @PostMapping("/comments/post/{cid:[0-9]+}")
    public String postComments(@PathVariable(value = "cid") Long cid, @RequestBody CommentPost commentPost) {
        try {
            if (commentPost.replyText.length() < 4) return "too short";
            User user = (User) session.getAttribute("currentUser");
            if (user == null) return "need login";
            ContestComment father = contestService.getFatherComment(commentPost.getReplyId());
            @NotNull Contest contest = contestService.getContestById(cid);
            if (!contest.isStarted() || contest.isEnded())
                return "contest is not running";
            ContestComment contestComment = new ContestComment(user, commentPost.replyText, father, contest);
            contestService.postComment(contestComment);
            return "success";
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "failed";
    }

    @GetMapping("/comments/{cid:[0-9]+}")
    public List<ContestComment> getCommentsOfContest(@PathVariable Long cid) {
        try {
            @NotNull Contest contest = contestService.getContestById(cid, false);
            if (!contest.isStarted())
                throw new NotFoundException();
            List<ContestComment> contestComments = contestService.getCommentsOfContest(contest);
            contestComments.forEach(c -> c.getUser().hideInfo());
            return contestComments;
        } catch (Exception e) {
            throw new NotFoundException();
        }
    }

    @GetMapping("/status/{cid:[0-9]+}")
    public Page<Solution> getUserSolutions(@PathVariable("cid") Long cid,
                                           @RequestParam(value = "page", defaultValue = "0") int page) {
        log.info("getUserSolutions called: contestId={}, page={}", cid, page);
        try {
            Contest contest = contestService.getContestById(cid, true);
            if (contest == null) {
                log.warn("Contest not found: contestId={}", cid);
                throw new NotFoundException("Contest not found");
            }
            if (!contest.isStarted()) {
                log.warn("Contest not started: contestId={}, startTime={}, currentTime={}", 
                        cid, contest.getStartTime(), Instant.now());
                throw new NotFoundException("Contest not started");
            }
            User user = (User) session.getAttribute("currentUser");
            if (user == null) {
                log.warn("User not logged in for contest status: contestId={}", cid);
                throw new NeedLoginException();
            }
            log.info("Getting solutions for user: userId={}, contestId={}, page={}, pageSize={}", 
                    user.getId(), cid, page, PAGE_SIZE);
            Page<Solution> solutions = solutionService.getSolutionsOfUserInContest(page, PAGE_SIZE, user, contest);
            log.info("Query returned: totalElements={}, totalPages={}, numberOfElements={}, empty={}", 
                    solutions.getTotalElements(), solutions.getTotalPages(), 
                    solutions.getNumberOfElements(), solutions.isEmpty());
            Map<Long, ContestProblem> cpmap = new HashMap<>();
            for (ContestProblem cp : contest.getProblems()) {
                cpmap.put(cp.getProblem().getId(), cp);
            }
            for (Solution s : solutions.getContent()) {
                s.setSource(null);
                s.setIp(null);
                s.getUser().setEmail(null);
                s.getUser().setPassword(null);
                s.getUser().setIntro(null);
                s.getUser().setUserProfile(null);
                Problem tp = Problem.jsonReturnProblemFactory();
                ContestProblem cp = cpmap.get(s.getProblem().getId());
                if (cp != null) {
                    tp.setId(cp.getTempId());
                }
                s.setProblem(tp);
                s.setContest(null);
            }
            log.info("Returning {} solutions for user {} in contest {}", 
                    solutions.getContent().size(), user.getId(), cid);
            return solutions;
        } catch (NeedLoginException | NotFoundException e) {
            log.warn("Exception in getUserSolutions: contestId={}, exception={}, message={}", 
                    cid, e.getClass().getSimpleName(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error getting user solutions for contest: contestId={}, page={}", cid, page, e);
            log.error("Exception stack trace:", e);
            throw new NotFoundException("Error retrieving solutions");
        }
    }

    @GetMapping("/status/{cid:[0-9]+}/all")
    public Page<Solution> getAllSolutionsInContest(@PathVariable("cid") Long cid,
                                                    @RequestParam(value = "page", defaultValue = "0") int page) {
        log.info("getAllSolutionsInContest called: contestId={}, page={}", cid, page);
        try {
            User currentUser = (User) session.getAttribute("currentUser");
            if (currentUser == null) {
                log.warn("User not logged in for contest status (all): contestId={}", cid);
                throw new NeedLoginException();
            }
            int permission = userService.getUserPermission(currentUser);
            if (permission != Teacher.ADMIN) {
                log.warn("User {} attempted to access all solutions in contest {} without admin permission, permission={}", 
                        currentUser.getId(), cid, permission);
                throw new ForbiddenException("Only administrators can view all solutions");
            }
            Contest contest = contestService.getContestById(cid, true);
            if (contest == null) {
                log.warn("Contest not found: contestId={}", cid);
                throw new NotFoundException("Contest not found");
            }
            if (!contest.isStarted()) {
                log.warn("Contest not started: contestId={}, startTime={}, currentTime={}", 
                        cid, contest.getStartTime(), Instant.now());
                throw new NotFoundException("Contest not started");
            }
            log.info("Admin {} getting all solutions in contest {}, page={}, pageSize={}", 
                    currentUser.getId(), cid, page, PAGE_SIZE);
            Page<Solution> solutions = solutionService.getSolutionsInContest(page, PAGE_SIZE, contest);
            log.info("Query returned: totalElements={}, totalPages={}, numberOfElements={}, empty={}", 
                    solutions.getTotalElements(), solutions.getTotalPages(), 
                    solutions.getNumberOfElements(), solutions.isEmpty());
            Map<Long, ContestProblem> cpmap = new HashMap<>();
            for (ContestProblem cp : contest.getProblems()) {
                cpmap.put(cp.getProblem().getId(), cp);
            }
            for (Solution s : solutions.getContent()) {
                s.setSource(null);
                s.setIp(null);
                s.getUser().setEmail(null);
                s.getUser().setPassword(null);
                s.getUser().setIntro(null);
                s.getUser().setUserProfile(null);
                Problem tp = Problem.jsonReturnProblemFactory();
                ContestProblem cp = cpmap.get(s.getProblem().getId());
                if (cp != null) {
                    tp.setId(cp.getTempId());
                }
                s.setProblem(tp);
                s.setContest(null);
            }
            log.info("Returning {} solutions (all) for contest {}", solutions.getContent().size(), cid);
            return solutions;
        } catch (NeedLoginException | NotFoundException | ForbiddenException e) {
            log.warn("Exception in getAllSolutionsInContest: contestId={}, exception={}, message={}", 
                    cid, e.getClass().getSimpleName(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error getting all solutions for contest: contestId={}, page={}", cid, page, e);
            log.error("Exception stack trace:", e);
            throw new NotFoundException("Error retrieving solutions");
        }
    }

    @PostMapping("/complete/{cid:[0-9]+}")
    public Result completeContest(@PathVariable("cid") Long cid) {
        try {
            User user = (User) session.getAttribute("currentUser");
            if (user == null) {
                log.warn("User not logged in for completing contest: {}", cid);
                throw new NeedLoginException();
            }
            Contest contest = contestService.getContestById(cid);
            if (contest == null) {
                log.warn("Contest not found for completion: {}", cid);
                return new Result(404, "Contest not found");
            }
            if (!contest.isStarted() || contest.isEnded()) {
                log.warn("Contest not running for completion: {}", cid);
                return new Result(403, "Contest is not running");
            }
            // Check if user has attended the contest
            Contest scontest = (Contest) session.getAttribute("contest" + cid);
            if (scontest == null || scontest.getId() != contest.getId()) {
                log.warn("User {} has not attended contest {}", user.getId(), cid);
                return new Result(403, "Need attendance!");
            }
            // Check if already completed
            if (contestService.isUserCompleted(contest, user)) {
                log.info("User {} already completed contest {}", user.getId(), cid);
                return new Result(200, "Already completed");
            }
            contestService.completeContest(contest, user);
            log.info("User {} completed contest {}", user.getId(), cid);
            return new Result(200, "Contest completed successfully");
        } catch (NeedLoginException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error completing contest {}", cid, e);
            return new Result(500, "Internal server error: " + e.getMessage());
        }
    }

    @GetMapping("/complete/{cid:[0-9]+}")
    public Result checkCompletionStatus(@PathVariable("cid") Long cid) {
        try {
            User user = (User) session.getAttribute("currentUser");
            if (user == null) {
                throw new NeedLoginException();
            }
            Contest contest = contestService.getContestById(cid);
            if (contest == null) {
                return new Result(404, "Contest not found");
            }
            boolean completed = contestService.isUserCompleted(contest, user);
            return new Result(200, completed ? "completed" : "not completed");
        } catch (NeedLoginException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error checking completion status for contest {}", cid, e);
            return new Result(500, "Internal server error");
        }
    }

    @GetMapping("/ranklist/{cid:[0-9]+}")
    @Cacheable(value = "contestRank", key = "#cid")
    public Map<String, Object> getRankOfContest(@PathVariable Long cid) {
        try {
            @NotNull Contest contest = contestService.getContestById(cid, true);
            contest.setTeam(null);
            @NotNull Rank rank = new Rank(contest);
            @NotNull List<Solution> solutions = solutionService.getSolutionsInContest(contest);
            for (int i = solutions.size() - 1; i >= 0; i--) {
                rank.update(solutions.get(i));
            }
            Map<String, Object> result = new HashMap<>();
            result.put("problemsNumber", rank.getProblemsNumber());
            result.put("rows", rank.getRows());
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        throw new NotFoundException();
    }

    @PostMapping("/create")
    public String insertContestAction(@RequestBody @Valid CreateContest postContest
            , @SessionAttribute User currentUser) {
        try {
            if (currentUser == null)
                throw new NeedLoginException();
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            LocalDateTime localDateTime = LocalDateTime.parse(postContest.getStartTime(), dtf);
            Instant startTime = Instant.from(localDateTime.atZone(ZoneId.systemDefault()));
            Instant endTime = startTime.plusSeconds(60 * postContest.getLength());
            List<ContestProblem> contestProblems = new ArrayList<>();
            long cnt = 1L;
            for (CreateContest.CreateProblem cp : postContest.getProblems()) {
                Problem p = problemService.getProblemById(cp.getId());
                if (p == null)
                    return "problem error";
                contestProblems.add(new ContestProblem(p, cp.getTempTitle(), cnt++));
            }
            Contest contest = new Contest(postContest.getTitle(),
                    postContest.getDescription(),
                    postContest.getPrivilege(),
                    postContest.getPassword(),
                    startTime, endTime, Instant.now());
            contest.setCreator(currentUser);
            // 默认设置为待审核状态，必须通过管理员审核后才能在前台显示
            contest.setStatus(Contest.Status.PENDING);
            if (postContest.getPrivilege().equals(Contest.TEAM)) {
                Team team = teamService.getTeamById(postContest.getTid());
                if (team == null)
                    throw new NotFoundException();
                contest.setTeam(team);
            } else {
                contest.setTeam(null);
            }
            contest.setSolutions(null);
            contest = contestService.saveContest(contest);
            for (ContestProblem cp : contestProblems) {
                cp.setContest(contest);
                contestProblemRepository.save(cp);
            }
            return "success";
        } catch (Exception e) {
//            e.printStackTrace();
        }
        return "failed";
    }

    @Data
    public static class CommentPost {
        String replyText = "";
        Long replyId = 0L;

        public CommentPost() {
        }

        public Long getReplyId() {
            return replyId == null ? 0L : replyId;
        }
    }

    @Data
    static class EditContest {
        @Size(min = 1, max = 255, message = "比赛标题长度必须在1-255个字符之间")
        String title;
        String description;
        String privilege;
        @Size(max = 200, message = "比赛密码长度不能超过200个字符")
        String password;
        String startTime;
        Long length;

        public EditContest() {
        }
    }

    @Data
    static class CreateContest {
        @Size(min = 1, max = 255, message = "比赛标题长度必须在1-255个字符之间")
        private String title;
        private String description;
        private String privilege;
        @Size(max = 200, message = "比赛密码长度不能超过200个字符")
        private String password;
        private String startTime;
        private Long length;
        private Long tid;
        private ArrayList<CreateProblem> problems;

        public CreateContest() {
        }

        @Data
        static class CreateProblem {
            private Long id;
            private String tempTitle;
            private String name;

            public CreateProblem() {
            }
        }
    }
  /*  @GetMapping("/rejudge/{cid}")
    public String Rejudge(@PathVariable Long cid) {
        Contest contest = contestService.getContestById(cid);
        RejudgeThread rejudgeThread = new RejudgeThread();
        List<Solution> solutions = solutionService.getSolutionsInContest(contest);
        if (!contest.getPattern().equals("acm")) {
            solutions.sort((o1, o2) -> (int) (o2.getId() - o1.getId()));
        }
        rejudgeThread.solutions = solutions;
        rejudgeThread.run();
        return "Running";
    }*/
}