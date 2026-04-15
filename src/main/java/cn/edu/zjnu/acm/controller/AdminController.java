package cn.edu.zjnu.acm.controller;

import cn.edu.zjnu.acm.config.Config;
import cn.edu.zjnu.acm.config.GlobalStatus;
import cn.edu.zjnu.acm.entity.Teacher;
import cn.edu.zjnu.acm.entity.User;
import cn.edu.zjnu.acm.entity.oj.Contest;
import cn.edu.zjnu.acm.entity.oj.ContestProblem;
import cn.edu.zjnu.acm.entity.oj.Problem;
import cn.edu.zjnu.acm.entity.oj.Solution;
import cn.edu.zjnu.acm.entity.oj.Tag;
import cn.edu.zjnu.acm.exception.ForbiddenException;
import cn.edu.zjnu.acm.exception.NotFoundException;
import cn.edu.zjnu.acm.repo.contest.ContestProblemRepository;
import cn.edu.zjnu.acm.repo.contest.ContestRepository;
import cn.edu.zjnu.acm.repo.problem.AnalysisRepository;
import cn.edu.zjnu.acm.repo.problem.ProblemRepository;
import cn.edu.zjnu.acm.repo.problem.SolutionRepository;
import cn.edu.zjnu.acm.repo.problem.TagRepository;
import cn.edu.zjnu.acm.repo.user.TeacherRepository;
import cn.edu.zjnu.acm.repo.user.UserProblemRepository;
import cn.edu.zjnu.acm.repo.user.UserProfileRepository;
import cn.edu.zjnu.acm.service.ContestService;
import cn.edu.zjnu.acm.service.DataService;
import cn.edu.zjnu.acm.service.BackupService;
import cn.edu.zjnu.acm.service.LogService;
import cn.edu.zjnu.acm.service.ProblemService;
import cn.edu.zjnu.acm.service.SolutionService;
import cn.edu.zjnu.acm.service.UserService;
import cn.edu.zjnu.acm.util.RestfulResult;
import lombok.Data;

import javax.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@RestController
@RequestMapping("/api/admin")
@Slf4j
public class AdminController {
    public static final int PAGE_SIZE = 50;
    private static final ExecutorService threadPool = Executors.newFixedThreadPool(200);
    private final UserProblemRepository userProblemRepository;
    private final ProblemService problemService;
    private final ContestService contestService;
    private final UserService userService;
    private final HttpSession session;
    private final ProblemRepository problemRepository;
    private final Config config;
    private final SolutionService solutionService;
    private final ContestProblemRepository contestProblemRepository;
    private final SolutionRepository solutionRepository;
    private final UserProfileRepository userProfileRepository;
    private final AnalysisRepository analysisRepository;
    private final TeacherRepository teacherRepository;
    private final TagRepository tagRepository;
    private final ContestRepository contestRepository;
    private final DataService dataService;
    private final BackupService backupService;
    private final LogService logService;

    public AdminController(UserProblemRepository userProblemRepository, ProblemService problemService, ContestService contestService, UserService userService, HttpSession session, Config config, SolutionService solutionService, ProblemRepository problemRepository, ContestProblemRepository contestProblemRepository, SolutionRepository solutionRepository, UserProfileRepository userProfileRepository, AnalysisRepository analysisRepository, TeacherRepository teacherRepository, TagRepository tagRepository, ContestRepository contestRepository, DataService dataService, BackupService backupService, LogService logService) {
        this.userProblemRepository = userProblemRepository;
        this.problemService = problemService;
        this.contestService = contestService;
        this.userService = userService;
        this.session = session;
        this.config = config;
        this.solutionService = solutionService;
        this.problemRepository = problemRepository;
        this.contestProblemRepository = contestProblemRepository;
        this.solutionRepository = solutionRepository;
        this.userProfileRepository = userProfileRepository;
        this.analysisRepository = analysisRepository;
        this.teacherRepository = teacherRepository;
        this.tagRepository = tagRepository;
        this.contestRepository = contestRepository;
        this.dataService = dataService;
        this.backupService = backupService;
        this.logService = logService;
    }

    @GetMapping("/config")
    public UpdateConfig getConfig() {
        return new UpdateConfig(config);
    }

