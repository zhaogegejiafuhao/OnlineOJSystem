package cn.edu.zjnu.acm.repo.user;

import cn.edu.zjnu.acm.entity.User;
import cn.edu.zjnu.acm.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
    void deleteAllByUser(User user);
    @Modifying
    @Query("UPDATE UserProfile up SET up.submitted = ?2 WHERE up.id = ?1")
    void setUserSubmitted(Long userProfileId, int submitted);
    @Modifying
    @Query("UPDATE UserProfile up SET up.accepted = ?2 WHERE up.id = ?1")
    void setUserAccepted(Long userProfileId, int accepted);
    @Modifying
    @Query("UPDATE UserProfile up SET up.score = ?2 WHERE up.id = ?1")
    void setUserScore(Long userProfileId, int score);
    @Modifying
    @Query("UPDATE UserProfile up SET up.submitted = ?2 WHERE up.id = ?1")
    void updateUserSubmitted(Long userProfileId, int submitted);
    @Modifying
    @Query("UPDATE UserProfile up SET up.accepted = ?2 WHERE up.id = ?1")
    void updateUserAccepted(Long userProfileId, int accepted);
    @Modifying
    @Query("UPDATE UserProfile up SET up.score = ?2 WHERE up.id = ?1")
    void updateUserScore(Long userProfileId, Integer score);
}