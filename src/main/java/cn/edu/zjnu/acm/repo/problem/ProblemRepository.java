package cn.edu.zjnu.acm.repo.problem;

import cn.edu.zjnu.acm.entity.oj.Problem;
import cn.edu.zjnu.acm.entity.oj.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface ProblemRepository extends JpaRepository<Problem, Long> {
    Optional<Problem> findProblemByIdAndStatus(Long id, Problem.Status status);

    Page<Problem> findAll(Pageable pageable);

    Page<Problem> findProblemsByStatus(Pageable pageable, Problem.Status status);

    List<Problem> findProblemsByStatusAndId(Problem.Status status, Long id);

    Page<Problem> findAllByTitleContaining(Pageable pageable, String title);

    List<Problem> findAllByStatusAndTitleContaining(Problem.Status status, String title);

    Page<Problem> findProblemsByStatusAndTagsInAndIdIn(Pageable pageable, Problem.Status status, List<Tag> tags, List<Long> ids);

    List<Problem> findAllByTags(Tag tag);

    Optional<Problem> findByTitle(String title);

    Optional<Problem> findById(Long id);

    Page<Problem> findByStatus(Pageable pageable, Problem.Status status);

    @Transactional
    @Modifying
    @Query(value = "UPDATE problem set accepted=accepted+:ac where id = :pid", nativeQuery = true)
    void updateAcceptedNumber(@Param(value = "pid") Long pid, @Param(value = "ac") int ac);

    @Transactional
    @Modifying
    @Query(value = "UPDATE problem set accepted=:ac where id = :pid", nativeQuery = true)
    void setAcceptedNumber(@Param(value = "pid") Long pid, @Param(value = "ac") int ac);

    @Transactional
    @Modifying
    @Query(value = "UPDATE problem set submitted=submitted+:sub where id = :pid", nativeQuery = true)
    void updateSubmittedNumber(@Param(value = "pid") Long pid, @Param(value = "sub") int sub);

    @Transactional
    @Modifying
    @Query(value = "UPDATE problem set submitted=:sub where id = :pid", nativeQuery = true)
    void setSubmittedNumber(@Param(value = "pid") Long pid, @Param(value = "sub") int sub);
}