    @PostMapping("/config")
    public String updateConfig(@RequestBody UpdateConfig updateConfig) {
        log.info(updateConfig.toString());
        config.setLeastScoreToSeeOthersCode(updateConfig.getLeastScoreToSeeOthersCode());
        config.setLeastScoreToPostBlog(updateConfig.getLeastScoreToPostBlog());
        config.setJudgerhost(updateConfig.getJudgerhost());
        config.setC(updateConfig.getC());
        config.setCpp(updateConfig.getCpp());
        config.setJava(updateConfig.getJava());
        config.setPython2(updateConfig.getPython2());
        config.setPython3(updateConfig.getPython3());
        config.setGo(updateConfig.getGo());
        config.setNotice(updateConfig.getNotice());
        return "success";
    }

    @GetMapping("/problem")
    public Page<Problem> getAllProblems(@RequestParam(value = "page", defaultValue = "0") int page,
                                        @RequestParam(value = "search", defaultValue = "") String search) {
        page = Math.max(page, 0);
        Page<Problem> problemPage;
        problemPage = problemService.getAllProblems(page, PAGE_SIZE, search);
        return problemPage;
    }

    @GetMapping("/contest")
    public RestfulResult getAllContest(@RequestParam(value = "page", defaultValue = "0") int page,
                                       @RequestParam(value = "search", defaultValue = "") String search) {
        page = Math.max(page, 0);
        Page<Contest> contestPage = contestService.getContestPage(page, PAGE_SIZE, search);
        contestPage.getContent().forEach(contest -> {
            contest.getCreator().hideInfo();
            contest.clearLazyRoles();
        });
        return new RestfulResult(200, "success", contestPage);
    }

    @GetMapping("/user")
    public RestfulResult getAllUsers(@RequestParam(value = "page", defaultValue = "0") int page,
                                     @RequestParam(value = "search", defaultValue = "") String search) {
        Page<User> users = userService.searchUser(page, PAGE_SIZE, search);
        users.getContent().forEach(u -> u.setPassword(null));
        return new RestfulResult(200, "success", users);
    }

    @GetMapping("/user/reset/{uid:[0-9]+}")
    public RestfulResult resetUserPassword(@PathVariable Long uid) {
        User u = userService.getUserById(uid);
        String pwd = "000000";
        if (u == null) {
            throw new NotFoundException("no user found");
        }
        u = userService.setUserPassword(u, pwd);
        userService.updateUserInfo(u);
        return new RestfulResult(200, "success", "reset password:" + pwd);
    }

    @GetMapping("/user/teacher")
    public RestfulResult manageTeachers() {
        List<Teacher> teachers = teacherRepository.findAll();
        teachers.forEach(t -> {
            t.setUser(userService.getUserById(t.getUser().getId()));
            t.getUser().hideInfo();
        });
        return new RestfulResult(200, "success", teachers);
    }

    @PostMapping("/user/teacher/{uid:[0-9]+}")
    public RestfulResult addTeacher(@PathVariable Long uid) {
        User user = userService.getUserById(uid);
        if (user == null) {
            throw new NotFoundException();
        }
        if (userService.getUserPermission(user) != -1) {
            return new RestfulResult(200, "已经是Teacher!", null);
        }
        Teacher teacher = new Teacher(user, Teacher.TEACHER);
        teacherRepository.save(teacher);
        return RestfulResult.successResult();
    }

    @DeleteMapping("/user/teacher/{uid:[0-9]+}")
    @Transactional
    public RestfulResult deleteTeacher(@PathVariable Long uid, @SessionAttribute User currentUser) {
        if (userService.getUserPermission(currentUser) != Teacher.ADMIN) {
            throw new ForbiddenException("Only Administrator");
        }
        User user = userService.getUserById(uid);
        if (user == null || userService.getUserPermission(user) == -1) {
            throw new NotFoundException();
        }
        if (user.getId() == currentUser.getId()) {
            return new RestfulResult(400, "You cannot remove yourself");
        }
        teacherRepository.deleteByUser(user);
        return RestfulResult.successResult();
    }
    
    @DeleteMapping("/user/{uid:[0-9]+}")
    @Transactional
    public RestfulResult deleteUser(@PathVariable Long uid, @SessionAttribute User currentUser) {
        if (userService.getUserPermission(currentUser) != Teacher.ADMIN) {
            throw new ForbiddenException("Only Administrator can delete users");
        }
        User user = userService.getUserById(uid);
        if (user == null) {
            throw new NotFoundException("User not found");
        }
        if (user.getId() == currentUser.getId()) {
            return new RestfulResult(400, "You cannot delete yourself");
        }
        try {
            userService.deleteUser(user);
            return RestfulResult.successResult();
        } catch (Exception e) {
            return new RestfulResult(500, "Delete failed: " + e.getMessage());
        }
    }
    
