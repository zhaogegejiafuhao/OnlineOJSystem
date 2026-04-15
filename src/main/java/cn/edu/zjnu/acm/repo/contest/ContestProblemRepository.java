package cn.edu.zjnu.acm.repo.contest;

import cn.edu.zjnu.acm.entity.oj.Contest;
import cn.edu.zjnu.acm.entity.oj.ContestProblem;
import cn.edu.zjnu.acm.entity.oj.Problem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContestProblemRepository extends JpaRepository<ContestProblem, Long> {
    void deleteAllByContest(Contest contest);
    void deleteAllByProblem(Problem problem);
    List<ContestProblem> findAllByContest(Contest contest);
    ContestProblem findByContestAndTempId(Contest contest, Long tempId);
    @Modifying
    @Query("UPDATE ContestProblem cp SET cp.submitted = ?2 WHERE cp.id = ?1")
    void updateSubmittedNumber(Long contestProblemId, int submitted);
    @Modifying
    @Query("UPDATE ContestProblem cp SET cp.accepted = ?2 WHERE cp.id = ?1")
    void updateAcceptedNumber(Long contestProblemId, int accepted);
}