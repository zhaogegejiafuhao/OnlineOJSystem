package cn.edu.zjnu.acm.service;

import cn.edu.zjnu.acm.entity.User;
import cn.edu.zjnu.acm.entity.oj.ErrorCategory;
import cn.edu.zjnu.acm.entity.oj.ErrorRecord;
import cn.edu.zjnu.acm.entity.oj.Problem;
import cn.edu.zjnu.acm.repo.oj.ErrorCategoryRepository;
import cn.edu.zjnu.acm.repo.oj.ErrorRecordRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class ErrorService {
    private final ErrorRecordRepository errorRecordRepository;
    private final ErrorCategoryRepository errorCategoryRepository;
    
    public ErrorService(ErrorRecordRepository errorRecordRepository, ErrorCategoryRepository errorCategoryRepository) {
        this.errorRecordRepository = errorRecordRepository;
        this.errorCategoryRepository = errorCategoryRepository;
    }
    
    // 错题记录管理
    public Page<ErrorRecord> getErrorRecords(User user, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createTime"));
        return errorRecordRepository.findByUserOrderByCreateTimeDesc(user, pageable);
    }
    
    public Page<ErrorRecord> getErrorRecordsByCategory(User user, ErrorCategory category, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createTime"));
        return errorRecordRepository.findByUserAndCategoryOrderByCreateTimeDesc(user, category, pageable);
    }
    
    public Page<ErrorRecord> getMarkedErrorRecords(User user, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createTime"));
        return errorRecordRepository.findByUserAndIsMarkedTrueOrderByCreateTimeDesc(user, pageable);
    }
    
    public Page<ErrorRecord> getUnresolvedErrorRecords(User user, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createTime"));
        return errorRecordRepository.findByUserAndIsResolvedFalseOrderByCreateTimeDesc(user, pageable);
    }
    
    public Page<ErrorRecord> getErrorRecordsByReviewPriority(User user, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return errorRecordRepository.findByUserOrderByReviewCountAsc(user, pageable);
    }
    
    public Optional<ErrorRecord> getErrorRecordById(Long id) {
        return errorRecordRepository.findById(id);
    }
    
    @Transactional
    public ErrorRecord markErrorRecord(Long id, boolean isMarked) {
        ErrorRecord record = errorRecordRepository.findById(id).orElse(null);
        if (record != null) {
            record.setIsMarked(isMarked);
            errorRecordRepository.save(record);
        }
        return record;
    }
    
    @Transactional
    public ErrorRecord resolveErrorRecord(Long id, boolean isResolved) {
        ErrorRecord record = errorRecordRepository.findById(id).orElse(null);
        if (record != null) {
            record.setIsResolved(isResolved);
            errorRecordRepository.save(record);
        }
        return record;
    }
    
    @Transactional
    public void deleteErrorRecord(Long id) {
        errorRecordRepository.deleteById(id);
    }
    
    @Transactional
    public ErrorRecord updateErrorRecordCategory(Long id, ErrorCategory category) {
        ErrorRecord record = errorRecordRepository.findById(id).orElse(null);
        if (record != null) {
            record.setCategory(category);
            errorRecordRepository.save(record);
        }
        return record;
    }
    
    @Transactional
    public ErrorRecord updateErrorRecordReview(Long id) {
        ErrorRecord record = errorRecordRepository.findById(id).orElse(null);
        if (record != null) {
            record.setReviewCount(record.getReviewCount() + 1);
            record.setLastReviewTime(Instant.now());
            errorRecordRepository.save(record);
        }
        return record;
    }
    
    // 错题分类管理
    public List<ErrorCategory> getErrorCategories(User user) {
        return errorCategoryRepository.findByUserOrderByOrderIndexAsc(user);
    }
    
    public Optional<ErrorCategory> getErrorCategoryById(Long id) {
        return errorCategoryRepository.findById(id);
    }
    
    @Transactional
    public ErrorCategory createErrorCategory(String name, String description, User user) {
        ErrorCategory category = new ErrorCategory(name, description, user);
        return errorCategoryRepository.save(category);
    }
    
    @Transactional
    public ErrorCategory updateErrorCategory(Long id, String name, String description) {
        ErrorCategory category = errorCategoryRepository.findById(id).orElse(null);
        if (category != null) {
            category.setName(name);
            category.setDescription(description);
            errorCategoryRepository.save(category);
        }
        return category;
    }
    
    @Transactional
    public void deleteErrorCategory(Long id) {
        errorCategoryRepository.deleteById(id);
    }
    
    @Transactional
    public void updateCategoryOrder(List<ErrorCategory> categories) {
        for (int i = 0; i < categories.size(); i++) {
            ErrorCategory category = categories.get(i);
            category.setOrderIndex(i);
            errorCategoryRepository.save(category);
        }
    }
    
    // 统计信息
    public long getErrorRecordCount(User user) {
        return errorRecordRepository.countByUser(user);
    }
    
    public long getUnresolvedErrorRecordCount(User user) {
        return errorRecordRepository.countUnresolvedByUser(user);
    }
    
    public long getMarkedErrorRecordCount(User user) {
        return errorRecordRepository.countMarkedByUser(user);
    }
    
    // 手动添加错题
    @Transactional
    public ErrorRecord addErrorRecord(User user, Problem problem, String errorType, String errorMessage) {
        ErrorRecord record = new ErrorRecord(user, problem, null, errorType, errorMessage);
        return errorRecordRepository.save(record);
    }
}