    @DeleteMapping("/users")
    @Transactional
    public RestfulResult deleteUsers(@RequestBody List<Long> userIds, @SessionAttribute User currentUser) {
        if (userService.getUserPermission(currentUser) != Teacher.ADMIN) {
            throw new ForbiddenException("Only Administrator can delete users");
        }
        try {
            int deletedCount = userService.deleteUsers(userIds);
            return new RestfulResult(200, "success", deletedCount);
        } catch (Exception e) {
            return new RestfulResult(500, "Delete failed: " + e.getMessage());
        }
    }
    
    @PostMapping("/user/add")
    @Transactional
    public RestfulResult addUser(@RequestBody @Valid User user, @SessionAttribute User currentUser) {
        if (userService.getUserPermission(currentUser) != Teacher.ADMIN) {
            throw new ForbiddenException("Only Administrator can add users");
        }
        try {
            User newUser = userService.addUser(user);
            if (newUser == null) {
                return new RestfulResult(400, "Username already exists");
            }
            return new RestfulResult(200, "success", newUser);
        } catch (Exception e) {
            return new RestfulResult(500, "Add failed: " + e.getMessage());
        }
    }
    
    @PostMapping("/user/edit/{uid:[0-9]+}")
    @Transactional
    public RestfulResult updateUser(@RequestBody @Valid User user, @PathVariable Long uid, @SessionAttribute User currentUser) {
        if (userService.getUserPermission(currentUser) != Teacher.ADMIN) {
            throw new ForbiddenException("Only Administrator can edit users");
        }
        try {
            User existingUser = userService.getUserById(uid);
            if (existingUser == null) {
                return new RestfulResult(404, "User not found");
            }
            user.setId(uid);
            User updatedUser = userService.updateUser(user);
            if (updatedUser == null) {
                return new RestfulResult(404, "User not found");
            }
            return new RestfulResult(200, "success", updatedUser);
        } catch (Exception e) {
            return new RestfulResult(500, "Edit failed: " + e.getMessage());
        }
    }

    @PostMapping("/problem/insert")
    public String addProblem(@RequestBody JsonProblem problem) {
        if (problemService.isProblemRepeated(problem.getTitle())) {
            return "Problem name already existed!";
        }
        Problem p = new Problem(problem.getTitle(), problem.getDescription(),
                problem.getInput(), problem.getOutput(), problem.getSampleInput(),
                problem.getSampleOutput(), problem.getHint(), problem.getSource(),
                problem.getTime(), problem.getMemory(), problem.getScore());
        if (Boolean.TRUE.equals(problem.getActive())) {
            // 如果勾选了Active，则变为PENDING状态，等待管理员审核
            p.setStatus(Problem.Status.PENDING);
        } else {
            // 否则为DRAFT状态
            p.setStatus(Problem.Status.DRAFT);
        }
        p.setTags(problemService.convertString2Tag(problem.getTags()));
        problemService.insertNewProblem(p);
        return "success";
    }

    @PostMapping("/problem/edit/{pid:[0-9]+}")
    public String updateProblem(@RequestBody JsonProblem problem, @PathVariable("pid") Long pid) {
        Problem p = problemService.getProblemById(pid);
        if (null == p) {
            return "Problem not existed!";
        }
        p.setTitle(problem.getTitle());
        p.setDescription(problem.getDescription());
        p.setInput(problem.getInput());
        p.setOutput(problem.getOutput());
        p.setSampleInput(problem.getSampleInput());
        p.setSampleOutput(problem.getSampleOutput());
        p.setHint(problem.getHint());
        p.setSource(problem.getSource());
        p.setTimeLimit(problem.getTime());
        p.setMemoryLimit(problem.getMemory());
        p.setScore(problem.getScore());
        if (Boolean.TRUE.equals(problem.getActive())) {
            // 如果勾选了Active，则变为PENDING状态，等待管理员审核
            p.setStatus(Problem.Status.PENDING);
        } else {
            // 否则为DRAFT状态
            p.setStatus(Problem.Status.DRAFT);
        }
        p.setTags(problemService.convertString2Tag(problem.getTags()));
        problemService.insertNewProblem(p);
        return "success";
    }

