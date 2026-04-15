package cn.edu.zjnu.acm.controller;

import cn.edu.zjnu.acm.entity.User;
import cn.edu.zjnu.acm.entity.oj.ErrorCategory;
import cn.edu.zjnu.acm.entity.oj.ErrorRecord;
import cn.edu.zjnu.acm.entity.oj.Problem;
import cn.edu.zjnu.acm.exception.ForbiddenException;
import cn.edu.zjnu.acm.exception.NeedLoginException;
import cn.edu.zjnu.acm.exception.NotFoundException;
import cn.edu.zjnu.acm.service.ErrorService;
import cn.edu.zjnu.acm.service.ProblemService;
import cn.edu.zjnu.acm.util.RestfulResult;
import lombok.Data;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.List;

@RestController
@RequestMapping("/api/error")
public class ErrorController {
    private final ErrorService errorService;
    private final ProblemService problemService;
    private final HttpSession session;
    
    public ErrorController(ErrorService errorService, ProblemService problemService, HttpSession session) {
        this.errorService = errorService;
        this.problemService = problemService;
        this.session = session;
    }
    
    // 错题记录管理
    @GetMapping("/records")
    public RestfulResult getErrorRecords(@RequestParam(value = "page", defaultValue = "0") int page,
                                         @RequestParam(value = "size", defaultValue = "20") int size) {
        User user = getUserFromSession();
        Page<ErrorRecord> records = errorService.getErrorRecords(user, page, size);
        return new RestfulResult(200, "success", records);
    }
    
    @GetMapping("/records/category/{cid}")
    public RestfulResult getErrorRecordsByCategory(@PathVariable("cid") Long categoryId,
                                                  @RequestParam(value = "page", defaultValue = "0") int page,
                                                  @RequestParam(value = "size", defaultValue = "20") int size) {
        User user = getUserFromSession();
        ErrorCategory category = errorService.getErrorCategoryById(categoryId).orElse(null);
        if (category == null || !category.getUser().getId().equals(user.getId())) {
            throw new NotFoundException("Category not found");
        }
        Page<ErrorRecord> records = errorService.getErrorRecordsByCategory(user, category, page, size);
        return new RestfulResult(200, "success", records);
    }
    
    @GetMapping("/records/marked")
    public RestfulResult getMarkedErrorRecords(@RequestParam(value = "page", defaultValue = "0") int page,
                                              @RequestParam(value = "size", defaultValue = "20") int size) {
        User user = getUserFromSession();
        Page<ErrorRecord> records = errorService.getMarkedErrorRecords(user, page, size);
        return new RestfulResult(200, "success", records);
    }
    
    @GetMapping("/records/unresolved")
    public RestfulResult getUnresolvedErrorRecords(@RequestParam(value = "page", defaultValue = "0") int page,
                                                 @RequestParam(value = "size", defaultValue = "20") int size) {
        User user = getUserFromSession();
        Page<ErrorRecord> records = errorService.getUnresolvedErrorRecords(user, page, size);
        return new RestfulResult(200, "success", records);
    }
    
    @GetMapping("/records/review")
    public RestfulResult getErrorRecordsByReviewPriority(@RequestParam(value = "page", defaultValue = "0") int page,
                                                       @RequestParam(value = "size", defaultValue = "20") int size) {
        User user = getUserFromSession();
        Page<ErrorRecord> records = errorService.getErrorRecordsByReviewPriority(user, page, size);
        return new RestfulResult(200, "success", records);
    }
    
    @PostMapping("/records/mark/{id}")
    public RestfulResult markErrorRecord(@PathVariable("id") Long id,
                                        @RequestBody MarkRequest request) {
        User user = getUserFromSession();
        ErrorRecord record = errorService.getErrorRecordById(id).orElse(null);
        if (record == null || !record.getUser().getId().equals(user.getId())) {
            throw new NotFoundException("Error record not found");
        }
        errorService.markErrorRecord(id, request.isMarked());
        return RestfulResult.successResult();
    }
    
    @PostMapping("/records/resolve/{id}")
    public RestfulResult resolveErrorRecord(@PathVariable("id") Long id,
                                           @RequestBody ResolveRequest request) {
        User user = getUserFromSession();
        ErrorRecord record = errorService.getErrorRecordById(id).orElse(null);
        if (record == null || !record.getUser().getId().equals(user.getId())) {
            throw new NotFoundException("Error record not found");
        }
        errorService.resolveErrorRecord(id, request.isResolved());
        return RestfulResult.successResult();
    }
    
    @DeleteMapping("/records/{id}")
    public RestfulResult deleteErrorRecord(@PathVariable("id") Long id) {
        User user = getUserFromSession();
        ErrorRecord record = errorService.getErrorRecordById(id).orElse(null);
        if (record == null || !record.getUser().getId().equals(user.getId())) {
            throw new NotFoundException("Error record not found");
        }
        errorService.deleteErrorRecord(id);
        return RestfulResult.successResult();
    }
    
