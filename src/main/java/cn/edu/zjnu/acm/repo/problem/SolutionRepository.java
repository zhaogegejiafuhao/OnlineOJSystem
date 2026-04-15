package cn.edu.zjnu.acm.repo.problem;

import cn.edu.zjnu.acm.entity.User;
import cn.edu.zjnu.acm.entity.oj.Contest;
import cn.edu.zjnu.acm.entity.oj.Problem;
import cn.edu.zjnu.acm.entity.oj.Solution;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface SolutionRepository extends JpaRepository<Solution, Long> {
    void deleteAllByUser(User user);
    void deleteAllByProblem(Problem problem);
    List<Solution> findByResultOrderByIdAsc(Integer result);
    Page<Solution> findAllByOrderByIdDesc(PageRequest pageRequest);
    Page<Solution> findAllByUserAndProblemAndResult(Pageable pageable, User user, Problem problem, String result);
    Page<Solution> findAllByUserAndProblem(Pageable pageable, User user, Problem problem);
    Page<Solution> findAllByUserAndResult(Pageable pageable, User user, String result);
    Page<Solution> findAllByUser(Pageable pageable, User user);
    Page<Solution> findAllByProblemAndResult(Pageable pageable, Problem problem, String result);
    Page<Solution> findAllByProblem(Pageable pageable, Problem problem);
    Page<Solution> findAllByResult(Pageable pageable, String result);
    List<Solution> findFirst50ByResultAndProblemOrderByTimeAsc(String result, Problem problem);
    List<Solution> findAllByUserAndProblemOrderByIdDesc(User user, Problem problem);
    
    @Modifying
    @Query("UPDATE Solution s SET s.result = :result, s.time = :time, s.memory = :memory, s.caseNumber = :caseNumber WHERE s.id = :id")
    void updateResultTimeMemoryCase(@Param("id") Long id, @Param("result") String result, @Param("time") Integer time, @Param("memory") Integer memory, @Param("caseNumber") Integer caseNumber);
    
    @Modifying
    @Query("UPDATE Solution s SET s.share = :share WHERE s.id = :id")
    void updateShare(@Param("id") Long id, @Param("share") Boolean share);
    
    @Modifying
    @Query("UPDATE Solution s SET s.result = :result, s.info = :info WHERE s.id = :id")
    void updateResultInfo(@Param("id") Long id, @Param("result") String result, @Param("info") String info);
    
    @Query("SELECT s FROM Solution s WHERE s.contest = :contest AND s.user = :user ORDER BY s.id DESC")
    Page<Solution> findAllByContestAndUser(Pageable pageable, @Param("contest") Contest contest, @Param("user") User user);
    
    List<Solution> findAllByContestOrderByIdDesc(Contest contest);
    
    @Query("SELECT s FROM Solution s WHERE s.contest = :contest ORDER BY s.id DESC")
    Page<Solution> findAllByContestOrderByIdDesc(Pageable pageable, @Param("contest") Contest contest);
    long countAllByProblemAndResult(Problem problem, String result);
    long countAllByProblem(Problem problem);
    long countAllByUser(User user);
    long countAllByContestAndProblemAndResult(Contest contest, Problem problem, String result);
    long countAllByContestAndProblem(Contest contest, Problem problem);
    List<Solution> findByResultAndSubmitTimeBefore(String result, Instant submitTime);
}