    @GetMapping("/problem/{id:[0-9]+}")
    public Problem showProblem(@PathVariable Long id) {
        Problem problem = problemService.getProblemById(id);
        if (problem == null)
            throw new NotFoundException();
        return problem;
    }

    @DeleteMapping("/problem/{id:[0-9]+}")
    @Transactional
    public RestfulResult deleteProblem(@SessionAttribute User currentUser, @PathVariable Long id) {
        if (userService.getUserPermission(currentUser) != Teacher.ADMIN) {
            throw new ForbiddenException("Only Administrator can access");
        }
        Problem problem = problemRepository.findById(id).orElse(null);
        if (problem == null) {
            throw new NotFoundException();
        }
        solutionRepository.deleteAllByProblem(problem);
        analysisRepository.deleteAllByProblem(problem);
        userProblemRepository.deleteAllByProblem(problem);
        contestProblemRepository.deleteAllByProblem(problem);
        problemRepository.delete(problem);
        return new RestfulResult(200, "success", null);
    }

    @GetMapping("/correctData")
    public String calculateData() {
        try {
            User user = (User) session.getAttribute("currentUser");
            log.info("calculating data by user:" + user.getUsername());
            Thread main = new Thread(() -> {
                Thread threadProblem = new Thread(this::calcProblem);
                Thread threadContest = new Thread(this::calcContest);
                Thread threadUser = new Thread(this::calcUser);
                threadContest.start();
                threadProblem.start();
                threadUser.start();
                try {
                    threadContest.join();
                    threadProblem.join();
                    threadUser.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    log.info("calculating finished");
                    GlobalStatus.maintaining = false;
                }
            });
            GlobalStatus.maintaining = true;
            main.start();
            return "将持续一段时间, it will cost a long long time...";
        } catch (Exception e) {
            log.info("exception catched");
        }
        return "failed";
    }