    @PostMapping("/records/category/{id}")
    public RestfulResult updateErrorRecordCategory(@PathVariable("id") Long id,
                                                 @RequestBody CategoryUpdateRequest request) {
        User user = getUserFromSession();
        ErrorRecord record = errorService.getErrorRecordById(id).orElse(null);
        if (record == null || !record.getUser().getId().equals(user.getId())) {
            throw new NotFoundException("Error record not found");
        }
        ErrorCategory category = errorService.getErrorCategoryById(request.getCategoryId()).orElse(null);
        if (category == null || !category.getUser().getId().equals(user.getId())) {
            throw new NotFoundException("Category not found");
        }
        errorService.updateErrorRecordCategory(id, category);
        return RestfulResult.successResult();
    }
    
    @PostMapping("/records/review/{id}")
    public RestfulResult updateErrorRecordReview(@PathVariable("id") Long id) {
        User user = getUserFromSession();
        ErrorRecord record = errorService.getErrorRecordById(id).orElse(null);
        if (record == null || !record.getUser().getId().equals(user.getId())) {
            throw new NotFoundException("Error record not found");
        }
        errorService.updateErrorRecordReview(id);
        return RestfulResult.successResult();
    }
    
    @PostMapping("/records/add")
    public RestfulResult addErrorRecord(@RequestBody AddErrorRequest request) {
        User user = getUserFromSession();
        Problem problem = problemService.getProblemById(request.getProblemId());
        if (problem == null) {
            throw new NotFoundException("Problem not found");
        }
        ErrorRecord record = errorService.addErrorRecord(user, problem, request.getErrorType(), request.getErrorMessage());
        return new RestfulResult(200, "success", record);
    }
    
    // 错题分类管理
    @GetMapping("/categories")
    public RestfulResult getErrorCategories() {
        User user = getUserFromSession();
        List<ErrorCategory> categories = errorService.getErrorCategories(user);
        return new RestfulResult(200, "success", categories);
    }
    
    @PostMapping("/categories")
    public RestfulResult createErrorCategory(@RequestBody CreateCategoryRequest request) {
        User user = getUserFromSession();
        ErrorCategory category = errorService.createErrorCategory(request.getName(), request.getDescription(), user);
        return new RestfulResult(200, "success", category);
    }
    
    @PutMapping("/categories/{id}")
    public RestfulResult updateErrorCategory(@PathVariable("id") Long id,
                                           @RequestBody UpdateCategoryRequest request) {
        User user = getUserFromSession();
        ErrorCategory category = errorService.getErrorCategoryById(id).orElse(null);
        if (category == null || !category.getUser().getId().equals(user.getId())) {
            throw new NotFoundException("Category not found");
        }
        errorService.updateErrorCategory(id, request.getName(), request.getDescription());
        return RestfulResult.successResult();
    }
    
    @DeleteMapping("/categories/{id}")
    public RestfulResult deleteErrorCategory(@PathVariable("id") Long id) {
        User user = getUserFromSession();
        ErrorCategory category = errorService.getErrorCategoryById(id).orElse(null);
        if (category == null || !category.getUser().getId().equals(user.getId())) {
            throw new NotFoundException("Category not found");
        }
        errorService.deleteErrorCategory(id);
        return RestfulResult.successResult();
    }
    
    @PostMapping("/categories/order")
    public RestfulResult updateCategoryOrder(@RequestBody List<ErrorCategory> categories) {
        User user = getUserFromSession();
        for (ErrorCategory category : categories) {
            if (!category.getUser().getId().equals(user.getId())) {
                throw new ForbiddenException("No permission");
            }
        }
        errorService.updateCategoryOrder(categories);
        return RestfulResult.successResult();
    }
    
    // 统计信息
    @GetMapping("/stats")
    public RestfulResult getErrorStats() {
        User user = getUserFromSession();
        long total = errorService.getErrorRecordCount(user);
        long unresolved = errorService.getUnresolvedErrorRecordCount(user);
        long marked = errorService.getMarkedErrorRecordCount(user);
        ErrorStats stats = new ErrorStats(total, unresolved, marked);
        return new RestfulResult(200, "success", stats);
    }
    
    // 辅助方法
    private User getUserFromSession() {
        User user = (User) session.getAttribute("currentUser");
        if (user == null) {
            throw new NeedLoginException();
        }
        return user;
    }
    
    // 请求/响应数据类
    @Data
    static class MarkRequest {
        private boolean marked;
    }
    
    @Data
    static class ResolveRequest {
        private boolean resolved;
    }
    
    @Data
    static class CategoryUpdateRequest {
        private Long categoryId;
    }
    
    @Data
    static class AddErrorRequest {
        private Long problemId;
        private String errorType;
        private String errorMessage;
    }
    
    @Data
    static class CreateCategoryRequest {
        private String name;
        private String description;
    }
    
    @Data
    static class UpdateCategoryRequest {
        private String name;
        private String description;
    }
    
    @Data
    static class ErrorStats {
        private long total;
        private long unresolved;
        private long marked;
        
        public ErrorStats(long total, long unresolved, long marked) {
            this.total = total;
            this.unresolved = unresolved;
            this.marked = marked;
        }
    }
}