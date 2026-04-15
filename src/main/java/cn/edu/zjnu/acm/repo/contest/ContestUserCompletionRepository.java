package cn.edu.zjnu.acm.repo.contest;

import cn.edu.zjnu.acm.entity.User;
import cn.edu.zjnu.acm.entity.oj.Contest;
import cn.edu.zjnu.acm.entity.oj.ContestUserCompletion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ContestUserCompletionRepository extends JpaRepository<ContestUserCompletion, Long> {
    Optional<ContestUserCompletion> findByContestAndUser(Contest contest, User user);
    boolean existsByContestAndUser(Contest contest, User user);
}