    @Transactional
    void calcUser() {
        log.info("calculating on user");
        List<User> userList = userService.userList();
        int cnt = 0;
        for (User u : userList) {
            Future f = threadPool.submit(() -> {
                long submitted = solutionRepository.countAllByUser(u);
                long accepted = userProblemRepository.countAllByUser(u);
                Long userId = u.getId();
                Long scoreLong = userProblemRepository.calculateUserScore(userId);
                int score = scoreLong.intValue();
                userProfileRepository.setUserSubmitted(u.getUserProfile().getId(), (int) submitted);
                userProfileRepository.setUserAccepted(u.getUserProfile().getId(), (int) accepted);
                userProfileRepository.setUserScore(u.getUserProfile().getId(), score);
            });
            if (cnt == userList.size() - 1) {
                try {
                    f.get();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
            ++cnt;
        }
        log.info("calculating on user finished");
    }

    @Transactional
    void calcProblem() {
        log.info("calculating on problem");
        List<Problem> problemList = problemService.getProblemList();
        int cnt = 0;
        for (Problem p : problemList) {
            Future f = threadPool.submit(() -> {
                problemRepository.setSubmittedNumber(p.getId(), solutionService.countOfProblem(p).intValue());
                long acceptedCount = userProblemRepository.countAllByProblem(p);
                problemRepository.setAcceptedNumber(p.getId(), (int) acceptedCount);
            });
            if (cnt == problemList.size() - 1) {
                try {
                    f.get();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
            ++cnt;
        }
        log.info("calculating on problem finished");
    }

    @Transactional
    void calcContest() {
        log.info("calculating on contest");
        List<Contest> contestList = contestService.getContestList();
        int cnt = 0;
        for (Contest c : contestList) {
            Future f = threadPool.submit(() -> {
                List<ContestProblem> contestProblemList = contestProblemRepository.findAllByContest(c);
                for (ContestProblem cp : contestProblemList) {
                    cp.setSubmitted(solutionService.countOfProblemContest(cp.getProblem(), c).intValue());
                    cp.setAccepted(solutionService.countAcOfProblemContest(cp.getProblem(), c).intValue());
                    contestProblemRepository.save(cp);
                }
            });
            if (cnt == contestList.size() - 1) {
                try {
                    f.get();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
            ++cnt;
        }
        log.info("calculating on contest finished");
    }

    @GetMapping("/maintain")
    public String maintainSwich() {
        GlobalStatus.teacherOnly ^= true;
        User user = (User) session.getAttribute("currentUser");
        if (user == null) {
            throw new ForbiddenException();
        }
        log.info(user.getId() + "set status: teacherOnly to " + GlobalStatus.teacherOnly);
        return GlobalStatus.teacherOnly ? "maintaining now" : "not maintaining now";
    }

    @GetMapping("/tag")
    public RestfulResult getAllTags() {
        return new RestfulResult(200, RestfulResult.SUCCESS, problemService.getAllTags());
    }

    @PostMapping("/tag/add")
    public RestfulResult addTag(@RequestBody Map<String, String> tagmap) {
        String tagname = tagmap.getOrDefault("tagname", "").trim();
        if (tagname.length() == 0) {
            return new RestfulResult(400, "标签名称不能为空");
        }
        if (tagname.length() > 200) {
            return new RestfulResult(400, "标签名称长度不能超过200个字符");
        }
        Tag tag = tagRepository.findByName(tagname).orElse(null);
        if (tag == null) {
            tagRepository.save(new Tag(tagname));
            return RestfulResult.successResult();
        }
        return new RestfulResult(400, "标签已存在");
    }

    @GetMapping("/problem/pending")
    public RestfulResult getPendingProblems(@RequestParam(value = "page", defaultValue = "0") int page) {
        page = Math.max(page, 0);
        Page<Problem> problemPage = problemRepository.findByStatus(org.springframework.data.domain.PageRequest.of(page, PAGE_SIZE), Problem.Status.PENDING);
        return new RestfulResult(200, "success", problemPage);
    }

    @GetMapping("/contest/pending")
    public RestfulResult getPendingContests(@RequestParam(value = "page", defaultValue = "0") int page) {
        page = Math.max(page, 0);
        Page<Contest> contestPage = contestRepository.findByStatus(org.springframework.data.domain.PageRequest.of(page, PAGE_SIZE), Contest.Status.PENDING);
        contestPage.getContent().forEach(contest -> {
            contest.getCreator().hideInfo();
            contest.clearLazyRoles();
        });
        return new RestfulResult(200, "success", contestPage);
    }

    @PostMapping("/problem/approve/{pid:[0-9]+}")
    public RestfulResult approveProblem(@PathVariable Long pid) {
        Problem problem = problemRepository.findById(pid).orElse(null);
        if (problem == null) {
            throw new NotFoundException("Problem not found");
        }
        problem.setStatus(Problem.Status.APPROVED);
        problemRepository.save(problem);
        return RestfulResult.successResult();
    }

    @PostMapping("/problem/reject/{pid:[0-9]+}")
    public RestfulResult rejectProblem(@PathVariable Long pid) {
        Problem problem = problemRepository.findById(pid).orElse(null);
        if (problem == null) {
            throw new NotFoundException("Problem not found");
        }
        problem.setStatus(Problem.Status.REJECTED);
        problemRepository.save(problem);
        return RestfulResult.successResult();
    }

    @PostMapping("/contest/approve/{cid:[0-9]+}")
    public RestfulResult approveContest(@PathVariable Long cid) {
        Contest contest = contestRepository.findById(cid).orElse(null);
        if (contest == null) {
            throw new NotFoundException("Contest not found");
        }
        contest.setStatus(Contest.Status.APPROVED);
        contestRepository.save(contest);
        return RestfulResult.successResult();
    }

    @PostMapping("/contest/reject/{cid:[0-9]+}")
    public RestfulResult rejectContest(@PathVariable Long cid) {
        Contest contest = contestRepository.findById(cid).orElse(null);
        if (contest == null) {
            throw new NotFoundException("Contest not found");
        }
        contest.setStatus(Contest.Status.REJECTED);
        contestRepository.save(contest);
        return RestfulResult.successResult();
    }

    @PostMapping("/contest/end/{cid:[0-9]+}")
    public RestfulResult forceEndContest(@PathVariable Long cid) {
        Contest contest = contestRepository.findById(cid).orElse(null);
        if (contest == null) {
            throw new NotFoundException("Contest not found");
        }
        contest.setEndTime(java.time.Instant.now());
        contestRepository.save(contest);
        return RestfulResult.successResult();
    }
    
    @GetMapping("/monitor/judgers")
    public RestfulResult getJudgerStatus(@SessionAttribute User currentUser) {
        if (userService.getUserPermission(currentUser) != Teacher.ADMIN) {
            throw new ForbiddenException("Only Administrator can access monitor");
        }
        List<Map<String, Object>> judgerStatus = new ArrayList<>();
        // 暂时返回空列表，实际项目中需要从JudgeService获取
        return new RestfulResult(200, "success", judgerStatus);
    }
    
    @GetMapping("/monitor/queue")
    public RestfulResult getSubmissionQueue(@SessionAttribute User currentUser) {
        if (userService.getUserPermission(currentUser) != Teacher.ADMIN) {
            throw new ForbiddenException("Only Administrator can access monitor");
        }
        // 这里需要实现获取提交队列的逻辑
        // 暂时返回空列表，实际项目中需要从数据库或缓存中获取待判题的提交
        List<Solution> pendingSolutions = solutionRepository.findByResultOrderByIdAsc(0);
        return new RestfulResult(200, "success", pendingSolutions);
    }
    
    @GetMapping("/monitor/system")
    public RestfulResult getSystemStatus(@SessionAttribute User currentUser) {
        if (userService.getUserPermission(currentUser) != Teacher.ADMIN) {
            throw new ForbiddenException("Only Administrator can access monitor");
        }
        Map<String, Object> systemStatus = new HashMap<>();
        systemStatus.put("version", "1.0.0");
        systemStatus.put("timestamp", Instant.now());
        systemStatus.put("maintaining", GlobalStatus.maintaining);
        systemStatus.put("teacher_only", GlobalStatus.teacherOnly);
        // 可以添加更多系统状态信息
        return new RestfulResult(200, "success", systemStatus);
    }
    
    @PostMapping("/problem/draft")
    public RestfulResult createDraftProblem(@RequestBody JsonProblem problem, @SessionAttribute User currentUser) {
        if (userService.getUserPermission(currentUser) == -1) {
            throw new ForbiddenException("Only Admin can create problems");
        }
        if (problemService.isProblemRepeated(problem.getTitle())) {
            return new RestfulResult(400, "Problem name already existed!");
        }
        Problem p = new Problem(problem.getTitle(), problem.getDescription(),
                problem.getInput(), problem.getOutput(), problem.getSampleInput(),
                problem.getSampleOutput(), problem.getHint(), problem.getSource(),
                problem.getTime(), problem.getMemory(), problem.getScore());
        p.setTags(problemService.convertString2Tag(problem.getTags()));
        p = problemService.createDraftProblem(p);
        return new RestfulResult(200, "success", p);
    }
    
    @GetMapping("/problem/statistics/{pid:[0-9]+}")
    public RestfulResult getProblemStatistics(@PathVariable Long pid, @SessionAttribute User currentUser) {
        if (userService.getUserPermission(currentUser) == -1) {
            throw new ForbiddenException("Only Admin can view statistics");
        }
        Map<String, Object> stats = problemService.getProblemStatistics(pid);
        if (stats == null) {
            throw new NotFoundException("Problem not found");
        }
        return new RestfulResult(200, "success", stats);
    }
    
    @GetMapping("/problems/own")
    public RestfulResult getOwnProblems(@RequestParam(value = "page", defaultValue = "0") int page, @SessionAttribute User currentUser) {
        if (userService.getUserPermission(currentUser) == -1) {
            throw new ForbiddenException("Only Admin can access");
        }
        Page<Problem> problems = problemService.getUserProblems(currentUser, page, PAGE_SIZE);
        return new RestfulResult(200, "success", problems);
    }
    
    @PostMapping("/contest/add")
    @Transactional
    public RestfulResult addContest(@RequestBody @Valid Contest contest, @SessionAttribute User currentUser) {
        if (userService.getUserPermission(currentUser) != Teacher.ADMIN) {
            throw new ForbiddenException("Only Administrator can add contests");
        }
        try {
            contest.setCreator(currentUser);
            contest.setStatus(Contest.Status.APPROVED);
            Contest newContest = contestRepository.save(contest);
            return new RestfulResult(200, "success", newContest);
        } catch (Exception e) {
            return new RestfulResult(500, "Add failed: " + e.getMessage());
        }
    }
    
    @PostMapping("/contest/edit/{cid:[0-9]+}")
    @Transactional
    public RestfulResult updateContest(@RequestBody @Valid Contest contest, @PathVariable Long cid, @SessionAttribute User currentUser) {
        if (userService.getUserPermission(currentUser) != Teacher.ADMIN) {
            throw new ForbiddenException("Only Administrator can edit contests");
        }
        try {
            Contest existingContest = contestRepository.findById(cid).orElse(null);
            if (existingContest == null) {
                return new RestfulResult(404, "Contest not found");
            }
            existingContest.setTitle(contest.getTitle());
            existingContest.setDescription(contest.getDescription());
            existingContest.setStatus(contest.getStatus());
            Contest updatedContest = contestRepository.save(existingContest);
            return new RestfulResult(200, "success", updatedContest);
        } catch (Exception e) {
            return new RestfulResult(500, "Edit failed: " + e.getMessage());
        }
    }
    
    @DeleteMapping("/contest/{cid:[0-9]+}")
    @Transactional
    public RestfulResult deleteContest(@PathVariable Long cid, @SessionAttribute User currentUser) {
        if (userService.getUserPermission(currentUser) != Teacher.ADMIN) {
            throw new ForbiddenException("Only Administrator can delete contests");
        }
        try {
            Contest contest = contestRepository.findById(cid).orElse(null);
            if (contest == null) {
                return new RestfulResult(404, "Contest not found");
            }
            // Delete contest problems
            contestProblemRepository.deleteAllByContest(contest);
            // Delete contest itself
            contestRepository.delete(contest);
            return RestfulResult.successResult();
        } catch (Exception e) {
            return new RestfulResult(500, "Delete failed: " + e.getMessage());
        }
    }

    @Data
    static class UpdateConfig {
        private Integer leastScoreToPostBlog = 750;
        private Integer leastScoreToSeeOthersCode = 1000;
        private ArrayList<String> judgerhost;
        private Config.LanguageConfig c;
        private Config.LanguageConfig cpp;
        private Config.LanguageConfig java;
        private Config.LanguageConfig python2;
        private Config.LanguageConfig python3;
        private Config.LanguageConfig go;
        private String notice;

        public UpdateConfig() {
        }

        public UpdateConfig(Config config) {
            setLeastScoreToSeeOthersCode(config.getLeastScoreToSeeOthersCode());
            setLeastScoreToPostBlog(config.getLeastScoreToPostBlog());
            setJudgerhost(config.getJudgerhost());
            setC(config.getC());
            setCpp(config.getCpp());
            setJava(config.getJava());
            setPython2(config.getPython2());
            setPython3(config.getPython3());
            setGo(config.getGo());
            setNotice(config.getNotice());
        }
    }

    @Data
    private static class JsonProblem {
        private String title;
        private String description;
        private String input;
        private String output;
        private String sampleInput;
        private String sampleOutput;
        private String hint;
        private String source;
        private Integer time;
        private Integer memory;
        private Boolean active;
        private Integer score;
        private String tags;

        public JsonProblem() {
        }
    }
    
    // 导出请求
    @Data
    static class ExportRequest {
        private String type; // problems, users, solutions, contests
        private String format; // csv, json, xlsx
        private Map<String, Object> parameters;
    }
    
    // 备份请求
    @Data
    static class BackupRequest {
        private String type; // full, database, config, media
        private Map<String, Object> parameters;
    }
    
    // 恢复请求
    @Data
    static class RestoreRequest {
        private String filePath;
        private String type; // full, database, config, media
    }

}


@Controller
@RequestMapping("/admin")
class AdminViewController {
    @GetMapping("")
    public String adminHome() {
        return "admin/index";
    }

    @GetMapping("/problem")
    public String getAllProblem() {
        return "admin/problems";
    }

    @GetMapping("/problem/edit/{pid:[0-9]+}")
    public String editProblem() {
        return "admin/edit";
    }

    @GetMapping("/problem/add")
    public String addProblem() {
        return "admin/insert";
    }

    @GetMapping("/settings")
    public String setting() {
        return "admin/setting";
    }

    @GetMapping("/contest")
    public String getAllContest() {
        return "admin/contests";
    }

    @GetMapping("/user")
    public String getAllUsers() {
        return "admin/users";
    }

    @GetMapping("/user/teacher")
    public String getTeachers() {
        return "admin/teachers";
    }

    @GetMapping("/tag")
    public String getTags() {
        return "admin/tagManage";
    }
}
